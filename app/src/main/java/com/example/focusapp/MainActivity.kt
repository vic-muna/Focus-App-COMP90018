package com.example.focusapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.focusapp.ui.navigation.FocusAppNavGraph
import com.example.focusapp.ui.theme.FocusAppTheme
import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.example.focusapp.data.notification.TimeFocusNotification

/** The app's only Activity. It shows [FocusAppNavGraph], which switches between the screens. */
class MainActivity : ComponentActivity() {

    // Android 13+ needs permission to show the focus timer notification.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    // The time slot to open, when the app was opened from a Time Focus notification.
    private val timeSlotToOpen = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timeSlotToOpen.value = intent.getStringExtra(TimeFocusNotification.EXTRA_OPEN_TIME_SLOT)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

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

    /** Called instead of onCreate when a notification is tapped while the app is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(TimeFocusNotification.EXTRA_OPEN_TIME_SLOT)?.let { timeSlotToOpen.value = it }
    }
}
