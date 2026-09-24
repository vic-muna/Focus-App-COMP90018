package com.example.focusapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusapp.domain.model.FocusZone

/** Room row mirroring [FocusZone], plus one data-layer-only field: `synced`
 *  tracks whether this row has reached Firebase yet (mirrors
 *  FocusSessionEntity's `synced` column - see that file's doc comment for
 *  why it isn't on the domain model). */
@Entity(tableName = "focus_zones")
data class FocusZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float,
    val synced: Boolean = false
)

fun FocusZoneEntity.toDomain() = FocusZone(id, name, latitude, longitude, radiusMeters)
fun FocusZone.toEntity(synced: Boolean = false) =
    FocusZoneEntity(id, name, latitude, longitude, radiusMeters, synced)
