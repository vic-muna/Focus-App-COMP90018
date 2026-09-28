package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.containsLocation
import java.util.Calendar
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.isActiveNow

/**
 * Result of a single [EvaluateFocusTriggerUseCase.execute] call - lets the
 * UI label *why* a session is being suggested, instead of just a Boolean.
 */
sealed class FocusTriggerResult {
    data object NoTrigger : FocusTriggerResult()
    data class ScheduleMatch(val groupId: String, val groupName: String) : FocusTriggerResult()
    data class LocationMatch(val zoneId: String, val zoneName: String) : FocusTriggerResult()
    data class WifiMatch(val ssid: String) : FocusTriggerResult()
}

/**
 * Decides whether Home should suggest focusing now: a schedule, a saved location
 * or a Wi-Fi network matches. Plain Kotlin (no Android), so it is easy to test.
 */
// TODO: call this from a real geofence callback too, not only from Home.
class EvaluateFocusTriggerUseCase {

    /**
     * @param groups the user's Blocked-App-Groups, checked for an active schedule.
     * @param currentZones the user's saved focus zones, checked against [currentLatLng].
     * @param currentLatLng the last known device location, or null if unavailable/not permitted.
     * @param taggedWifiSsids the user's saved Wi-Fi-trigger SSIDs, checked against [currentWifiSsid].
     * @param currentWifiSsid the SSID currently connected to, or null if not on Wi-Fi/not permitted/feature off.
     * @param now the clock to evaluate schedules against (defaults to the real current time).
     * @return the first matching trigger (schedule, then location, then Wi-Fi), or [FocusTriggerResult.NoTrigger].
     */
    fun execute(
        groups: List<BlockedAppGroup> = emptyList(),
        currentZones: List<FocusZone> = emptyList(),
        currentLatLng: Pair<Double, Double>? = null,
        taggedWifiSsids: List<String> = emptyList(),
        currentWifiSsid: String? = null,
        now: Calendar = Calendar.getInstance()
    ): FocusTriggerResult =
        executeAll(groups, currentZones, currentLatLng, taggedWifiSsids, currentWifiSsid, now)
            .firstOrNull() ?: FocusTriggerResult.NoTrigger

    /**
     * Every trigger that should show its own
     * banner right now - schedule, location and Wi-Fi are each independent
     * (each has its own app groups), so up to all three at once, in that
     * order. Same parameters as [execute]; empty if nothing matches.
     */
    fun executeAll(
        groups: List<BlockedAppGroup> = emptyList(),
        currentZones: List<FocusZone> = emptyList(),
        currentLatLng: Pair<Double, Double>? = null,
        taggedWifiSsids: List<String> = emptyList(),
        currentWifiSsid: String? = null,
        now: Calendar = Calendar.getInstance()
    ): List<FocusTriggerResult> {
        val scheduleMatch = groups.firstOrNull { it.enabled && it.schedule.isActiveNow(now) }
            ?.let { FocusTriggerResult.ScheduleMatch(it.id, it.name) }
        val locationMatch = currentLatLng?.let { (lat, lng) ->
            currentZones.firstOrNull { it.containsLocation(lat, lng) }
                ?.let { FocusTriggerResult.LocationMatch(it.id, it.name) }
        }
        val wifiMatch = currentWifiSsid?.takeIf { it in taggedWifiSsids }
            ?.let { FocusTriggerResult.WifiMatch(it) }
        return listOfNotNull(scheduleMatch, locationMatch, wifiMatch)
    }
}
