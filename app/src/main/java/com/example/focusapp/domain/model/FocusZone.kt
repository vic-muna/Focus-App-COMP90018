package com.example.focusapp.domain.model

import android.location.Location

/**
 * A saved location (e.g. "Library") with a radius, checked by [FocusValidation]
 * before saving, and backed up to Firebase.
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
