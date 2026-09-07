package com.example.focusapp.data.repository

import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.PartyMemberStatus
import com.example.focusapp.domain.repository.FocusRepository
import kotlinx.coroutines.flow.Flow

/**
 * FocusRepositoryImpl
 * ----------------------
 * Concrete implementation of [FocusRepository]. Zones/App Groups are
 * local-only for now (no remote sync needed for those yet). Sessions are
 * offline-first: always written to Room immediately, then pushed to
 * Firebase opportunistically - saveFocusSession() never fails just
 * because the phone is offline. Study Party calls pass straight through
 * to RemoteDataSource since that feature is inherently live/online-only.
 */
class FocusRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource
) : FocusRepository {

    override suspend fun getFocusZones(): List<FocusZone> =
        localDataSource.getFocusZones()

    override suspend fun saveFocusZone(zone: FocusZone) =
        localDataSource.saveFocusZone(zone)

    override suspend fun getAppGroups(): List<AppGroup> =
        localDataSource.getAppGroups()

    override suspend fun saveAppGroup(group: AppGroup) =
        localDataSource.saveAppGroup(group)

    override suspend fun getSessionHistory(): List<FocusSession> =
        localDataSource.getSessionHistory()

    override suspend fun saveFocusSession(session: FocusSession) {
        localDataSource.saveFocusSession(session)
        syncPendingSessions() // best-effort; fine if this fails while offline
    }

    override suspend fun syncPendingSessions() {
        localDataSource.getUnsyncedSessions().forEach { session ->
            try {
                remoteDataSource.pushSession(session)
                localDataSource.markSessionSynced(session.id)
            } catch (e: Exception) {
                // Left unsynced on purpose - call this again once back online.
            }
        }
    }

    override fun sendPartyInvite(partyId: String, toUid: String) =
        remoteDataSource.sendPartyInvite(partyId, toUid)

    override suspend fun respondToPartyInvite(partyId: String, accept: Boolean) =
        remoteDataSource.respondToPartyInvite(partyId, accept)

    override fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>> =
        remoteDataSource.observePartyMembers(partyId)

    override fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus) =
        remoteDataSource.updateMyPartyStatus(partyId, status)
}
