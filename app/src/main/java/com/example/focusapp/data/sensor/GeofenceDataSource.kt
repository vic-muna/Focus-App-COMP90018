package com.example.focusapp.data.sensor

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.local.RoomLocalDataSource
import com.example.focusapp.domain.model.FocusZone
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GeofenceTracker"

fun registerGeofenceForZone(context: Context, zone: FocusZone) {
    val geofencingClient = LocationServices.getGeofencingClient(context)

    val geofence = Geofence.Builder()
        .setRequestId(zone.id)
        .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
        .setExpirationDuration(Geofence.NEVER_EXPIRE)
        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
        .build()

    val geofencingRequest = GeofencingRequest.Builder()
        .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
        .addGeofence(geofence)
        .build()

    val pendingIntent = getGeofencePendingIntent(context)

    val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (hasFine || hasCoarse) {
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Successfully registered geofence for Focus Zone '${zone.name}' (${zone.id}): ${zone.latitude}, ${zone.longitude}, radius ${zone.radiusMeters}m")
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to register geofence for Focus Zone ${zone.id}: ${exception.message}")
            }
    } else {
        Log.e(TAG, "Cannot add geofence: Missing location permission.")
    }
}

fun addFocusZoneGeofence(context: Context, lat: Double, lng: Double, radius: Float) {
    val localDataSource = RoomLocalDataSource(context)
    val newZone = FocusZone(
        id = "zone_${System.currentTimeMillis()}",
        name = "New Focus Zone",
        latitude = lat,
        longitude = lng,
        radiusMeters = radius
    )

    CoroutineScope(Dispatchers.IO).launch {
        val saveSuccessful = localDataSource.addFocusZone(newZone)
        if (saveSuccessful) {
            registerGeofenceForZone(context, newZone)
        } else {
            Log.e(TAG, "Aborting OS geofence registration because local save failed.")
        }
    }
}

fun getGeofencePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
    val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    } else {
        PendingIntent.FLAG_UPDATE_CURRENT
    }

    return PendingIntent.getBroadcast(
        context,
        0,
        intent,
        flags
    )
}

/**
 * Unregisters a geofence from Android OS and clears its active app restrictions
 * WITHOUT deleting the zone from the database or SharedPreferences storage.
 */
fun unregisterGeofence(context: Context, id: String) {
    val geofencingClient = LocationServices.getGeofencingClient(context)

    geofencingClient.removeGeofences(listOf(id))
        .addOnSuccessListener {
            Log.d(TAG, "Successfully unregistered OS geofence: $id")
        }
        .addOnFailureListener { exception ->
            if (exception is ApiException) {
                when (exception.statusCode) {
                    GeofenceStatusCodes.GEOFENCE_NOT_AVAILABLE -> {
                        Log.e(TAG, "Geofence $id not found in OS. It may have already been unregistered.")
                    }
                    else -> {
                        Log.e(TAG, "API Error unregistering OS geofence $id: ${GeofenceStatusCodes.getStatusCodeString(exception.statusCode)}")
                    }
                }
            } else {
                Log.e(TAG, "Failed to unregister OS geofence $id: ${exception.message}")
            }
        }

    GeofenceBroadcastReceiver.onGeofenceRemoved(context, id)
}

/**
 * Completely deletes a geofence from OS, database, and SharedPreferences storage.
 */
fun deleteFocusZoneGeofence(context: Context, id: String) {
    unregisterGeofence(context, id)

    CoroutineScope(Dispatchers.IO).launch {
        val localDataSource = RoomLocalDataSource(context)
        localDataSource.deleteFocusZone(id)

        val locationStorage = BlockedAppGroupStorage.forLocationGroups(context)
        val existingGroups = locationStorage.getGroupsWithoutIcons()
        if (existingGroups != null) {
            val updatedGroups = existingGroups.filterNot { it.id == id }
            locationStorage.saveGroups(updatedGroups)
            Log.d(TAG, "Successfully deleted location group for zone: $id")
        }
    }
}

@Deprecated("Use unregisterGeofence or deleteFocusZoneGeofence explicitly.")
fun removeFocusZoneGeofence(context: Context, id: String) {
    deleteFocusZoneGeofence(context, id)
}
