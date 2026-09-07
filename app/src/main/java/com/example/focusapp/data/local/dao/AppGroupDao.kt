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
}
