package com.example.focusapp.ui.common

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices

/**
 * Reads the device's last known position once. Callers must already hold
 * location permission (see [rememberLocationPermissionState]); [onResult]
 * is simply not called if no position is available yet.
 */
@SuppressLint("MissingPermission")
fun fetchLastKnownLocation(
    context: Context,
    onResult: (latitude: Double, longitude: Double) -> Unit,
) {
    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            if (location != null) onResult(location.latitude, location.longitude)
        }
}
