package com.example.focusapp.data.repository

import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.io.IOException

/**
 * FakeRemoteDataSource
 * -----------------------
 * Test double for [RemoteDataSource] - no real Firebase project, network,
 * or Auth needed. Set [shouldFailPush] = true to simulate being offline,
 * so FocusRepositoryImplTest can check the offline-first sync contract
 * (saveFocusSession() must never throw just because the cloud push
 * failed) without any real network access.
 */
class FakeRemoteDataSource : RemoteDataSource {

    var shouldFailPush: Boolean = false

    val pushedSessions = mutableListOf<FocusSession>()
    val sentInvites = mutableListOf<Pair<String, String>>() // partyId to toUid
    val respondedInvites = mutableListOf<Pair<String, Boolean>>() // partyId to accept
    val updatedStatuses = mutableListOf<Pair<String, PartyMemberStatus>>() // partyId to status

    override suspend fun getUid(): String = "fake-uid"

    override suspend fun pushSession(session: FocusSession) {
        if (shouldFailPush) throw IOException("simulated offline / push failure")
        pushedSessions.add(session)
    }

    override fun sendPartyInvite(partyId: String, toUid: String) {
        sentInvites.add(partyId to toUid)
    }

    override suspend fun respondToPartyInvite(partyId: String, accept: Boolean) {
        respondedInvites.add(partyId to accept)
    }

    override fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>> =
        flowOf(emptyList())

    override fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus) {
        updatedStatuses.add(partyId to status)
    }
}
