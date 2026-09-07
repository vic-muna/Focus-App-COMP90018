package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.focusapp.data.local.entity.FocusZoneEntity

@Dao
interface FocusZoneDao {
    @Query("SELECT * FROM focus_zones")
    suspend fun getAll(): List<FocusZoneEntity>

    // @Upsert (Room 2.5+) = insert new / replace existing by primary key,
    // matching the original in-memory stub's "removeAll { id matches }; add" behaviour.
    @Upsert
    suspend fun upsert(zone: FocusZoneEntity)
}
