package com.example.focusapp.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Looper
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// --- 1. CONSTANTS ---
private const val TAG = "GpsTracker"
private const val DEFAULT_UPDATE_INTERVAL_SECONDS = 30L
private const val FAST_UPDATE_INTERVAL_SECONDS = 5L

// --- 2. DEFAULT CONFIGURATION ---
private val defaultLocationRequest: LocationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    FAST_UPDATE_INTERVAL_SECONDS * 1000
)
    .setMinUpdateIntervalMillis(FAST_UPDATE_INTERVAL_SECONDS * 1000)
    .build()

// --- STATE FLOW SETUP ---
// The private Mutable flow allows us to push new values internally
private val _currentLocation = MutableStateFlow<Pair<Double, Double>?>(null)

// The public read-only flow prevents other files from accidentally overwriting the coordinates
val currentLocationFlow = _currentLocation.asStateFlow()

// --- 3. CALLBACK (THE ENGINE) ---
// This defines exactly what happens every time the GPS hardware finds a new location.
private val locationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
        for (location in locationResult.locations) {
            Log.d(TAG, "Periodic update: Lat ${location.latitude}, Lng ${location.longitude}")

            // Broadcast the new coordinates to anything that is listening!
            _currentLocation.value = Pair(location.latitude, location.longitude)
        }
    }
}
// --- 4. FUNCTIONS ---

/**
 * Starts continuous GPS tracking.
 * Note: Ensure ACCESS_FINE_LOCATION is granted in the UI before calling this.
 */
fun startGPSUpdates(context: Context) {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        Log.d(TAG, "Permission confirmed. Starting location updates...")

        val client = LocationServices.getFusedLocationProviderClient(context)

        // Remove any existing subscriptions to prevent duplicate callbacks running simultaneously
        client.removeLocationUpdates(locationCallback)

        // Start listening on the main looper thread using the default request
        client.requestLocationUpdates(defaultLocationRequest, locationCallback, Looper.getMainLooper())
    } else {
        Log.e(TAG, "Cannot start GPS: Missing ACCESS_FINE_LOCATION permission.")
    }
}

/**
 * Updates the priority and interval of an actively running GPS tracker on the fly.
 */
fun setGpsPriority(context: Context, isHigh: Boolean) {
    val priority = if (isHigh) Priority.PRIORITY_HIGH_ACCURACY else Priority.PRIORITY_BALANCED_POWER_ACCURACY
    val interval = if (isHigh) FAST_UPDATE_INTERVAL_SECONDS else DEFAULT_UPDATE_INTERVAL_SECONDS

    val newRequest = LocationRequest.Builder(priority, interval * 1000)
        .setMinUpdateIntervalMillis(interval * 1000)
        .build()

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // Passing the same 'locationCallback' seamlessly updates the active session
        client.requestLocationUpdates(newRequest, locationCallback, Looper.getMainLooper())

        Log.d(TAG, "Tracker updated. High Priority: $isHigh, Interval: ${interval}s")
    } else {
        Log.e(TAG, "Cannot update priority: Missing ACCESS_FINE_LOCATION permission.")
    }
}

/**
 * Stops the GPS tracking to save battery.
 */
fun stopGPSUpdates(context: Context) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.removeLocationUpdates(locationCallback)
    Log.d(TAG, "Continuous tracking stopped.")
}
