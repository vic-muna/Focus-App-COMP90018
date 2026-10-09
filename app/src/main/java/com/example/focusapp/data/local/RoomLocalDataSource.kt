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

    override suspend fun getUnsyncedZone(): FocusZone? =
        db.focusZoneDao().getUnsynced().firstOrNull()?.toDomain()

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
}
