package com.example.focusapp.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.example.focusapp.data.local.entity.FocusZoneEntity

// Holds any number of zones - the app supports several (multi-geofence).
// RoomLocalDataSource.saveFocusZone() and addFocusZone() both just upsert
// by id; nothing limits the table to one row any more.
@Dao
interface FocusZoneDao {
    @Query("SELECT * FROM focus_zones")
    suspend fun getAll(): List<FocusZoneEntity>

    // @Upsert (Room 2.5+) = insert new / replace existing by primary key,
    // matching the original in-memory stub's "removeAll { id matches }; add" behaviour.
    @Upsert
    suspend fun upsert(zone: FocusZoneEntity)

    // No longer called by RoomLocalDataSource.saveFocusZone() (it stopped clearing
    // the table once several zones were allowed) - kept for FocusZoneDaoTest.
    @Query("DELETE FROM focus_zones")
    suspend fun deleteAll()

    // Cloud sync (mirrors FocusSessionDao's getUnsynced/markSynced) - can return one
    // row per unsynced zone. RoomLocalDataSource.getUnsyncedZone() only takes the
    // first, and the cloud keeps a single users/{uid}/zone node, so only one zone
    // is actually pushed (see FirebaseRemoteDataSource.pushFocusZone).
    @Query("SELECT * FROM focus_zones WHERE synced = 0")
    suspend fun getUnsynced(): List<FocusZoneEntity>

    @Query("UPDATE focus_zones SET synced = 1 WHERE id = :zoneId")
    suspend fun markSynced(zoneId: String)

    @Query("DELETE FROM focus_zones WHERE id = :zoneId")
    suspend fun deleteById(zoneId: String)
}
