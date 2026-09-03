package com.example.focusapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.focusapp.ui.navigation.FocusAppNavGraph

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
