package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.focusapp.data.local.entity.FocusSessionEntity

@Dao
interface FocusSessionDao {
    // Feeds the History screen once HistoryViewModel is wired up.
    @Query("SELECT * FROM focus_sessions ORDER BY startTimeMillis DESC")
    suspend fun getAll(): List<FocusSessionEntity>

    // Sessions are append-only (matches the original stub's `cachedSessions.add(session)`).
    @Insert
    suspend fun insert(session: FocusSessionEntity)

    // Used by FocusRepositoryImpl.syncPendingSessions() (#43 / #48).
    @Query("SELECT * FROM focus_sessions WHERE synced = 0")
    suspend fun getUnsynced(): List<FocusSessionEntity>

    @Query("UPDATE focus_sessions SET synced = 1 WHERE id = :sessionId")
    suspend fun markSynced(sessionId: String)

    // Time-range query for "app-level and time interval analysis" (Innovation
    // marking criterion) - e.g. Dashboard's "this week vs last week" or
    // "distribution by hour" needs this instead of getAll()'s full dump.
    // Inclusive on both ends, matching System.currentTimeMillis()-based
    // range callers naturally construct (e.g. [startOfWeek, now]).
    @Query("SELECT * FROM focus_sessions WHERE startTimeMillis BETWEEN :fromMillis AND :toMillis ORDER BY startTimeMillis DESC")
    suspend fun getBetween(fromMillis: Long, toMillis: Long): List<FocusSessionEntity>
}
