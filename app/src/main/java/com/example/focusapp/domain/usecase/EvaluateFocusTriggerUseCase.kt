package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.domain.model.containsLocation
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.isActiveNow
import java.util.Calendar

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
 * EvaluateFocusTriggerUseCase
 * ------------------------------
 * Domain-layer rule: decides whether Focus Mode should automatically
 * activate right now, based on a Blocked-App-Group's schedule and/or a
 * saved FocusZone - matches the "Trigger (Work Independently)" node in
 * the Runtime-mode part of the process-flow diagram.
 *
 * This is called from a Compose LaunchedEffect while Home is on screen
 * (UI-simulated triggering - see HomeScreenWithSheet.kt), but every input
 * here is plain data (no Context, no Compose types), so a future real
 * background trigger (a WorkManager periodic worker for schedule checks,
 * or a Geofencing API callback for location checks) can call this exact
 * same function without any change to this class.
 */
// [HANDOFF -> Victor Munacoha | README task: "Geofencing API + Wi-Fi", "GPS ... sensor integration"]
// Once SensorDataSource.getCurrentLocation() (and a real Geofencing
// callback) exist, call this execute() from that real location update
// instead of only from Home's simulated LaunchedEffect polling.
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
    ): FocusTriggerResult {
        groups.firstOrNull { it.schedule.isActiveNow(now) }?.let {
            return FocusTriggerResult.ScheduleMatch(it.id, it.name)
        }
        currentLatLng?.let { (lat, lng) ->
            currentZones.firstOrNull { it.containsLocation(lat, lng) }?.let {
                return FocusTriggerResult.LocationMatch(it.id, it.name)
            }
        }
        currentWifiSsid?.takeIf { it in taggedWifiSsids }?.let {
            return FocusTriggerResult.WifiMatch(it)
        }
        return FocusTriggerResult.NoTrigger
    }
}
