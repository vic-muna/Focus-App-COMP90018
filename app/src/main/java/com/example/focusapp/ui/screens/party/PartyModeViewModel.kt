package com.example.focusapp.ui.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.SensorDataSource
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
 *  2. keeps publishing this phone's own status (location, focusing or not).
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

    private var currentPartyId: String? = null
    private var displayName: String = "You"
    private var isFocusing: Boolean = false

    private var observeJob: Job? = null
    private var publishJob: Job? = null

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

        observeJob = viewModelScope.launch {
            repository.observePartyMembers(partyId)
                .catch { e -> _errorMessage.value = friendlyPartyErrorMessage(e) }
                .collect { _members.value = it }
        }

        publishJob = viewModelScope.launch {
            val uid = try {
                repository.getMyUid()
            } catch (e: Exception) {
                _errorMessage.value = friendlyPartyErrorMessage(e)
                return@launch // Without an id we can't publish; watching members still works.
            }
            sensorDataSource.locationFlow.collect { location ->
                try {
                    repository.updateMyPartyStatus(
                        partyId,
                        PartyMemberStatus(
                            uid = uid,
                            displayName = this@PartyModeViewModel.displayName,
                            latitude = location?.first,
                            longitude = location?.second,
                            isFocusing = isFocusing
                        )
                    )
                } catch (e: Exception) {
                    // Keep going; the next GPS update tries again.
                    _errorMessage.value = friendlyPartyErrorMessage(e)
                }
            }
        }
    }

    /** Tells the party whether this phone is focusing, right away. */
    fun setFocusing(focusing: Boolean) {
        isFocusing = focusing
        val partyId = currentPartyId ?: return
        viewModelScope.launch {
            try {
                val uid = repository.getMyUid()
                val location = sensorDataSource.locationFlow.value
                repository.updateMyPartyStatus(
                    partyId,
                    PartyMemberStatus(
                        uid = uid,
                        displayName = displayName,
                        latitude = location?.first,
                        longitude = location?.second,
                        isFocusing = focusing
                    )
                )
            } catch (e: Exception) {
                _errorMessage.value = friendlyPartyErrorMessage(e)
            }
        }
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
        observeJob?.cancel()
        observeJob = null
        publishJob?.cancel()
        publishJob = null
        _members.value = emptyList()
        _errorMessage.value = null
        currentPartyId = null
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
