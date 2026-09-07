package com.example.focusapp.data.sensor

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
