package com.example.focusapp.data.sensor

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        // This is the function that wakes up in the background!
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent == null || geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Error receiving geofence event")
            return
        }

        // Check if the user entered or exited the zone
        val geofenceTransition = geofencingEvent.geofenceTransition

        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            Log.d("GeofenceReceiver", "User ENTERED the focus zone!")
            // TODO: Trigger focus mode, send notification, etc.
        } else if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Log.d("GeofenceReceiver", "User EXITED the focus zone!")
        }
    }
}