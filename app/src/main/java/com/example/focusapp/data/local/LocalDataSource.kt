package com.example.focusapp.data.local

import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.FocusZone

/**
 * LocalDataSource
 * ------------------
 * Placeholder for offline storage (the plan calls for Room/SQLite here).
 * No real database has been set up yet - add the Room dependency plus
 * @Entity / @Dao classes when implementing this for real, and replace the
 * in-memory lists below with actual queries.
 *
 * TODO: to be implemented later - replace this in-memory stub with a real
 * Room database so data survives app restarts.
 */
class LocalDataSource {

    // Temporary in-memory storage so the app compiles and runs before a
    // real database exists. Data here is lost when the app process dies.
    private val cachedZones = mutableListOf<FocusZone>()
    private val cachedGroups = mutableListOf<AppGroup>()
    private val cachedSessions = mutableListOf<FocusSession>()

    /** TODO: to be implemented later - replace with a real Room query. */
    suspend fun getFocusZones(): List<FocusZone> = cachedZones

    /** TODO: to be implemented later - replace with a real Room insert/update. */
    suspend fun saveFocusZone(zone: FocusZone) {
        cachedZones.removeAll { it.id == zone.id }
        cachedZones.add(zone)
    }

    /** TODO: to be implemented later - replace with a real Room query. */
    suspend fun getAppGroups(): List<AppGroup> = cachedGroups

    /** TODO: to be implemented later - replace with a real Room insert/update. */
    suspend fun saveAppGroup(group: AppGroup) {
        cachedGroups.removeAll { it.id == group.id }
        cachedGroups.add(group)
    }

    /** TODO: to be implemented later - replace with a real Room query. */
    suspend fun getSessionHistory(): List<FocusSession> = cachedSessions

    /** TODO: to be implemented later - replace with a real Room insert. */
    suspend fun saveFocusSession(session: FocusSession) {
        cachedSessions.add(session)
    }
}
