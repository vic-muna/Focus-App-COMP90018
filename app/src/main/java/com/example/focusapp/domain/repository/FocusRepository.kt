package com.example.focusapp.domain.repository

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
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

    suspend fun getFocusZones(): List<FocusZone>

    suspend fun saveFocusZone(zone: FocusZone)

    suspend fun getAppGroups(): List<AppGroup>

    suspend fun saveAppGroup(group: AppGroup)

    /** Read past focus sessions for the History screen (local cache, always available offline). */
    suspend fun getSessionHistory(): List<FocusSession>

    /** Persist a completed focus session locally, then best-effort push it to the cloud. */
    suspend fun saveFocusSession(session: FocusSession)

    /** Retry pushing any locally-saved sessions that haven't reached the cloud yet.
     *  Call this from a network-available callback in addition to the
     *  automatic attempt inside saveFocusSession(). */
    suspend fun syncPendingSessions()

    // --- Study Party (real-time, Firebase-backed - see data/remote) ---

    fun sendPartyInvite(partyId: String, toUid: String)

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)
}
