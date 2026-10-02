package com.example.focusapp.data.sensor

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.accessibility.BlockSource
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.local.RoomLocalDataSource
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private const val TAG = "GeofenceReceiver"

private object ActiveGeofenceState {
    val activeZoneIds = mutableSetOf<String>()
}

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == "android.intent.action.QUICKBOOT_POWERON") {
            Log.d(TAG, "Device booted! Restoring Focus Zones...")
            restoreGeofences(context)
            return
        }

        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e(TAG, "Error receiving geofence event: ${geofencingEvent?.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        val triggeringGeofences = geofencingEvent.triggeringGeofences ?: emptyList()
        val triggeredZoneIds = triggeringGeofences.map { it.requestId }.toSet()

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d(TAG, "User ENTERED focus zone(s): $triggeredZoneIds")
            ActiveGeofenceState.activeZoneIds.addAll(triggeredZoneIds)
            updateActiveGeofenceRestrictions(context)
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d(TAG, "User EXITED focus zone(s): $triggeredZoneIds")
            ActiveGeofenceState.activeZoneIds.removeAll(triggeredZoneIds)
            updateActiveGeofenceRestrictions(context)
        }
    }

    fun updateActiveGeofenceRestrictions(context: Context) {
        val activeIds = ActiveGeofenceState.activeZoneIds
        if (activeIds.isEmpty()) {
            Log.d(TAG, "No active geofence zones remaining. Clearing location restrictions.")
            AccessibilityBridge.clearBlocks(BlockSource.LOCATION)
            return
        }

        val locationGroups = BlockedAppGroupStorage.forLocationGroups(context).getGroupsWithoutIcons() ?: emptyList()
        val matchingGroups = locationGroups.filter { it.id in activeIds && it.enabled }

        val packageToReasonMap = mutableMapOf<String, String>()
        matchingGroups.forEach { group ->
            val zoneName = group.name.ifBlank { "Location Focus Zone" }
            val reason = "Blocked while inside '$zoneName'"
            group.apps.filter { it.isBlocked }.forEach { app ->
                if (app.packageName !in packageToReasonMap) {
                    packageToReasonMap[app.packageName] = reason
                }
            }
        }

        if (packageToReasonMap.isNotEmpty()) {
            Log.d(TAG, "Activating location block for active zones $activeIds. App mapping: $packageToReasonMap")
            AccessibilityBridge.setBlocks(BlockSource.LOCATION, packageToReasonMap)
        } else {
            Log.d(TAG, "Active geofence zones $activeIds have no blocked apps configured.")
            AccessibilityBridge.clearBlocks(BlockSource.LOCATION)
        }
    }

    fun restoreGeofences(context: Context) {
        val geofencingClient = LocationServices.getGeofencingClient(context)
        val localDataSource = RoomLocalDataSource(context)

        CoroutineScope(Dispatchers.IO).launch {
            val savedZones = localDataSource.getFocusZones()

            if (savedZones.isEmpty()) {
                Log.d(TAG, "Boot restore: No saved zones to restore.")
                return@launch
            }

            val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val hasBackground = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
            } else true

            if (!hasFine || !hasBackground) {
                Log.e(TAG, "Boot restore: Missing required location permissions.")
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
                    Log.d(TAG, "Successfully restored ${savedZones.size} Focus Zones on boot.")
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Failed to restore Focus Zones on boot: ${exception.message}")
                }
        }
    }

    companion object {
        fun onGeofenceRemoved(context: Context, zoneId: String) {
            ActiveGeofenceState.activeZoneIds.remove(zoneId)
            GeofenceBroadcastReceiver().updateActiveGeofenceRestrictions(context)
        }
    }
}
