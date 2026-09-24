package com.example.focusapp.ui.screens.party

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.SensorDataSource
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * PartyModeViewModel
 * ---------------------
 * Wires [PartyModeScreen] up to the real Study Party data layer
 * (FocusRepository.observePartyMembers/updateMyPartyStatus - see
 * domain/repository/FocusRepository.kt and data/remote/FirebaseRemoteDataSource.kt)
 * instead of the hardcoded participant lists the screen used to show.
 * Note: FocusRepositoryImpl.updateMyPartyStatus() already throttles by time/distance,
 * so this class (and the location flow it collects) can call it on every GPS tick
 * without worrying about battery/Firebase-quota - that's the data layer's job.
 *
 * Party-id contract used here: the 6-character code shown/entered on this screen
 * (see generatePartyCode() in PartyModeScreen.kt) IS the Firebase partyId - both the
 * host's "Invite" flow and a joiner's "Join" flow just call [joinParty] with that
 * same code, which starts (a) observing every member currently under
 * parties/{code}/members, and (b) publishing this device's own PartyMemberStatus
 * into that same path so everyone else with the code sees it too.
 *
 * Friend-targeted invites: [inviteFriend] wraps FocusRepository.sendPartyInvite()
 * so a caller just needs a uid (see ui/screens/party/FriendListScreen.kt for where
 * that uid comes from) - it's a no-op if called before [joinParty] has set up a
 * current party. Incoming invites: [incomingInvites] observes every invite
 * currently addressed to this device (across all parties, not just the one
 * [joinParty] was last called with - see FocusRepository.observeMyIncomingInvites's
 * doc comment for the Firebase layout behind this), started as soon as this
 * ViewModel is created rather than waiting for [joinParty], since a person should
 * be able to see/accept an invite before creating or joining a party themselves.
 * [respondToInvite] wraps FocusRepository.respondToPartyInvite() and, on accept,
 * also calls [joinParty] so accepting actually puts the device in that party -
 * before this, respondToPartyInvite() existed on the repository but nothing in
 * the UI ever called it.
 *
 * Error handling: every call into FocusRepository below that ends up hitting Firebase
 * (getMyUid/observePartyMembers/updateMyPartyStatus/sendPartyInvite/respondToInvite) is
 * wrapped, and failures go into [errorMessage] instead of propagating - previously NONE
 * of these were protected, so tapping Invite while e.g. Anonymous Auth wasn't enabled in
 * the Firebase Console (or Realtime Database Rules rejected the read/write) threw straight
 * out of a coroutine launched on viewModelScope and crashed the whole app. Party Mode
 * is inherently online-only (see FocusRepositoryImpl's doc comment), so a Firebase
 * failure here genuinely means the feature can't work right now - [errorMessage] is how
 * PartyModeScreen tells the user that, rather than the screen just sitting there with
 * an empty participant list and no explanation. [incomingInvites] itself is the one
 * exception - see its own doc comment for why failures there are swallowed instead.
 */
class PartyModeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusRepositoryProvider.get(application)
    private val sensorDataSource = SensorDataSource()

    private val _members = MutableStateFlow<List<PartyMemberStatus>>(emptyList())
    /** Live member list for whichever party [joinParty] was last called with. */
    val members: StateFlow<List<PartyMemberStatus>> = _members.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    /** Non-null when the last Firebase call failed - see this class's doc comment.
     *  PartyModeScreen shows this inside the Invite/Join dialog; call [clearError] once
     *  it's been shown (or just call [leaveParty], which already clears it). */
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun clearError() {
        _errorMessage.value = null
    }

    private val _incomingInvites = MutableStateFlow<List<PartyInvite>>(emptyList())
    /** Every invite currently addressed to this device, across all parties - see this class's
     *  doc comment. Started in [init] below (not [joinParty]) so it's already populated by the
     *  time PartyModeScreen opens. Failures here are swallowed (left as an empty list) rather
     *  than surfaced via [errorMessage] - this listener starts before the user has done
     *  anything, so a misconfigured Firebase project shouldn't greet them with an error banner
     *  before they've tapped Invite/Join/Invites; those explicit actions still report failures
     *  normally. */
    val incomingInvites: StateFlow<List<PartyInvite>> = _incomingInvites.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMyIncomingInvites()
                .catch { /* see incomingInvites' doc comment - stay empty rather than surface this */ }
                .collect { _incomingInvites.value = it }
        }
    }

    /**
     * Accepts or declines [invite]. On accept, also calls [joinParty] with the invite's
     * partyId (using this device's own uid as a fallback display name) so accepting an
     * invite actually puts the device in that party, not just marks it "accepted" in
     * Firebase with nothing else happening locally.
     */
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
     * Starts observing + publishing this device's status for [partyId]. Safe to call again with
     * a different code (e.g. the user backs out of one dialog and opens the other) - the
     * previous jobs are cancelled first so we don't keep writing into an old party.
     *
     * Starts GPS tracking via [SensorDataSource] as a side effect. If location permission
     * hasn't been granted, tracking silently does nothing (see LocationDataSource.startGPSUpdates)
     * and every published status just carries null lat/lng - Party Mode itself doesn't strictly
     * need location to show who's in the party, so this still works without it.
     *
     * Every Firebase call this kicks off is guarded - see this class's doc comment - so a
     * misconfigured Firebase project surfaces as [errorMessage], never a crash.
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
                return@launch // no uid, nothing safe to publish - observeJob above still runs
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
                    // Don't cancel the collect just because one push failed - the next GPS
                    // tick (or the next setFocusing() call) gets another chance.
                    _errorMessage.value = friendlyPartyErrorMessage(e)
                }
            }
        }
    }

    /**
     * Flips isFocusing and republishes right away - a focus-state change is a real event, not
     * GPS jitter, so FocusRepositoryImpl's throttle always lets it straight through (see its
     * updateMyPartyStatus() doc comment). Call this from PartyModeScreen's "Go Focus Mode"
     * button, before navigating away, so friends see the change immediately.
     */
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

    /**
     * Sends a targeted invite to [toUid] for the party this device is currently in - see
     * FirebaseRemoteDataSource.sendPartyInvite() for exactly what that writes. No-op if there's
     * no current party (shouldn't normally happen - the friend picker is only reachable from
     * inside the Invite dialog, which already called [joinParty]).
     */
    fun inviteFriend(toUid: String) {
        val partyId = currentPartyId ?: return
        try {
            repository.sendPartyInvite(partyId, toUid)
        } catch (e: Exception) {
            _errorMessage.value = friendlyPartyErrorMessage(e)
        }
    }

    /** Stops observing/publishing - call when leaving Party Mode (dialog dismissed, back
     *  pressed) so we don't keep writing this device's location to a party the screen isn't
     *  showing anymore. Also clears any [errorMessage] from the last attempt, so reopening
     *  Invite/Join starts clean. */
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

/**
 * Turns a raw Firebase exception into something worth putting in front of a user (and worth
 * grepping Logcat for) instead of a bare "kotlinx.coroutines.JobCancellationException"-style
 * message. The two branches below match the two most common misconfigurations:
 *  - Authentication -> Sign-in method -> Anonymous not enabled in the Firebase Console.
 *  - Realtime Database Rules rejecting the read/write (default rules require auth, so this
 *    usually goes hand-in-hand with the point above).
 * Anything else falls through to a generic-but-still-useful message with the raw exception
 * type/message attached, since this is a best-effort classification (matching on exception
 * class name / message text, not a documented Firebase API), not a guarantee.
 */
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
