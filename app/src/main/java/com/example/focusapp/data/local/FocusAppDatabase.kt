package com.example.focusapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.focusapp.data.local.dao.AppGroupDao
import com.example.focusapp.data.local.dao.FocusSessionDao
import com.example.focusapp.data.local.dao.FocusZoneDao
import com.example.focusapp.data.local.dao.FriendDao
import com.example.focusapp.data.local.entity.AppGroupEntity
import com.example.focusapp.data.local.entity.FocusSessionEntity
import com.example.focusapp.data.local.entity.FocusZoneEntity
import com.example.focusapp.data.local.entity.FriendEntity
import com.example.focusapp.data.local.entity.PackageListConverter

/**
 * FocusAppDatabase
 * -------------------
 * The Room database for issue #41 (schema design).
 *
 * version 2: added the `friends` table (see FriendEntity) - bumped from 1
 * and paired with fallbackToDestructiveMigration(dropAllTables = true)
 * below rather than a real Migration, since this hasn't shipped anywhere yet (every install
 * is a dev/emulator build) - the trade-off is that anyone pulling this
 * change keeps their existing FocusZone/AppGroup/FocusSession data only if
 * Room can open the old file; if not, it just wipes local data on next
 * launch (no server data is affected). Switch to a real Migration once
 * this matters (e.g. once there's a release build people don't want to
 * lose local data from).
 */
@Database(
    entities = [FocusZoneEntity::class, AppGroupEntity::class, FocusSessionEntity::class, FriendEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(PackageListConverter::class)
abstract class FocusAppDatabase : RoomDatabase() {

    abstract fun focusZoneDao(): FocusZoneDao
    abstract fun appGroupDao(): AppGroupDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun friendDao(): FriendDao

    companion object {
        @Volatile private var INSTANCE: FocusAppDatabase? = null

        fun getInstance(context: Context): FocusAppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusAppDatabase::class.java,
                    "focus_app.db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build().also { INSTANCE = it }
            }
    }
}
