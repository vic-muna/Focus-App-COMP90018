package com.example.focusapp.data.local

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend

/**
 * LocalDataSource
 * ------------------
 * Contract for on-device storage of zones/app groups/sessions.
 *
 * This used to be the concrete Room-backed class directly; it's now an
 * interface so [com.example.focusapp.data.repository.FocusRepositoryImpl]
 * - and its unit tests - can depend on this *behaviour* without needing a
 * real Android Context or SQLite:
 *  - [RoomLocalDataSource] is the real implementation used at runtime
 *    (unchanged logic, just moved - see that file).
 *  - `FakeLocalDataSource` (app/src/test/.../data/repository/) is a plain
 *    in-memory test double used by FocusRepositoryImplTest, so Repository
 *    behaviour (validation, offline-first session sync, single-zone
 *    overwrite-on-save) can be verified as a fast local JVM test with
 *    zero emulator/Robolectric setup.
 *
 * This is purely a "port and adapter" split - nothing else in the branch
 * referenced the concrete class (confirmed: only FocusRepositoryImpl and
 * FocusRepositoryProvider did), so existing call sites just need
 * `LocalDataSource(context)` swapped for `RoomLocalDataSource(context)`.
 */
interface LocalDataSource {

    /** The user's single saved focus zone, or null if none has been set yet. */
    suspend fun getFocusZone(): FocusZone?

    /** Persists the user's one focus zone, overwriting any previously saved value. */
    suspend fun saveFocusZone(zone: FocusZone)

    /** Every saved zone (used by the Geofence code, which supports several zones). */
    suspend fun getFocusZones(): List<FocusZone>

    /** Adds or updates one zone WITHOUT clearing the others (unlike [saveFocusZone]). */
    suspend fun addFocusZone(zone: FocusZone): Boolean

    /** Deletes the zone with this id. */
    suspend fun deleteFocusZone(zoneId: String): Boolean

    suspend fun getAppGroups(): List<AppGroup>

    suspend fun saveAppGroup(group: AppGroup)

    // --- Cloud sync for Focus Zone / App Group (mirrors the session sync methods
    // further below - see RoomLocalDataSource/FocusRepositoryImpl for how these
    // are actually used). "Restrictions/plans" in the original project plan's
    // Remote Data Source description. ---

    /** The saved zone, if any, that hasn't reached Firebase yet - null if there's no
     *  zone or it's already synced. */
    suspend fun getUnsyncedZone(): FocusZone?

    suspend fun markZoneSynced(zoneId: String)

    /** App groups that haven't reached Firebase yet. */
    suspend fun getUnsyncedAppGroups(): List<AppGroup>

    suspend fun markAppGroupSynced(groupId: String)

    suspend fun getSessionHistory(): List<FocusSession>

    /** Sessions whose startTimeMillis falls within [fromMillis, toMillis] (inclusive), newest first.
     *  Used for time-interval analysis (e.g. "this week vs last week") rather than the full history. */
    suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession>

    suspend fun saveFocusSession(session: FocusSession)

    /** Rows not yet pushed to Firebase. */
    suspend fun getUnsyncedSessions(): List<FocusSession>

    suspend fun markSessionSynced(sessionId: String)

    // --- Friends (local address book for the Study Party invite picker - see
    // ui/screens/party/FriendListScreen.kt. No server-side "friend request" concept;
    // each side just saves the other's uid locally.) ---

    suspend fun getFriends(): List<Friend>

    /** Adds a friend, or overwrites the existing one with the same uid (e.g. renaming). */
    suspend fun saveFriend(friend: Friend)

    suspend fun deleteFriend(uid: String)
}
