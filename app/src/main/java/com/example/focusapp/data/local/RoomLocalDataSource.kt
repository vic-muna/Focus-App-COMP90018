package com.example.focusapp.data.local

import android.content.Context
import com.example.focusapp.data.local.entity.toDomain
import com.example.focusapp.data.local.entity.toEntity
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.Friend
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The real [LocalDataSource], backed by Room.
 * It turns domain models into database rows (entity/) and back, using the DAOs (dao/).
 */
class RoomLocalDataSource(context: Context) : LocalDataSource {

    private val appContext = context.applicationContext
    private val db = FocusAppDatabase.getInstance(context)

    // The first saved zone - kept for callers that only deal with one zone; getFocusZones() has all of them.
    override suspend fun getFocusZone(): FocusZone? =
        db.focusZoneDao().getAll().firstOrNull()?.toDomain()

    // Upsert by id: a new id adds a zone, an existing id updates it in place. Other zones are
    // kept - this used to clear the table first (single-zone), but the app supports several
    // zones now. Same behaviour as addFocusZone() below; deleteFocusZone() removes one.
    override suspend fun saveFocusZone(zone: FocusZone) {
        db.focusZoneDao().upsert(zone.toEntity())
    }

    override suspend fun getFocusZones(): List<FocusZone> =
        db.focusZoneDao().getAll().map { it.toDomain() }

    override suspend fun addFocusZone(zone: FocusZone): Boolean =
        runCatching { db.focusZoneDao().upsert(zone.toEntity()) }.isSuccess

    override suspend fun deleteFocusZone(zoneId: String): Boolean =
        runCatching { db.focusZoneDao().deleteById(zoneId) }.isSuccess

    override suspend fun getAppGroups(): List<AppGroup> =
        db.appGroupDao().getAll().map { it.toDomain() }

    override suspend fun saveAppGroup(group: AppGroup) =
        db.appGroupDao().upsert(group.toEntity())

    // --- Cloud sync for zone/app groups (mirrors the session sync methods below -
    // see LocalDataSource's doc comment and FocusRepositoryImpl for how these are used). ---

    override suspend fun getUnsyncedZones(): List<FocusZone> {
        resetZoneSyncFlagsOnce()
        return db.focusZoneDao().getUnsynced().map { it.toDomain() }
    }

    /**
     * Zones used to be pushed to ONE shared cloud node, one per sync call - each push overwrote
     * the previous zone there, yet every zone was still marked synced locally. So after several
     * zones, rows flagged "synced" may never actually have reached the cloud. Now that every
     * zone has its own node, flag them all unsynced ONCE so the next sync uploads each of them
     * to its new node (a zone that was already there is simply written again). Guarded by a
     * flag so it only ever happens once per install, not on every sync.
     */
    private suspend fun resetZoneSyncFlagsOnce() {
        val prefs = appContext.getSharedPreferences(ZONE_SYNC_PREFS, Context.MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ZONES_PER_ID_RESYNC_DONE, false)) return
        db.focusZoneDao().markAllUnsynced()
        prefs.edit().putBoolean(KEY_ZONES_PER_ID_RESYNC_DONE, true).apply()
    }

    override suspend fun markZoneSynced(zoneId: String) =
        db.focusZoneDao().markSynced(zoneId)

    override suspend fun getUnsyncedAppGroups(): List<AppGroup> =
        db.appGroupDao().getUnsynced().map { it.toDomain() }

    override suspend fun markAppGroupSynced(groupId: String) =
        db.appGroupDao().markSynced(groupId)

    override suspend fun getSessionHistory(): List<FocusSession> =
        db.focusSessionDao().getAll().map { it.toDomain() }

    override suspend fun getSessionsBetween(fromMillis: Long, toMillis: Long): List<FocusSession> =
        db.focusSessionDao().getBetween(fromMillis, toMillis).map { it.toDomain() }

    override suspend fun saveFocusSession(session: FocusSession) =
        db.focusSessionDao().insert(session.toEntity())

    /** Rows not yet pushed to Firebase. */
    override suspend fun getUnsyncedSessions(): List<FocusSession> =
        db.focusSessionDao().getUnsynced().map { it.toDomain() }

    override suspend fun markSessionSynced(sessionId: String) =
        db.focusSessionDao().markSynced(sessionId)

    override suspend fun saveSessionFeedback(sessionId: String, feedback: String) =
        db.focusSessionDao().updateAiFeedback(sessionId, feedback)

    override suspend fun importFromCloud(sessions: List<FocusSession>, zones: List<FocusZone>, appGroups: List<AppGroup>) {
        db.focusSessionDao().insertIfMissing(sessions.map { it.toEntity(synced = true) })
        val localZoneIds = db.focusZoneDao().getAll().map { it.id }.toSet()
        zones.filter { it.id !in localZoneIds }.forEach { db.focusZoneDao().upsert(it.toEntity(synced = true)) }
        val localGroupIds = db.appGroupDao().getAll().map { it.id }.toSet()
        appGroups.filter { it.id !in localGroupIds }.forEach { db.appGroupDao().upsert(it.toEntity(synced = true)) }
    }

    // clearAllTables() blocks, so it can't run on the main thread.
    override suspend fun clearAll() = withContext(Dispatchers.IO) { db.clearAllTables() }

    override suspend fun getFriends(): List<Friend> =
        db.friendDao().getAll().map { it.toDomain() }

    override suspend fun saveFriend(friend: Friend) =
        db.friendDao().upsert(friend.toEntity())

    override suspend fun deleteFriend(uid: String) =
        db.friendDao().deleteByUid(uid)

    private companion object {
        const val ZONE_SYNC_PREFS = "focus_zone_sync"
        const val KEY_ZONES_PER_ID_RESYNC_DONE = "zones_per_id_resync_done"
    }
}
