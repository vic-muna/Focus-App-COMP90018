package com.example.focusapp.ui.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.notification.PartyNotification
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.SensorDataSource
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import com.example.focusapp.data.account.AccountManager

/**
 * Party Mode's cloud logic (Firebase), used by [FriendsScreen].
 *
 * The 6-letter group code is the party's id in Firebase. Both the host and
 * the people joining call [joinParty] with that code, which:
 *  1. watches everyone in the party ([members]), and
 *  2. keeps publishing this phone's own status (location, focusing or not, App Blocking on or off).
 *
 * Starting together: the host's Start ([requestStart]) is held back until every member has App
 * Blocking (Accessibility) on; the members who don't get a notification and the permission
 * dialog. Once the host is focusing, every joined member follows ([partyFocusStarted]).
 *
 * Every Firebase call is wrapped in try/catch: a failure shows up in
 * [errorMessage] instead of crashing the app.
 *
 * [Claude, 2026-10-03] Friends ([friends], [myFriendCode], [addFriend], [deleteFriend]) are the
 * missing first link [inviteFriend]/[incomingInvites] were "ready and waiting" for - see
 * [FriendsScreen] for where these actually show up (a copyable "My Code" row, an Add Friend
 * card, a delete icon per friend, and an Invite button per friend while hosting a group). Still
 * no server-side friend directory/search - adding someone means they tell you their
 * [myFriendCode] (e.g. out loud, or copy-paste over text) and you type it into [addFriend]
 * yourself; there's no way to look a stranger up by name. [myFriendCode] is a short (6-char)
 * stand-in for this phone's real, much longer Firebase uid - see
 * RemoteDataSource.getOrCreateMyFriendCode()'s doc comment for why.
 *
 * [Claude, 2026-10-03] [inviteFriend] used to call a fire-and-forget write (no .await(), no
 * error ever surfaced) - a rejected write (e.g. Realtime Database Rules not allowing one user
 * to write into another user's users/$toUid subtree, which sendPartyInvite needs to do to
 * deliver the invite) failed completely silently: the inviter saw no error, and the invitee's
 * [incomingInvites] just never got anything. sendPartyInvite is now suspend and awaited, so a
 * rejected write throws and shows up in [errorMessage] instead.
 */
class PartyModeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusRepositoryProvider.get(application)
    private val sensorDataSource = SensorDataSource()


    /** A guest has no friend code, friend list or invites - only group codes ([joinParty]).
     *  The Database Rules refuse a guest's friend code too, so this isn't just hidden UI.
     *  Read once: creating an account happens on another screen, which makes a new ViewModel. */
    val isGuest: Boolean = AccountManager.account.value?.isGuest != false

    private val _members = MutableStateFlow<List<PartyMemberStatus>>(emptyList())
    /** Live member list for whichever party [joinParty] was last called with. */
    val members: StateFlow<List<PartyMemberStatus>> = _members.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** A message for the user when the last Firebase call failed, else null. */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    private val _incomingInvites = MutableStateFlow<List<PartyInvite>>(emptyList())
    /** Invites sent to this phone. Errors here are ignored, so nobody sees an error before tapping anything. */
    val incomingInvites: StateFlow<List<PartyInvite>> = _incomingInvites.asStateFlow()

    private val _friends = MutableStateFlow<List<Friend>>(emptyList())
    /** This phone's saved friends (local only - see class doc comment). */
    val friends: StateFlow<List<Friend>> = _friends.asStateFlow()

    private val _myFriendCode = MutableStateFlow<String?>(null)
    /** This phone's own short friend code, to share with a friend so they can [addFriend] you
     *  back - null until it's loaded (first launch generates and claims one). */
    val myFriendCode: StateFlow<String?> = _myFriendCode.asStateFlow()

    init {
        if (!isGuest) {
            viewModelScope.launch {
                repository.observeMyIncomingInvites()
                    .catch { /* stay empty */ }
                    .collect { _incomingInvites.value = it }
            }
            viewModelScope.launch { reloadFriends() }
            viewModelScope.launch {
                try {
                    _myFriendCode.value = repository.getOrCreateMyFriendCode()
                } catch (e: Exception) {
                    // Left null - FriendsScreen just doesn't show the "My Code" row until a retry
                    // succeeds (e.g. the next time this screen is opened); not worth an error
                    // banner for something the user hasn't asked to do yet.
                }
            }
        }
    }

    private suspend fun reloadFriends() {
        try {
            _friends.value = repository.getFriends().sortedBy { it.nickname.lowercase() }
        } catch (e: Exception) {
            // Local Room read - failing here would mean something is wrong with the database
            // itself, not just "offline". Leaving the list as whatever it last was (likely
            // empty, on first call) is safer than surfacing a scary error for a local read.
        }
    }

    /** Resolves [code] (one of someone's [myFriendCode] values) to their real uid, then saves
     *  them as a friend called [nickname] - both trimmed; does nothing if either is blank after
     *  trimming. The resolve step is the one part of this that needs Firebase (friend codes
     *  only exist there); the save itself is local only (see class doc comment).
     *  [onSaved] runs only once the friend is saved; a wrong code or a failure sets
     *  [errorMessage] instead, so the Add Friend card can stay open and show it. */
    fun addFriend(code: String, nickname: String, onSaved: () -> Unit = {}) {
        val trimmedCode = code.trim()
        val trimmedNickname = nickname.trim()
        if (isGuest || trimmedCode.isBlank() || trimmedNickname.isBlank()) return
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val uid = repository.resolveFriendCode(trimmedCode)
                    ?: run { _errorMessage.value = "No one has that code - check it and try again."; return@launch }
                repository.saveFriend(Friend(uid = uid, nickname = trimmedNickname))
                reloadFriends()
                onSaved()
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't save that friend - try again."
            }
        }
    }

    /** Removes a saved friend - local only, so this always succeeds unless Room itself is
     *  broken. Does not affect anything already shared with them (party invites already sent,
     *  memberships already joined). */
    fun deleteFriend(uid: String) {
        viewModelScope.launch {
            try {
                repository.deleteFriend(uid)
                reloadFriends()
            } catch (e: Exception) {
                _errorMessage.value = "Couldn't remove that friend - try again."
            }
        }
    }

    /** Accepts or declines [invite]. Accepting also joins that party. */
    fun respondToInvite(invite: PartyInvite, accept: Boolean) {
        viewModelScope.launch {
            try {
                repository.respondToPartyInvite(invite.partyId, accept)
                if (accept) joinParty(invite.partyId, displayName)
            } catch (e: Exception) {
                _errorMessage.value = friendlyPartyErrorMessage(e)
            }
        }
    }

    // --- The party this phone is in right now ---

    private var currentPartyId: String? = null
    private var displayName: String = "You"
    private var myUid: String? = null

    // --- What this phone says about itself to the party (see PartyMemberStatus) ---

    private var isFocusing: Boolean = false

    /** Host only: Start was tried while some member had App Blocking off. Published, so
     *  those members get told. Cleared when they are all ready, or when the host leaves. */
    private var waitingOnMembers: Boolean = false

    // --- What FriendsScreen reacts to ---

    private val _partyFocusStarted = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Fires once each time someone else in the party goes from not focusing to focusing - the
     *  cue for a joined participant to start focusing too. An event, not a state, so coming
     *  back to the screen doesn't replay it. */
    val partyFocusStarted: SharedFlow<Unit> = _partyFocusStarted.asSharedFlow()

    private val _waitingForMembers = MutableStateFlow(0)
    /** How many other members still need to turn on App Blocking before the host can start;
     *  0 = nothing is holding the host back (or Start hasn't been tried yet). */
    val waitingForMembers: StateFlow<Int> = _waitingForMembers.asStateFlow()

    private val _permissionNeeded = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    /** Fires when the host tries to start while this phone has App Blocking off - the cue to
     *  show the permission dialog (a notification goes out as well). */
    val permissionNeeded: SharedFlow<Unit> = _permissionNeeded.asSharedFlow()

    private var observeJob: Job? = null
    private var publishJob: Job? = null
    private var accessibilityJob: Job? = null

    /**
     * Starts watching [partyId]'s members and publishing this phone's status.
     * Calling it again with another code leaves the old party first.
     * GPS starts too; without location permission the status just has no location.
     */
    fun joinParty(partyId: String, displayName: String) {
        if (currentPartyId == partyId && observeJob?.isActive == true) return
        leaveParty()
        currentPartyId = partyId
        this.displayName = displayName

        sensorDataSource.startTracking(getApplication())

        observeJob = viewModelScope.launch { watchMembers(partyId) }

        // Publishes right away and again whenever this phone turns App Blocking on or off, so
        // the host's Start sees the change without waiting for a GPS fix.
        accessibilityJob = viewModelScope.launch {
            AccessibilityBridge.isServiceConnected.collect { publishNow() }
        }

        publishJob = viewModelScope.launch { publishOnEveryLocation(partyId) }
    }

    /** Keeps [members] current and reacts to what the other members' statuses say. */
    private suspend fun watchMembers(partyId: String) {
        val me = try { repository.getMyUid() } catch (e: Exception) { null }
        myUid = me
        var othersWereFocusing = false
        var someoneWasWaiting = false
        repository.observePartyMembers(partyId)
            .catch { e -> _errorMessage.value = friendlyPartyErrorMessage(e) }
            .collect { members ->
                _members.value = members

                val othersFocusing = members.any { it.isFocusing && it.uid != me }
                if (othersFocusing && !othersWereFocusing) _partyFocusStarted.tryEmit(Unit)
                othersWereFocusing = othersFocusing

                refreshWaitingOnMembers(members, me)

                val someoneWaiting = members.any { it.waitingForPermission && it.uid != me }
                refreshPermissionNotice(someoneWaiting, justStartedWaiting = !someoneWasWaiting)
                someoneWasWaiting = someoneWaiting
            }
    }

    /** Host: keeps the "wait for N members" count current, and stops waiting once all are ready. */
    private fun refreshWaitingOnMembers(members: List<PartyMemberStatus>, me: String?) {
        if (!waitingOnMembers) return
        val notReady = members.count { it.uid != me && !it.accessibilityReady }
        _waitingForMembers.value = notReady
        if (notReady == 0) setWaitingOnMembers(false)
    }

    /**
     * Member: when the host starts waiting and this phone is the one holding things up, shows
     * the notification and asks the screen for the permission dialog. Takes the notification
     * down again once the permission is on or the host stops waiting.
     */
    private fun refreshPermissionNotice(someoneWaiting: Boolean, justStartedWaiting: Boolean) {
        val appBlockingOff = !AccessibilityBridge.isServiceConnected.value
        if (someoneWaiting && appBlockingOff) {
            if (justStartedWaiting) {
                PartyNotification.showPermissionNeeded(getApplication())
                _permissionNeeded.tryEmit(Unit)
            }
        } else {
            PartyNotification.cancelPermissionNeeded(getApplication())
        }
    }

    /** Publishes this phone's status on every new GPS fix (the repository throttles the writes). */
    private suspend fun publishOnEveryLocation(partyId: String) {
        val uid = try {
            repository.getMyUid()
        } catch (e: Exception) {
            _errorMessage.value = friendlyPartyErrorMessage(e)
            return // Without an id we can't publish; watching members still works.
        }
        sensorDataSource.locationFlow.collect { location ->
            try {
                repository.updateMyPartyStatus(partyId, currentStatus(uid, location))
            } catch (e: Exception) {
                // Keep going; the next GPS update tries again.
                _errorMessage.value = friendlyPartyErrorMessage(e)
            }
        }
    }

    private fun currentStatus(uid: String, location: Pair<Double, Double>?) = PartyMemberStatus(
        uid = uid,
        displayName = displayName,
        latitude = location?.first,
        longitude = location?.second,
        isFocusing = isFocusing,
        accessibilityReady = AccessibilityBridge.isServiceConnected.value,
        waitingForPermission = waitingOnMembers
    )

    /** Pushes this phone's status now (not waiting for the next GPS fix). */
    private fun publishNow() {
        val partyId = currentPartyId ?: return
        viewModelScope.launch {
            try {
                val uid = repository.getMyUid()
                repository.updateMyPartyStatus(partyId, currentStatus(uid, sensorDataSource.locationFlow.value))
            } catch (e: Exception) {
                _errorMessage.value = friendlyPartyErrorMessage(e)
            }
        }
    }

    /** Tells the party whether this phone is focusing, right away. */
    fun setFocusing(focusing: Boolean) {
        isFocusing = focusing
        if (focusing) waitingOnMembers = false // Starting means nobody is being waited for any more.
        publishNow()
    }

    private fun setWaitingOnMembers(waiting: Boolean) {
        if (waitingOnMembers == waiting) return
        waitingOnMembers = waiting
        publishNow()
    }

    /**
     * The host's Start: true if every other member has App Blocking on, so the session can begin.
     * If not, returns false, tells the others the host is waiting (they get a notification) and
     * counts them in [waitingForMembers]; that count drops by itself as they turn it on.
     */
    fun requestStart(): Boolean {
        val notReady = _members.value.count { it.uid != myUid && !it.accessibilityReady }
        _waitingForMembers.value = notReady
        setWaitingOnMembers(notReady > 0)
        return notReady == 0
    }

    /** Invites the friend with id [toUid] to the current party (does nothing if not in one). */
    fun inviteFriend(toUid: String) {
        if (isGuest) return
        val partyId = currentPartyId ?: return
        viewModelScope.launch {
            try {
                repository.sendPartyInvite(partyId, toUid)
            } catch (e: Exception) {
                _errorMessage.value = friendlyPartyErrorMessage(e)
            }
        }
    }

    /** Stops watching and publishing, e.g. when a group card is closed. */
    fun leaveParty() {
        announceLeaving()
        isFocusing = false
        waitingOnMembers = false
        _waitingForMembers.value = 0
        PartyNotification.cancelPermissionNeeded(getApplication())

        observeJob?.cancel()
        observeJob = null
        publishJob?.cancel()
        publishJob = null
        accessibilityJob?.cancel()
        accessibilityJob = null

        _members.value = emptyList()
        _errorMessage.value = null
        currentPartyId = null
    }

    /**
     * The last status this phone leaves in the party: not focusing and not waiting, or the next
     * person to join would be pulled into a session that ended long ago. Marked ready too - a
     * member who has left stays in the party's list, and must not keep blocking the host's Start.
     * A plain write, so it still goes out as the ViewModel is cleared.
     */
    private fun announceLeaving() {
        val partyId = currentPartyId ?: return
        val uid = myUid ?: return
        repository.updateMyPartyStatus(
            partyId,
            PartyMemberStatus(uid = uid, displayName = displayName, accessibilityReady = true)
        )
    }

    override fun onCleared() {
        super.onCleared()
        leaveParty()
    }
}

/** Turns a Firebase error into a message that says what to check. */
private fun friendlyPartyErrorMessage(e: Throwable): String {
    val raw = e.message.orEmpty()
    return when {
        raw.contains("permission", ignoreCase = true) ->
            "Can't reach the party right now: Firebase says permission denied. Check the Realtime Database Rules in the Firebase Console."
        e::class.java.simpleName.contains("FirebaseAuth", ignoreCase = true) ->
            "Can't sign in to Firebase. Check Authentication -> Sign-in method -> Anonymous is enabled in the Firebase Console."
        else ->
            "Party Mode couldn't reach Firebase right now (${e::class.java.simpleName}${if (raw.isNotBlank()) ": $raw" else ""}). Check your connection and the Firebase project setup."
    }
}
