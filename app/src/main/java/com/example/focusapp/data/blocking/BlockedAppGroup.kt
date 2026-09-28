package com.example.focusapp.data.blocking

/**
 * A named list of apps to block.
 * Used by Time Focus (with [schedule] and the daily limits),
 * Location (id = zone id) and Wi-Fi (id = network name).
 *
 * [maxOpensPerApp] / [maxMinutesPerApp] apply to each app on its own; null = no limit.
 */
data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val maxOpensPerApp: Int? = null,
    val maxMinutesPerApp: Int? = null,
    val enabled: Boolean = true
)
