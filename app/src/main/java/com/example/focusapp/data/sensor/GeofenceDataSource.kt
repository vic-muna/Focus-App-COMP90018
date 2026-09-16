package com.example.focusapp.data.sensor

import android.app.PendingIntent
import java.util.UUID //this in temporary.
import android.content.Context
import android.content.Intent
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest

private const val TAG = "GeofenceTracker"

fun addFocusZoneGeofence(context: Context, lat: Double, lng: Double, radius: Float) {
    // 1. Initialize the client using the context you passed in
    val geofencingClient = LocationServices.getGeofencingClient(context)

    // 2. Build the Geofence object
    val newLocationId = UUID.randomUUID().toString() // TODO: Once the Focus Locations get saved in the database of the phone, the id should be adjusted to the databases primary keys
    val geofence = Geofence.Builder()
        .setRequestId(newLocationId)
        .setCircularRegion(lat, lng, radius)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .build()

    val geofencingRequest = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    // 3. Create the PendingIntent to wake up GeofenceBroadcastReceiver
    val pendingIntent = getGeofencePendingIntent(context)

    // 4. Hand the Geofence and Intent to the geofencingClient
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully registered Focus Zone: $lat, $lng, $radius, $newLocationId")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to register Focus Zone: ${exception.message}")
            }

    } else {
        Log.e(TAG, "Cannot add geofence: Missing fine location permission.")
    }
}

private fun getGeofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    return PendingIntent.getBroadcast(
        context,
        0, // Request code (0 is standard here)
        intent,
        flags
    )
}

fun removeFocusZoneGeofence(context: Context, id: String) {
    val geofencingClient = LocationServices.getGeofencingClient(context)

    // Remove the specific geofence by passing its ID in a list
    geofencingClient.removeGeofences(listOf(id))
        .addOnSuccessListener {
            Log.d(TAG, "Successfully removed Focus Zone: $id")
        }
        .addOnFailureListener { exception ->
            Log.e(TAG, "Failed to remove Focus Zone $id: ${exception.message}")
        }
}