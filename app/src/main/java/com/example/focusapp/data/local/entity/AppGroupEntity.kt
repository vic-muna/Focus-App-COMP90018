package com.example.focusapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.example.focusapp.domain.model.AppGroup

/** Room row mirroring [AppGroup], plus one data-layer-only field: `synced`
 *  tracks whether this row has reached Firebase yet (mirrors
 *  FocusSessionEntity's `synced` column). */
@Entity(tableName = "app_groups")
data class AppGroupEntity(
    @PrimaryKey val id: String,
    val groupName: String,
    // Room can't store List<String> natively - PackageListConverter below
    // turns it into one delimited column and back.
    val packageNames: List<String>,
    val synced: Boolean = false
)

fun AppGroupEntity.toDomain() = AppGroup(id, groupName, packageNames)
fun AppGroup.toEntity(synced: Boolean = false) = AppGroupEntity(id, groupName, packageNames, synced)

/** Registered on FocusAppDatabase via @TypeConverters(PackageListConverter::class).
 *  Assumes package names never contain "|" (they can't - it's not a legal
 *  character in an Android application ID). */
class PackageListConverter {
    @TypeConverter
    fun fromList(value: List<String>): String = value.joinToString(separator = "|")

    @TypeConverter
    fun toList(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split("|")
}
