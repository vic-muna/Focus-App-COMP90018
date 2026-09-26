package com.example.focusapp.ui.common

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Reads the device's position once. Callers must already hold location
 * permission (see [rememberLocationPermissionState]).
 *
 * Tries the cached last-known position first; that's often null (fresh
 * boot, emulator, location recently off), so it then asks for a current
 * fix. [onUnavailable] runs if neither produces a position.
 */
@SuppressLint("MissingPermission")
fun fetchLastKnownLocation(
    context: Context,
    onResult: (latitude: Double, longitude: Double) -> Unit,
    onUnavailable: () -> Unit = {},
) {
    val client = LocationServices.getFusedLocationProviderClient(context)
    client.lastLocation
        .addOnSuccessListener { cached ->
            if (cached != null) {
                onResult(cached.latitude, cached.longitude)
                return@addOnSuccessListener
            }
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, CancellationTokenSource().token)
                .addOnSuccessListener { current ->
                    if (current != null) onResult(current.latitude, current.longitude) else onUnavailable()
                }
                .addOnFailureListener { onUnavailable() }
        }
        .addOnFailureListener { onUnavailable() }
}
