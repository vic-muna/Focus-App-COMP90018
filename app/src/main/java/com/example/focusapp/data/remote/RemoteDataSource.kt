package com.example.focusapp.data.remote

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.flow.Flow

/**
 * What cloud storage can do: back up sessions and run Party Mode.
 * The real one is [FirebaseRemoteDataSource].
 */
interface RemoteDataSource {

    /** Current user's stable id, signing in anonymously if this is the
     *  first call (there is no login screen anywhere in the app yet). */
    suspend fun getUid(): String

    /** Push one completed session to the cloud (used by the offline-sync flow). */
    suspend fun pushSession(session: FocusSession)

    /** Push the user's one focus zone to the cloud - "restrictions/plans" in the original
     *  project plan's Remote Data Source description. Used by the same offline-first
     *  sync flow as [pushSession] (see FocusRepositoryImpl.syncPendingZoneAndAppGroups()). */
    suspend fun pushFocusZone(zone: FocusZone)

    /** Push one app group ("restrictions/plans") to the cloud - see [pushFocusZone]. */
    suspend fun pushAppGroup(group: AppGroup)

    fun sendPartyInvite(partyId: String, toUid: String)

    /** Live stream of every invite currently addressed to this device's own uid, across every
     *  party - what an "Invites" list/badge in the UI observes. An invite disappears from this
     *  stream once [respondToPartyInvite] has been called for it (accepted or declined). */
    fun observeMyIncomingInvites(): Flow<List<PartyInvite>>

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party - collect this in a ViewModel. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)
}
