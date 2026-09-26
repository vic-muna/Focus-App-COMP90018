package com.example.focusapp.data.repository

import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.remote.RemoteDataSource
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import com.example.focusapp.domain.model.PartyInvite
import com.example.focusapp.domain.model.PartyMemberStatus
import com.example.focusapp.domain.repository.FocusRepository
import com.example.focusapp.domain.validation.FocusValidation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
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

// How long a single Firebase push gets before syncPendingSessions()/syncPendingZoneAndAppGroups()
// give up on it and treat it as "still offline, retry later" - see syncPendingSessions()'s doc
// comment for why this exists at all (a hung network call, not just a *failed* one, is the
// actual bug it fixes).
private const val FIREBASE_PUSH_TIMEOUT_MILLIS = 15_000L

/**
 * FocusRepositoryImpl
 * ----------------------
 * Concrete implementation of [FocusRepository]. Zones/App Groups and
 * Sessions are all offline-first the same way: always written to Room
 * immediately, then pushed to Firebase opportunistically - none of the
 * save*() methods below fail just because the phone is offline. Study
 * Party calls pass straight through to RemoteDataSource since that
 * feature is inherently live/online-only.
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
    private val clock: () -> Long = System::currentTimeMillis,
    // Where saveFocusSession()/saveFocusZone()/saveAppGroup()'s best-effort cloud sync actually
    // runs. Defaults to a real, app-lifetime background scope (SupervisorJob so one bad push can
    // never cancel the others) so a slow/hung network call NEVER blocks whoever called those
    // methods - this used to be awaited inline here, which is exactly what caused "Quick Focus
    // ends -> screen stays blank forever, only a restart fixes it" (and would have caused the
    // same for saving a Focus Zone or App Group) whenever the device's network was slow,
    // unreachable, or Firebase Anonymous Auth just took a while to respond: the calling UI's own
    // coroutine was suspended on this call, so its navigate-away callback never ran. Tests
    // override this with an Unconfined scope - see FocusRepositoryImplTest - so the fakes'
    // (non-suspending) work still finishes before each test's next line runs, keeping them
    // exactly as deterministic as before this change.
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : FocusRepository {

    // Last status actually pushed to Firebase, keyed by "partyId/uid", so repeat calls for the
    // same party+user can be throttled - see updateMyPartyStatus() below. Not persisted; a fresh
    // process (or a fresh FocusRepositoryImpl in tests) always lets the first call for a key
    // through, which is correct - we always want a party member's very first status to show up.
    private val lastPushedPartyStatus = mutableMapOf<String, Pair<PartyMemberStatus, Long>>()

    override suspend fun getFocusZone(): FocusZone? =
        localDataSource.getFocusZone()

    // [Claude, 2026-09-26] Local-only for now - there's no RemoteDataSource
    // delete yet, so a synced zone's Firebase copy is left behind.
    // [HANDOFF -> Victor Munacoha / Yu-Hao Lu | README task: "Cloud REST API integration" / "Firebase real-time sync"]
    // Add a RemoteDataSource delete (e.g. deleteFocusZone(zoneId)) and call it
    // here best-effort via syncScope, the same way saveFocusZone() pushes.
    override suspend fun deleteFocusZone(zoneId: String) {
        localDataSource.deleteFocusZone(zoneId)
    }

    override suspend fun saveFocusZone(zone: FocusZone) {
        FocusValidation.validateFocusZone(zone)
        localDataSource.saveFocusZone(zone)
        // Fire-and-forget - see syncScope's doc comment (same "must never block the caller on a
        // hung network call" reasoning as saveFocusSession(), just applied here too).
        syncScope.launch { syncPendingZoneAndAppGroups() }
    }

    override suspend fun getAppGroups(): List<AppGroup> =
        localDataSource.getAppGroups()

    override suspend fun saveAppGroup(group: AppGroup) {
        FocusValidation.validateAppGroup(group)
        localDataSource.saveAppGroup(group)
        syncScope.launch { syncPendingZoneAndAppGroups() }
    }

    /** Same "retry whatever hasn't reached Firebase yet" shape as [syncPendingSessions] -
     *  called opportunistically from saveFocusZone/saveAppGroup above, and safe to call again
     *  from a network-available callback (not wired up anywhere yet, same as sessions'). Each
     *  push gets the same [FIREBASE_PUSH_TIMEOUT_MILLIS] treatment as sessions - see
     *  [syncPendingSessions]'s doc comment for why a hang (not just a failure) needs one. */
    override suspend fun syncPendingZoneAndAppGroups() {
        localDataSource.getUnsyncedZone()?.let { zone ->
            try {
                withTimeout(FIREBASE_PUSH_TIMEOUT_MILLIS) {
                    remoteDataSource.pushFocusZone(zone)
                }
                localDataSource.markZoneSynced(zone.id)
            } catch (e: TimeoutCancellationException) {
                // Treated exactly like any other failed push - stays unsynced, retried next time.
            } catch (e: CancellationException) {
                throw e // real cancellation (app/scope shutting down) - must not be swallowed
            } catch (e: Exception) {
                // Left unsynced on purpose - call this again once back online.
            }
        }
        localDataSource.getUnsyncedAppGroups().forEach { group ->
            try {
                withTimeout(FIREBASE_PUSH_TIMEOUT_MILLIS) {
                    remoteDataSource.pushAppGroup(group)
                }
                localDataSource.markAppGroupSynced(group.id)
            } catch (e: TimeoutCancellationException) {
                // Treated exactly like any other failed push - stays unsynced, retried next time.
            } catch (e: CancellationException) {
                throw e // real cancellation (app/scope shutting down) - must not be swallowed
            } catch (e: Exception) {
                // Left unsynced on purpose - call this again once back online.
            }
        }
    }

    override suspend fun getSessionHistory(): List<FocusSession> =
        localDataSource.getSessionHistory()

    override suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession> =
        localDataSource.getSessionsBetween(fromMillis, toMillis)

    override suspend fun saveFocusSession(session: FocusSession) {
        localDataSource.saveFocusSession(session)
        // Fire-and-forget on purpose - see syncScope's doc comment above for why this must
        // NOT be `syncPendingSessions()` awaited directly here.
        syncScope.launch { syncPendingSessions() }
    }

    /**
     * Retries pushing any locally-saved sessions that haven't reached the cloud yet. Each push
     * gets [FIREBASE_PUSH_TIMEOUT_MILLIS] before being treated as failed - a network call that
     * hangs (no signal, a captive/blocked wifi portal, Firebase Auth waiting on a response that
     * never comes) is just as much "still offline" as one that fails outright, and without a
     * timeout it would sit here forever holding a coroutine open instead of ever getting marked
     * unsynced-and-retry-later.
     */
    override suspend fun syncPendingSessions() {
        localDataSource.getUnsyncedSessions().forEach { session ->
            try {
                withTimeout(FIREBASE_PUSH_TIMEOUT_MILLIS) {
                    remoteDataSource.pushSession(session)
                }
                localDataSource.markSessionSynced(session.id)
            } catch (e: TimeoutCancellationException) {
                // Treated exactly like any other failed push - stays unsynced, retried next time.
            } catch (e: CancellationException) {
                throw e // real cancellation (app/scope shutting down) - must not be swallowed
            } catch (e: Exception) {
                // Left unsynced on purpose - call this again once back online.
            }
        }
    }

    override suspend fun getMyUid(): String = remoteDataSource.getUid()

    override fun sendPartyInvite(partyId: String, toUid: String) =
        remoteDataSource.sendPartyInvite(partyId, toUid)

    override fun observeMyIncomingInvites(): Flow<List<PartyInvite>> =
        remoteDataSource.observeMyIncomingInvites()

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
