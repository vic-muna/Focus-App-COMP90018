package com.example.focusapp

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.focusapp.ui.navigation.FocusAppNavGraph
import com.google.android.gms.location.FusedLocationProviderClient
/**
 * MainActivity
 * -------------
 * The single Activity entry point for the whole app. Every screen is a
 * Composable function, and switching between them is handled entirely by
 * [FocusAppNavGraph] (Jetpack Navigation Compose) - this class does not
 * need to change when new screens are added.
 *
 * This class currently contains NO business logic. It only builds the
 * Compose UI tree. Sensors, background services, permission requests, etc.
 * will be wired up separately, inside the relevant screen/ViewModel/data
 * source files, as those features are implemented.
 */
class MainActivity : ComponentActivity() {

    // [Claude, 2026-09-21] POST_NOTIFICATIONS is a runtime (not just
    // manifest-declared) permission on API 33+ - without this request the
    // OS silently drops FocusTimerService's notification, it doesn't
    // crash or error. Fire-and-forget: does not gate or otherwise affect
    // setContent below, since the rest of the app doesn't depend on the
    // result either way.
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
            // MaterialTheme with no custom arguments = default Material3
            // colors/typography. No branding/visual design has been done
            // yet - that is intentional at this stage of the project.
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    // Root navigation graph - decides which screen is shown
                    // and owns the bottom navigation bar.
                    FocusAppNavGraph()
                }
            }
        }
    }
}
