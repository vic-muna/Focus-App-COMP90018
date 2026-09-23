package com.example.focusapp.domain.model

import android.location.Location

/**
 * FocusZone
 * -----------
 * A user-defined location where Focus Mode should automatically activate
 * (e.g. "Library"). Corresponds to the "Add Location" / "Set Radius" /
 * "Location Match" nodes in the process-flow diagram.
 *
 * TODO: to be implemented later - persistence via LocalDataSource, and
 * validation (radius bounds, duplicate-name checks, etc.).
 */
data class FocusZone(
    val id: String,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Float
)

/** True if (latitude, longitude) falls within this zone's radius. */
fun FocusZone.containsLocation(latitude: Double, longitude: Double): Boolean {
    val results = FloatArray(1)
    Location.distanceBetween(latitude, longitude, this.latitude, this.longitude, results)
    return results[0] <= radiusMeters
}
