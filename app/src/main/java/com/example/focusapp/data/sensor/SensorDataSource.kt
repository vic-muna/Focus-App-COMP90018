package com.example.focusapp.data.sensor
import android.content.Context
import kotlinx.coroutines.flow.StateFlow
import android.util.Log
import com.example.focusapp.data.local.LocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * SensorDataSource
 * -------------------
 * Placeholder wrapper around all the raw Android sensor/location APIs
 * the plan calls for: GPS, Wi-Fi, accelerometer, gyroscope, light sensor,
 * microphone (AudioRecord), and app-usage stats.
 *
 * None of these are implemented yet - this class exists purely so the
 * Domain layer has a stable interface to depend on later.
 *
 * TODO: to be implemented later:
 *  - GPS + Geofencing API + Wi-Fi (location zone dete
 *  ction)
 *  - Accelerometer + Gyroscope (shake / face-down gesture detection)
 *  - Light sensor (supplementary face-down detection signal)
 *  - AudioRecord (ambient volume detection)
 *  - UsageStatsManager (screen usage / app-category detection)
 *  - AccessibilityService (app restriction enforcement - kept separate
 *    from this class since it behaves more like a system service than a
 *    plain sensor reading)
 */
// [HANDOFF -> Victor Munacoha | README task: "GPS + Geofencing + Accelerometer sensor integration"]
// This whole class is your entry point. Implement getCurrentLocation() via
// FusedLocationProviderClient + the Geofencing API, and the shake/face-down
// methods via SensorManager (accelerometer + gyroscope) listeners.
class SensorDataSource {

    // --- 1. EXPOSE THE DATA STREAM ---
    /**
     * A continuous, observable stream of the latest GPS coordinates.
     *
     * HOW IT WORKS:
     * Unlike a standard function that executes instantly and returns a single value,
     * hardware sensors operate asynchronously. A `StateFlow` acts like a radio tower.
     * It holds the latest known `Pair<Double, Double>` (starting as `null`) and
     * continuously broadcasts new coordinates every time the GPS hardware updates.
     *
     * HOW OTHER PARTS OF THE APP ACCESS IT:
     *
     * 1. From Jetpack Compose (The UI Layer)
     *    Compose can "tune in" to this flow using `collectAsState()`. Whenever a new
     *    location is broadcast, Compose automatically redraws the screen with the new data.
     *
     *    Example:
     *    val location by sensorDataSource.locationFlow.collectAsState()
     *    if (location != null) {
     *        Text("Lat: ${location.first}, Lng: ${location.second}")
     *    }
     *
     * 2. From a ViewModel (The Logic Layer)
     *    A ViewModel can `collect` the flow inside a background coroutine to run
     *    calculations, check if the user entered a focus zone, or save the path to a database.
     *
     *    Example:
     *    viewModelScope.launch {
     *        sensorDataSource.locationFlow.collect { location ->
     *            if (location != null) {
     *                checkGeofenceTriggers(location)
     *            }
     *        }
     *    }
     */
    val locationFlow: StateFlow<Pair<Double, Double>?> = currentLocationFlow

    fun startTracking(context: Context) {
        startGPSUpdates(context)
    }

    fun stopTracking(context: Context) {
        stopGPSUpdates(context)
    }

    fun setTrackingPriority(context: Context, isHigh: Boolean) {
        setGpsPriority(context, isHigh)
    }

    fun addFocusZone(context: Context, lat: Double, lng: Double, rad: Float) {
        addFocusZoneGeofence(context, lat, lng, rad)
    }

    fun removeFocusZone(context: Context, id: String) {
        removeFocusZoneGeofence(context, id)
    }

    fun logLocalFocusZones(context: Context) {
        val localDataSource = LocalDataSource(context)

        CoroutineScope(Dispatchers.IO).launch {
            val savedZones = localDataSource.getFocusZones()

            if (savedZones.isEmpty()) {
                Log.d("GeofenceTracker", "Local Storage: No Focus Zones saved.")
            } else {
                Log.d("GeofenceTracker", "--- Local Storage: ${savedZones.size} Focus Zone(s) ---")
                savedZones.forEach { zone ->
                    Log.d("GeofenceTracker", "ID: ${zone.id} | Lat: ${zone.latitude}, Lng: ${zone.longitude} | Radius: ${zone.radiusMeters}m")
                }
                Log.d("GeofenceTracker", "---------------------------------------------")
            }
        }
    }


    /** TODO: to be implemented later - true if a shake gesture was just detected. */
    fun isShakeDetected(): Boolean {
        return false
    }

    /** TODO: to be implemented later - true if the phone is currently face-down. */
    fun isFaceDown(): Boolean {
        return false
    }

    /** TODO: to be implemented later - current ambient volume in dB (via AudioRecord). */
    fun getAmbientVolumeDb(): Float? {
        return null
    }
}
