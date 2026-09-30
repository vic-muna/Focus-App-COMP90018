package com.example.focusapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.focusapp.data.notification.TimeFocusNotification
import com.example.focusapp.data.sensor.startGPSUpdates
import com.example.focusapp.ui.navigation.FocusAppNavGraph
import com.example.focusapp.ui.theme.FocusAppTheme

private const val TAG = "MainActivity"

/** The app's only Activity. It shows [FocusAppNavGraph], which switches between the screens. */
class MainActivity : ComponentActivity() {

    // Launcher for background location (geofencing) on Android 10+ (Q+)
    private val backgroundLocationLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            Log.d(TAG, "Background location permission granted: $isGranted")
        }

    // Launcher for foreground location (Fine + Coarse) and Notifications (Android 13+)
    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineLocationGranted || coarseLocationGranted) {
                Log.d(TAG, "Location permission granted. Starting GPS updates...")
                startGPSUpdates(this)

                // On Android 10+ (Q+), request background location for geofencing after foreground location is granted
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
                ) {
                    backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                }
            } else {
                Log.w(TAG, "Location permission was denied by user.")
            }
        }

    // The time slot to open, when the app was opened from a Time Focus notification.
    private val timeSlotToOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timeSlotToOpen.value = intent.getStringExtra(TimeFocusNotification.EXTRA_OPEN_TIME_SLOT)

        requestInitialPermissions()

        setContent {
            // Gives every screen FocusTheme.colors and FocusTheme.typography.
            FocusAppTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FocusAppNavGraph(
                        timeSlotToOpen = timeSlotToOpen.value,
                        onTimeSlotOpened = { timeSlotToOpen.value = null },
                    )
                }
            }
        }
    }

    private fun requestInitialPermissions() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            // Permission already granted on startup - start GPS tracking right away!
            startGPSUpdates(this)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED
            ) {
                backgroundLocationLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
        } else {
            val permissionsToRequest = mutableListOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            )

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    /** Called instead of onCreate when a notification is tapped while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(TimeFocusNotification.EXTRA_OPEN_TIME_SLOT)?.let { timeSlotToOpen.value = it }
    }
}
