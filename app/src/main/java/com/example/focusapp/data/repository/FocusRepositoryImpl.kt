package com.example.focusapp.data.repository

import android.content.Context
import com.example.focusapp.data.ai.FocusCoach
import com.example.focusapp.data.ai.GeminiFocusCoach
import com.example.focusapp.data.local.LocalDataSource
import com.example.focusapp.data.preferences.FocusCoachStorage
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
 * The real [FocusRepository].
 * Saving always writes to the phone (Room) first, then uploads to Firebase
 * when it can - so saving works offline. Party Mode goes straight to Firebase.
 * Zones and app groups are checked by [FocusValidation] before saving.
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
    private val syncScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    // [Claude, 2026-10-04] Only used for per-session AI feedback (see saveFocusSession() below) -
    // null in every test (FocusRepositoryImplTest never passes one), which is also exactly how
    // production behaves before the user has ever opened Focus Coach: no consent/quota record
    // exists yet, generateAndCacheSessionFeedback() checks that first and returns early, so a
    // null context here isn't a special case to work around, it's the same "nothing to do yet"
    // path a real context would also take for a first-time user.
    private val context: Context? = null,
) : FocusRepository {

    // [Claude, 2026-10-04 fix] `by lazy` is a property delegate - valid on a val declared in a
    // class BODY, not inside a primary constructor's parameter list (that's a parameter
    // declaration, not a property-with-getter one). Having it in the constructor above was a
    // syntax error that threw off the parser for the rest of the file, which is why the
    // original error log showed dozens of unrelated-looking "unresolved reference" lines below
    // this point - none of those were really wrong on their own, the parser just lost its place
    // after this line failed to parse.
    private val focusCoach: FocusCoach by lazy { GeminiFocusCoach() }

    // Last status actually pushed to Firebase, keyed by "partyId/uid", so repeat calls for the
    // same party+user can be throttled - see updateMyPartyStatus() below. Not persisted; a fresh
    // process (or a fresh FocusRepositoryImpl in tests) always lets the first call for a key
    // through, which is correct - we always want a party member's very first status to show up.
    private val lastPushedPartyStatus = mutableMapOf<String, Pair<PartyMemberStatus, Long>>()

    override suspend fun getFocusZones(): List<FocusZone> =
        localDataSource.getFocusZones()

    override suspend fun getFocusZone(): FocusZone? =
        localDataSource.getFocusZones().firstOrNull()

    // Deletes on the phone first, then the Firebase copy (best-effort, fire-and-forget like every
    // other cloud call here - see syncScope's doc comment). Limitation: a delete made while
    // offline is NOT retried later - there's no record on the phone of "this zone was deleted"
    // once its row is gone - so that zone's cloud copy stays and would come back on the next
    // restore. Fixing that needs a small "pending deletes" list; not built yet.
    override suspend fun deleteFocusZone(zoneId: String) {
        localDataSource.deleteFocusZone(zoneId)
        syncScope.launch { pushFocusZoneDelete(zoneId) }
    }

    private suspend fun pushFocusZoneDelete(zoneId: String) {
        try {
            withTimeout(FIREBASE_PUSH_TIMEOUT_MILLIS) {
                remoteDataSource.deleteFocusZone(zoneId)
            }
        } catch (e: TimeoutCancellationException) {
            // Treated exactly like any other failed call - see deleteFocusZone()'s limitation note.
        } catch (e: CancellationException) {
            throw e // real cancellation (app/scope shutting down) - must not be swallowed
        } catch (e: Exception) {
            // Offline / not signed in: the phone's copy is already gone, which is what matters here.
        }
    }

    override suspend fun saveFocusZone(zone: FocusZone) {
        FocusValidation.validateFocusZone(zone)
        localDataSource.addFocusZone(zone)
        // Fire-and-forget - see syncScope's doc comment
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
        // Every unsynced zone, each to its own cloud node - this used to push only the FIRST one
        // per call, all to the same single node, so each zone overwrote the previous one there.
        localDataSource.getUnsyncedZones().forEach { zone ->
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
        syncScope.launch { generateAndCacheSessionFeedback(session) }
    }

    /**
     * [Claude, 2026-10-04] Generates [session]'s Focus Coach line and caches it locally - same
     * fire-and-forget reasoning as syncPendingSessions() above (a slow/failed Gemini call must
     * never delay saving the session). Shares FocusCoachStorage's consent flag and
     * DAILY_LIMIT counter with the weekly report/follow-ups (see that class's doc comment) -
     * deliberately the SAME quota, not a separate one: both features hit the same free-tier
     * Gemini budget, and a user doing several Quick Focus sessions a day could otherwise burn
     * through it before ever opening Focus Coach's weekly view. A null [context] (every test,
     * and production before Focus Coach has ever been opened once) just means there's no
     * consent/quota record yet, same as a real context would show a first-time user - see this
     * class's constructor doc comment.
     */
    private suspend fun generateAndCacheSessionFeedback(session: FocusSession) {
        val appContext = context ?: return
        val durationMinutes = ((session.endTimeMillis ?: return) - session.startTimeMillis) / 60_000
        val storage = FocusCoachStorage(appContext)
        if (!storage.hasConsent()) return
        if (storage.requestsUsedToday() >= FocusCoachStorage.DAILY_LIMIT) return
        val feedback = try {
            focusCoach.sessionFeedback(
                durationMinutes = durationMinutes,
                distractingAppOpenCount = session.distractingAppOpenCount,
                wasCompletedSuccessfully = session.wasCompletedSuccessfully
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return // Same "just don't show one this time" contract as a declined/unreachable call.
        }
        storage.recordRequest()
        localDataSource.saveSessionFeedback(session.id, feedback)
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

    override suspend fun restoreFromCloud() {
        val cloud = withTimeout(FIREBASE_PUSH_TIMEOUT_MILLIS) { remoteDataSource.fetchUserData() }
        localDataSource.importFromCloud(
            sessions = cloud.sessions,
            zones = cloud.zones,
            appGroups = cloud.appGroups,
        )
    }

    override suspend fun clearLocalData() {
        localDataSource.clearAll()
        // Forget throttle state from the previous user's party.
        lastPushedPartyStatus.clear()
    }

    override suspend fun getMyUid(): String = remoteDataSource.getUid()

    override suspend fun sendPartyInvite(partyId: String, toUid: String) =
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
     *  - isFocusing, accessibilityReady, waitingForPermission or displayName changed (a real state change, not just noisy GPS jitter -
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
            previous.first.accessibilityReady != status.accessibilityReady ||
            previous.first.waitingForPermission != status.waitingForPermission ||
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

    override suspend fun getOrCreateMyFriendCode(): String = remoteDataSource.getOrCreateMyFriendCode()

    override suspend fun resolveFriendCode(code: String): String? = remoteDataSource.resolveFriendCode(code)
}
