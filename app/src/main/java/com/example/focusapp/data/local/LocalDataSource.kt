package com.example.focusapp.data.local

import android.content.Context
import com.example.focusapp.data.local.entity.toDomain
import com.example.focusapp.data.local.entity.toEntity
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone

/**
 * LocalDataSource
 * ------------------
 * Real Room-backed implementation - replaces the earlier in-memory stub,
 * so data now survives app restarts (issue #41 schema, #42 CRUD).
 * Table/column definitions live in data/local/entity/, DAOs in
 * data/local/dao/; this class just maps domain models <-> entities and
 * keeps the exact same public API FocusRepositoryImpl already depends on,
 * plus two additions needed for cloud sync (#43 offline caching / #48
 * local-to-cloud sync):
 *   getUnsyncedSessions() / markSessionSynced(id)
 *
 * Needs a Context to open the database - construct with
 * LocalDataSource(context), e.g. via AppContainer (see di/AppContainer.kt).
 */
class LocalDataSource(context: Context) {

    private val db = FocusAppDatabase.getInstance(context)

    suspend fun getFocusZones(): List<FocusZone> =
        db.focusZoneDao().getAll().map { it.toDomain() }

    suspend fun saveFocusZone(zone: FocusZone) =
        db.focusZoneDao().upsert(zone.toEntity())

    suspend fun getAppGroups(): List<AppGroup> =
        db.appGroupDao().getAll().map { it.toDomain() }

    suspend fun saveAppGroup(group: AppGroup) =
        db.appGroupDao().upsert(group.toEntity())

    suspend fun getSessionHistory(): List<FocusSession> =
        db.focusSessionDao().getAll().map { it.toDomain() }

    suspend fun saveFocusSession(session: FocusSession) =
        db.focusSessionDao().insert(session.toEntity())

    /** Rows not yet pushed to Firebase. */
    suspend fun getUnsyncedSessions(): List<FocusSession> =
        db.focusSessionDao().getUnsynced().map { it.toDomain() }

    suspend fun markSessionSynced(sessionId: String) =
        db.focusSessionDao().markSynced(sessionId)
}
