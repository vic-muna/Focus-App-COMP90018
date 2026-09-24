package com.example.focusapp.data.repository

import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

/**
 * FakeRemoteDataSource
 * -----------------------
 * Test double for [RemoteDataSource] - no real Firebase project, network,
 * or Auth needed. Set [shouldFailPush] = true to simulate being offline,
 * so FocusRepositoryImplTest can check the offline-first sync contract
 * (saveFocusSession()/saveFocusZone()/saveAppGroup() must never throw
 * just because the cloud push failed) without any real network access.
 * Set [shouldHangPush] = true to simulate a network call that never
 * resolves either way (no signal, a captive wifi portal, Firebase Auth
 * stuck) rather than one that fails outright - the actual bug behind
 * "Quick Focus ends -> blank screen forever" (and the same risk for
 * saving a Focus Zone / App Group) was this case, not the shouldFailPush
 * one - see FocusRepositoryImpl's syncScope doc comment.
 */
class FakeRemoteDataSource : RemoteDataSource {

    var shouldFailPush: Boolean = false
    var shouldHangPush: Boolean = false

    val pushedSessions = mutableListOf<FocusSession>()
    val pushedZones = mutableListOf<FocusZone>()
    val pushedAppGroups = mutableListOf<AppGroup>()
    val sentInvites = mutableListOf<Pair<String, String>>() // partyId to toUid
    val respondedInvites = mutableListOf<Pair<String, Boolean>>() // partyId to accept
    val updatedStatuses = mutableListOf<Pair<String, PartyMemberStatus>>() // partyId to status

    override suspend fun getUid(): String = "fake-uid"

    override suspend fun pushSession(session: FocusSession) {
        if (shouldHangPush) awaitCancellation() // never returns, never throws, until cancelled
        if (shouldFailPush) throw IOException("simulated offline / push failure")
        pushedSessions.add(session)
    }

    override suspend fun pushFocusZone(zone: FocusZone) {
        if (shouldHangPush) awaitCancellation()
        if (shouldFailPush) throw IOException("simulated offline / push failure")
        pushedZones.add(zone)
    }

    override suspend fun pushAppGroup(group: AppGroup) {
        if (shouldHangPush) awaitCancellation()
        if (shouldFailPush) throw IOException("simulated offline / push failure")
        pushedAppGroups.add(group)
    }

    override fun sendPartyInvite(partyId: String, toUid: String) {
        sentInvites.add(partyId to toUid)
    }

    /** Empty by default - tests that need incoming invites can subclass or wrap this fake;
     *  none of the current tests need a non-empty stream here. */
    override fun observeMyIncomingInvites(): Flow<List<PartyInvite>> = flowOf(emptyList())

    override suspend fun respondToPartyInvite(partyId: String, accept: Boolean) {
        respondedInvites.add(partyId to accept)
    }

    override fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>> =
        flowOf(emptyList())

    override fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus) {
        updatedStatuses.add(partyId to status)
    }
}
