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

/** The app's only Activity. It shows [FocusAppNavGraph], which switches between the screens. */
class MainActivity : ComponentActivity() {

    // Android 13+ needs permission to show the focus timer notification.
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                    FocusAppNavGraph()
                }
            }
        }
    }
}
