package com.example.focusapp.data.local

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend

/**
 * What on-phone storage can do. [RoomLocalDataSource] is the real one;
 * tests use a fake in-memory one (FakeLocalDataSource).
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

    // --- Accounts (see AccountManager) ---

    /** Saves rows downloaded from the cloud, marked as already synced. Rows already on the
     *  phone (same id) are kept as they are - the phone's copy may be newer. */
    suspend fun importFromCloud(sessions: List<FocusSession>, zones: List<FocusZone>, appGroups: List<AppGroup>)

    /** Deletes everything stored here - used when a different user signs in on this phone. */
    suspend fun clearAll()

    // --- Friends (saved on this phone) ---

    suspend fun getFriends(): List<Friend>

    /** Adds a friend, or overwrites the existing one with the same uid (e.g. renaming). */
    suspend fun saveFriend(friend: Friend)

    suspend fun deleteFriend(uid: String)
}
