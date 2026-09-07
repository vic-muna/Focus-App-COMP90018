package com.example.focusapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.focusapp.data.local.dao.AppGroupDao
import com.example.focusapp.data.local.dao.FocusSessionDao
import com.example.focusapp.data.local.dao.FocusZoneDao
import com.example.focusapp.data.local.entity.AppGroupEntity
import com.example.focusapp.data.local.entity.FocusSessionEntity
import com.example.focusapp.data.local.entity.FocusZoneEntity
import com.example.focusapp.data.local.entity.PackageListConverter

/**
 * FocusAppDatabase
 * -------------------
 * The Room database for issue #41 (schema design). Bumping schema later
 * (new column/table) means bumping `version` and adding a Migration - for
 * this first version exportSchema=false is fine since there's nothing to
 * migrate from yet.
 */
@Database(
    entities = [FocusZoneEntity::class, AppGroupEntity::class, FocusSessionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(PackageListConverter::class)
abstract class FocusAppDatabase : RoomDatabase() {

    abstract fun focusZoneDao(): FocusZoneDao
    abstract fun appGroupDao(): AppGroupDao
    abstract fun focusSessionDao(): FocusSessionDao

    companion object {
        @Volatile private var INSTANCE: FocusAppDatabase? = null

        fun getInstance(context: Context): FocusAppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FocusAppDatabase::class.java,
                    "focus_app.db"
                ).build().also { INSTANCE = it }
            }
    }
}
