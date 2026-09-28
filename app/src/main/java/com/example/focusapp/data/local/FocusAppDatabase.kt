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
 * The Room database (tables on the phone).
 * When the version goes up, old local data is wiped (fallbackToDestructiveMigration).
 */
@Database(
    entities = [FocusZoneEntity::class, AppGroupEntity::class, FocusSessionEntity::class, FriendEntity::class],
    version = 4,
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
