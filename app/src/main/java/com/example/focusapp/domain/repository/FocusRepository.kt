package com.example.focusapp.domain.repository

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.flow.Flow

/**
 * The one place screens get and save data. It combines the phone's storage (Room)
 * and the cloud (Firebase) - see FocusRepositoryImpl.
 */
interface FocusRepository {

    /** Reads all saved focus zones. */
    suspend fun getFocusZones(): List<FocusZone>

    /** Reads the user's first saved focus zone, or null if none has been set yet. */
    suspend fun getFocusZone(): FocusZone?

    /** Persists the user's one focus zone, overwriting any previously saved value, then
     *  best-effort pushes it to the cloud ("restrictions/plans" - see [syncPendingZoneAndAppGroups]). */
    suspend fun saveFocusZone(zone: FocusZone)

    /** Removes a saved zone from local storage (the cloud copy isn't removed yet). */
    suspend fun deleteFocusZone(zoneId: String)

    suspend fun getAppGroups(): List<AppGroup>

    /** Persists an app group, then best-effort pushes it to the cloud - see [saveFocusZone]. */
    suspend fun saveAppGroup(group: AppGroup)

    /** Retry pushing the saved zone/app groups that haven't reached the cloud yet - the
     *  zone/app-group equivalent of [syncPendingSessions]. Call this from a network-available
     *  callback in addition to the automatic attempt inside saveFocusZone()/saveAppGroup(). */
    suspend fun syncPendingZoneAndAppGroups()

    /** Read past focus sessions for the History screen (local cache, always available offline). */
    suspend fun getSessionHistory(): List<FocusSession>

    /** Sessions started within [fromMillis, toMillis] (inclusive), newest first - for time-interval
     *  analysis (e.g. "this week vs last week", hourly distribution) rather than the full history. */
    suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession>

    /** Persist a completed focus session locally, then best-effort push it to the cloud. */
    suspend fun saveFocusSession(session: FocusSession)

    /** Retry pushing any locally-saved sessions that haven't reached the cloud yet.
     *  Call this from a network-available callback in addition to the
     *  automatic attempt inside saveFocusSession(). */
    suspend fun syncPendingSessions()

    // --- Accounts (see data/account/AccountManager) ---

    /** Downloads the signed-in user's backed-up sessions, zone and app groups into the phone's
     *  storage. Rows already on the phone are kept. Throws if the download fails. */
    suspend fun restoreFromCloud()

    /** Deletes everything in the phone's storage (Room) - used when a different user signs in. */
    suspend fun clearLocalData()

    // --- Study Party (real-time, Firebase-backed - see data/remote) ---

    /** The signed-in user's id (guest or account) - needed so a caller can put itself
     *  into its own PartyMemberStatus.uid before calling [updateMyPartyStatus]. */
    suspend fun getMyUid(): String

    fun sendPartyInvite(partyId: String, toUid: String)

    /** Live stream of every invite currently addressed to this device across every party -
     *  see [com.example.focusapp.data.remote.RemoteDataSource.observeMyIncomingInvites]. */
    fun observeMyIncomingInvites(): Flow<List<PartyInvite>>

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)

    // --- Friends (saved on this phone) ---

    suspend fun getFriends(): List<Friend>

    suspend fun saveFriend(friend: Friend)

    suspend fun deleteFriend(uid: String)
}
