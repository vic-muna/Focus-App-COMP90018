package com.example.focusapp.data.sensor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority

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
 *  - GPS + Geofencing API + Wi-Fi (location zone detection)
 *  - Accelerometer + Gyroscope (shake / face-down gesture detection)
 *  - Light sensor (supplementary face-down detection signal)
 *  - AudioRecord (ambient volume detection)
 *  - UsageStatsManager (screen usage / app-category detection)
 *  - AccessibilityService (app restriction enforcement - kept separate
 *    from this class since it behaves more like a system service than a
 *    plain sensor reading)
 */
class SensorDataSource {
    private val DEFAULT_UPDATE_INTERVAL_GPS: Long = 30
    private val FAST_UPDATE_INTERVAL_GPS: Long = 5

    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000 * DEFAULT_UPDATE_INTERVAL_GPS)
        .setMinUpdateIntervalMillis(1000 * FAST_UPDATE_INTERVAL_GPS)
        .setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY)
        .build()


    /** TODO: to be implemented later - returns current lat/lng from GPS/Wi-Fi. */
    fun getCurrentLocation(): Pair<Double, Double>? {
        return null
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
