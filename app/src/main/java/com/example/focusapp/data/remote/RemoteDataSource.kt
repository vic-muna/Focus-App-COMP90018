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

    /** The signed-in user's stable id (a guest's or an account's - see AccountManager).
     *  Throws if nobody is signed in. */
    suspend fun getUid(): String

    /** Downloads everything this user has backed up under users/{uid} - used to fill a
     *  new phone (or a reinstall) after logging in. */
    suspend fun fetchUserData(): CloudUserData

    /** Push one completed session to the cloud (used by the offline-sync flow). */
    suspend fun pushSession(session: FocusSession)

    /** Push the user's one focus zone to the cloud - "restrictions/plans" in the original
     *  project plan's Remote Data Source description. Used by the same offline-first
     *  sync flow as [pushSession] (see FocusRepositoryImpl.syncPendingZoneAndAppGroups()). */
    suspend fun pushFocusZone(zone: FocusZone)

    /** Push one app group ("restrictions/plans") to the cloud - see [pushFocusZone]. */
    suspend fun pushAppGroup(group: AppGroup)

    /** Was fire-and-forget (fun, not suspend) - changed to suspend so a failed write (most
     *  likely cause: Realtime Database Rules not letting one user write into another user's
     *  users/$toUid/incomingInvites node - the default rules only let a user write their own
     *  users/$uid subtree) actually throws instead of disappearing silently. See
     *  PartyModeViewModel.inviteFriend() for where that's now caught and shown. */
    suspend fun sendPartyInvite(partyId: String, toUid: String)

    /** Live stream of every invite currently addressed to this device's own uid, across every
     *  party - what an "Invites" list/badge in the UI observes. An invite disappears from this
     *  stream once [respondToPartyInvite] has been called for it (accepted or declined). */
    fun observeMyIncomingInvites(): Flow<List<PartyInvite>>

    /**
     * [Claude, 2026-10-03] A short (6-character), memorable stand-in for this user's own
     * Firebase uid, specifically so one friend can tell another "add me, my code is XYZ123"
     * instead of reading out a 28-character uid. Generated once per user and then reused every
     * time (stored at users/$uid/friendCode, looked up first before generating a new one) -
     * calling this twice in a row returns the same code. Collision-checked against
     * friendCodes/$code before being claimed; retries with a new random code on collision
     * (vanishingly rare at this app's scale - six characters from a 32-symbol alphabet is over
     * a billion combinations - but checked rather than assumed).
     *
     * Deliberately separate from AccountManager's username system (see that class): a username
     * requires creating a password-protected account, which is a bigger commitment than "give
     * my friend a short code to paste in" - this works the same for a guest or an account
     * holder, with no sign-up step of its own.
     */
    suspend fun getOrCreateMyFriendCode(): String

    /** The uid [code] belongs to, or null if no such code has been claimed (typo, or the code
     *  was never generated because that user hasn't opened Party Mode's Friends screen yet). */
    suspend fun resolveFriendCode(code: String): String?

    suspend fun respondToPartyInvite(partyId: String, accept: Boolean)

    /** Live stream of every member's status in a party - collect this in a ViewModel. */
    fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>>

    fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus)
}

/** What [RemoteDataSource.fetchUserData] downloads: the user's backed-up sessions, zone and app groups. */
data class CloudUserData(
    val sessions: List<FocusSession> = emptyList(),
    val zone: FocusZone? = null,
    val appGroups: List<AppGroup> = emptyList(),
)
