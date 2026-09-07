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
}
