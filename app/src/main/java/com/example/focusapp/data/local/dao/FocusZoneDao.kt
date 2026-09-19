package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.focusapp.data.local.entity.FocusZoneEntity

// This DAO/table itself stays general-purpose (able to hold any number of
// rows) - it's RoomLocalDataSource.saveFocusZone() that enforces the
// app's single-zone contract, by calling deleteAll() before every
// upsert() so at most one row ever exists. See that class for why.
@Dao
interface FocusZoneDao {
    @Query("SELECT * FROM focus_zones")
    suspend fun getAll(): List<FocusZoneEntity>

    // @Upsert (Room 2.5+) = insert new / replace existing by primary key,
    // matching the original in-memory stub's "removeAll { id matches }; add" behaviour.
    @Upsert
    suspend fun upsert(zone: FocusZoneEntity)

    // Used by RoomLocalDataSource.saveFocusZone() to enforce "at most one
    // saved zone" before inserting the new one.
    @Query("DELETE FROM focus_zones")
    suspend fun deleteAll()
}
