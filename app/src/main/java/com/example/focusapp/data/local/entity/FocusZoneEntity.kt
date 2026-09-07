package com.example.focusapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.focusapp.domain.model.FocusZone

/** Room row mirroring [FocusZone]. Kept separate from the domain model on
 *  purpose (clean architecture) - the Domain/UI layers never see @Entity. */
@Entity(tableName = "focus_zones")
data class FocusZoneEntity(
    @PrimaryKey val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)

fun FocusZoneEntity.toDomain() = FocusZone(id, name, latitude, longitude, radiusMeters)
fun FocusZone.toEntity() = FocusZoneEntity(id, name, latitude, longitude, radiusMeters)
