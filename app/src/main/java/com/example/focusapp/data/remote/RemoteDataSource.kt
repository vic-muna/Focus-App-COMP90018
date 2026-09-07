package com.example.focusapp.data.remote

import com.example.focusapp.domain.model.FocusSession
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
 */
interface RemoteDataSource {

    /** Current user's stable id, signing in anonymously if this is the
     *  first call (there is no login screen anywhere in the app yet). */
    suspend fun getUid(): String

    /** Push one completed session to the cloud (used by the offline-sync flow). */
    suspend fun pushSession(session: FocusSession)

    fun sendPartyInvite(partyId: String, toUid: String)

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party - collect this in a ViewModel. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)
}
