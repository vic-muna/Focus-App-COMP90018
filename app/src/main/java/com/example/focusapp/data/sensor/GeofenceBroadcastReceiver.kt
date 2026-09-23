package com.example.focusapp.data.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.LocationServices
import android.os.Build
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.GeofencingRequest
import com.example.focusapp.data.local.LocalDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // 1. Intercept the reboot event
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON" ||
            intent.action == "com.example.focusapp.TEST_BOOT") { // NEW

            Log.d("GeofenceReceiver", "Device booted! Restoring Focus Zones...")
            restoreGeofences(context)
            return
        }

        // 2. Handle the standard geofence transitions
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error receiving geofence event")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "User ENTERED the focus zone!")
            // TODO: Trigger focus mode, send notification, etc.
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "User EXITED the focus zone!")
        }
    }

    fun restoreGeofences(context: Context) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        val localDataSource = LocalDataSource(context)

        CoroutineScope(Dispatchers.IO).launch {
            val savedZones = localDataSource.getFocusZones()

            if (savedZones.isEmpty()) {
                Log.d("GeofenceTracker", "Boot restore: No saved zones to restore.")
                return@launch
            }

            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasFine || !hasBackground) {
                Log.e("GeofenceTracker", "Boot restore: Missing required location permissions.")
                return@launch
            }

            val geofenceList = savedZones.map { zone ->
                Geofence.Builder()
                    .setRequestId(zone.id)
                    .setCircularRegion(zone.latitude, zone.longitude, zone.radiusMeters)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .build()
            }

            val geofencingRequest = GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofences(geofenceList)
                .build()

            val pendingIntent = getGeofencePendingIntent(context)

            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                .addOnSuccessListener {
                    Log.d("GeofenceTracker", "Successfully restored ${savedZones.size} Focus Zones on boot.")
                }
                .addOnFailureListener { exception ->
                    Log.e("GeofenceTracker", "Failed to restore Focus Zones on boot: ${exception.message}")
                }
        }
    }
}