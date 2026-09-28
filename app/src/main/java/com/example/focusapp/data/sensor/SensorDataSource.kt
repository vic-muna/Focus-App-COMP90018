package com.example.focusapp.data.sensor

import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * The phone's GPS position, for Party Mode's member status.
 * See LocationDataSource.kt for how it works.
 */
class SensorDataSource {

    /**
     * The latest GPS position as (latitude, longitude), or null before the first fix.
     * Screens can watch it with `collectAsState()`.
     */
    val locationFlow: StateFlow<Pair<Double, Double>?> = currentLocationFlow

    fun startTracking(context: Context) {
        startGPSUpdates(context)
    }
}
