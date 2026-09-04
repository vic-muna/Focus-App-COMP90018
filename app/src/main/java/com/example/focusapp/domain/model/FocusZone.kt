package com.example.focusapp.domain.model

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
