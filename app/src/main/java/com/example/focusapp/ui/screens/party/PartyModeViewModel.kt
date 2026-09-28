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
 * Party Mode's cloud logic (Firebase), used by [FriendsScreen].
 *
 * The 6-letter group code is the party's id in Firebase. Both the host and
 * the people joining call [joinParty] with that code, which:
 *  1. watches everyone in the party ([members]), and
 *  2. keeps publishing this phone's own status (location, focusing or not).
 *
 * Every Firebase call is wrapped in try/catch: a failure shows up in
 * [errorMessage] instead of crashing the app.
 * Invites ([inviteFriend], [incomingInvites]) are ready for the friend ID system.
 */
class PartyModeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FocusRepositoryProvider.get(application)
    private val sensorDataSource = SensorDataSource()

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

    init {
        viewModelScope.launch {
            repository.observeMyIncomingInvites()
                .catch { /* stay empty */ }
                .collect { _incomingInvites.value = it }
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
        val partyId = currentPartyId ?: return
        try {
            repository.sendPartyInvite(partyId, toUid)
        } catch (e: Exception) {
            _errorMessage.value = friendlyPartyErrorMessage(e)
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
