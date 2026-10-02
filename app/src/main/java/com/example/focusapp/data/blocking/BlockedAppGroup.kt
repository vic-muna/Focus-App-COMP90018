package com.example.focusapp.data.blocking

/**
 * A named list of apps to block.
 * Used by Time Focus (with [schedule] and the daily limits),
 * Location (id = zone id) and Wi-Fi (with [wifiSsids]).
 *
 * [maxOpensPerApp] / [maxMinutesPerApp] apply to each app on its own; null = no limit.
 * [wifiSsids]: the networks a Wi-Fi entry watches - see [watchedSsids].
 */
data class BlockedAppGroup(
    val id: String,
    val name: String,
    val apps: List<AppItem>,
    val schedule: TimeSlot,
    val maxOpensPerApp: Int? = null,
    val maxMinutesPerApp: Int? = null,
    val enabled: Boolean = true,
    val wifiSsids: List<String> = emptyList()
)

/**
 * The Wi-Fi networks a Wi-Fi entry watches. Entries saved before an entry
 * could watch several networks used their id as the one network name.
 */
val BlockedAppGroup.watchedSsids: List<String>
    get() = wifiSsids.ifEmpty { listOf(id) }
