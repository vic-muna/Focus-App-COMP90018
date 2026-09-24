package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.focusapp.data.local.entity.AppGroupEntity

@Dao
interface AppGroupDao {
    @Query("SELECT * FROM app_groups")
    suspend fun getAll(): List<AppGroupEntity>

    @Upsert
    suspend fun upsert(group: AppGroupEntity)

    // Cloud sync (mirrors FocusSessionDao's getUnsynced/markSynced).
    @Query("SELECT * FROM app_groups WHERE synced = 0")
    suspend fun getUnsynced(): List<AppGroupEntity>

    @Query("UPDATE app_groups SET synced = 1 WHERE id = :groupId")
    suspend fun markSynced(groupId: String)
}
