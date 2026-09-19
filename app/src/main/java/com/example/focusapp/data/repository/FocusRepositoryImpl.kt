package com.example.focusapp.data.repository

import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyMemberStatus
import com.example.focusapp.domain.repository.FocusRepository
import com.example.focusapp.domain.validation.FocusValidation
import kotlinx.coroutines.flow.Flow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

// --- updateMyPartyStatus() throttle tuning (battery-drain answer - see chat/README) ---
// A naive "call updateMyPartyStatus() on every GPS fix" burns both battery (radio wakeups)
// and Firebase write quota. These two thresholds are OR'd together: a push goes through once
// EITHER enough time has passed OR the device has moved far enough, so a stationary user still
// heartbeats occasionally but a fast-moving one doesn't wait the full interval to update friends.
private const val PARTY_STATUS_MIN_INTERVAL_MILLIS = 30_000L
private const val PARTY_STATUS_MIN_DISTANCE_METERS = 20.0
private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * FocusRepositoryImpl
 * ----------------------
 * Concrete implementation of [FocusRepository]. Zones/App Groups are
 * local-only for now (no remote sync needed for those yet). Sessions are
 * offline-first: always written to Room immediately, then pushed to
 * Firebase opportunistically - saveFocusSession() never fails just
 * because the phone is offline. Study Party calls pass straight through
 * to RemoteDataSource since that feature is inherently live/online-only.
 *
 * saveFocusZone/saveAppGroup run their input through [FocusValidation]
 * first and throw [IllegalArgumentException] on bad data (blank name,
 * non-positive radius, etc.) *before* touching localDataSource - so a
 * rejected save never reaches Room at all. See FocusRepositoryImplTest.
 */
class FocusRepositoryImpl(
    private val localDataSource: LocalDataSource,
    private val remoteDataSource: RemoteDataSource,
    // Injectable purely so tests can control "how much time has passed" without a real
    // Thread.sleep() - defaults to the real wall clock everywhere else. See
    // FocusRepositoryImplTest's throttle tests for how this is used.
    private val clock: () -> Long = System::currentTimeMillis
) : FocusRepository {

    // Last status actually pushed to Firebase, keyed by "partyId/uid", so repeat calls for the
    // same party+user can be throttled - see updateMyPartyStatus() below. Not persisted; a fresh
    // process (or a fresh FocusRepositoryImpl in tests) always lets the first call for a key
    // through, which is correct - we always want a party member's very first status to show up.
    private val lastPushedPartyStatus = mutableMapOf<String, Pair<PartyMemberStatus, Long>>()

    override suspend fun getFocusZone(): FocusZone? =
        localDataSource.getFocusZone()

    override suspend fun saveFocusZone(zone: FocusZone) {
        FocusValidation.validateFocusZone(zone)
        localDataSource.saveFocusZone(zone)
    }

    override suspend fun getAppGroups(): List<AppGroup> =
        localDataSource.getAppGroups()

    override suspend fun saveAppGroup(group: AppGroup) {
        FocusValidation.validateAppGroup(group)
        localDataSource.saveAppGroup(group)
    }

    override suspend fun getSessionHistory(): List<FocusSession> =
        localDataSource.getSessionHistory()

    override suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession> =
        localDataSource.getSessionsBetween(fromMillis, toMillis)

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

    override suspend fun getMyUid(): String = remoteDataSource.getUid()

    override fun sendPartyInvite(partyId: String, toUid: String) =
        remoteDataSource.sendPartyInvite(partyId, toUid)

    override suspend fun respondToPartyInvite(partyId: String, accept: Boolean) =
        remoteDataSource.respondToPartyInvite(partyId, accept)

    override fun observePartyMembers(partyId: String): Flow<List<PartyMemberStatus>> =
        remoteDataSource.observePartyMembers(partyId)

    /**
     * Throttled pass-through to RemoteDataSource. If a caller (e.g. a GPS callback) calls this
     * on every new location fix, that would fire a Firebase write - and a radio wakeup - every
     * few seconds per user, which is both a battery and a cost problem. This lets a status
     * through only when:
     *  - it's the first status we've ever pushed for this partyId+uid (always show up), OR
     *  - isFocusing or displayName changed (a real state change, not just noisy GPS jitter -
     *    friends should see "started focusing" immediately, not up to 30s late), OR
     *  - at least [PARTY_STATUS_MIN_INTERVAL_MILLIS] has passed since the last push, OR
     *  - the device has moved at least [PARTY_STATUS_MIN_DISTANCE_METERS] since the last push.
     * Everything else is dropped - the caller can keep calling this on every GPS tick without
     * worrying about throttling itself; that's this data layer's job, not the sensor/UI layer's.
     */
    override fun updateMyPartyStatus(partyId: String, status: PartyMemberStatus) {
        val key = "$partyId/${status.uid}"
        val now = clock()
        val previous = lastPushedPartyStatus[key]

        val shouldPush = previous == null ||
            previous.first.isFocusing != status.isFocusing ||
            previous.first.displayName != status.displayName ||
            now - previous.second >= PARTY_STATUS_MIN_INTERVAL_MILLIS ||
            distanceMetersBetween(previous.first, status) >= PARTY_STATUS_MIN_DISTANCE_METERS

        if (!shouldPush) return

        lastPushedPartyStatus[key] = status to now
        remoteDataSource.updateMyPartyStatus(partyId, status)
    }

    /** Haversine distance in meters. Returns Double.MAX_VALUE (i.e. "definitely moved") if either
     *  status is missing a location fix, so a status only just gaining/losing GPS still pushes. */
    private fun distanceMetersBetween(a: PartyMemberStatus, b: PartyMemberStatus): Double {
        val lat1 = a.latitude
        val lon1 = a.longitude
        val lat2 = b.latitude
        val lon2 = b.longitude
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) return Double.MAX_VALUE

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val h = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return EARTH_RADIUS_METERS * 2 * atan2(sqrt(h), sqrt(1 - h))
    }

    override suspend fun getFriends(): List<Friend> = localDataSource.getFriends()

    override suspend fun saveFriend(friend: Friend) = localDataSource.saveFriend(friend)

    override suspend fun deleteFriend(uid: String) = localDataSource.deleteFriend(uid)
}
