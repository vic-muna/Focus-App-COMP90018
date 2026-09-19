package com.example.focusapp.domain.repository

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.flow.Flow

/**
 * FocusRepository
 * ------------------
 * The single source of truth that the Domain/UI layers talk to. The real
 * implementation ([com.example.focusapp.data.repository.FocusRepositoryImpl])
 * combines Local (Room) and Remote (Firebase) data sources.
 */
interface FocusRepository {

    /** Reads the user's single saved focus zone, or null if none has been set yet. */
    suspend fun getFocusZone(): FocusZone?

    /** Persists the user's one focus zone, overwriting any previously saved value. */
    suspend fun saveFocusZone(zone: FocusZone)

    suspend fun getAppGroups(): List<AppGroup>

    suspend fun saveAppGroup(group: AppGroup)

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

    // --- Study Party (real-time, Firebase-backed - see data/remote) ---

    /** This device's stable per-install id (anonymous-auth uid) - needed so a caller can put itself
     *  into its own PartyMemberStatus.uid before calling [updateMyPartyStatus]. */
    suspend fun getMyUid(): String

    fun sendPartyInvite(partyId: String, toUid: String)

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)

    // --- Friends (local address book feeding the Study Party invite picker -
    // see ui/screens/party/FriendListScreen.kt) ---

    suspend fun getFriends(): List<Friend>

    suspend fun saveFriend(friend: Friend)

    suspend fun deleteFriend(uid: String)
}
