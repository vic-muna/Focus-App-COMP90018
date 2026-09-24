package com.example.focusapp.data.remote

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import kotlinx.coroutines.flow.Flow

/**
 * RemoteDataSource
 * -------------------
 * Cloud sync, mainly needed by the Study Party feature (syncing sessions
 * and other users' live location/focus status).
 *
 * ORIGINAL OPEN DECISION (kept for the record): the project plan hedged
 * between REST API and Firebase, and the Group Member Tasks table
 * reportedly assigned REST work to one member and Firebase work to
 * another. RESOLVED HERE for the Local Data Layer + Study Party scope:
 * implemented against Firebase Realtime Database (see
 * FirebaseRemoteDataSource), since that's what real-time friend-location
 * sync needs and it's what was assigned for this scope.
 *
 * IMPORTANT: if a teammate is separately building a REST backend for
 * zones/app-groups (or anything else), that should be a SEPARATE
 * interface (e.g. RestSyncDataSource) rather than added to this one -
 * confirm this with the team before anyone else implements
 * RemoteDataSource differently, or two conflicting implementations will
 * exist for the same type.
 *
 * See FirebaseRemoteDataSource's doc comment for the exact Realtime
 * Database paths used by each method below.
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
