package com.example.focusapp.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.WireframeColors

/**
 * SettingsScreen
 * ----------------
 * The "Settings" tab. No wireframe has been provided for this screen's
 * content yet - only the bottom-nav tab itself appears in the
 * teammate-provided mockups - so this stays a plain placeholder styled to
 * match the same background color as the Apps/Map screens.
 *
 * "Manage Focus Zones" and "Manage App Groups" used to be buttons on this
 * screen, but the new design promotes both to their own top-level tabs
 * (see ui.screens.apps.AppsScreen and ui.screens.map.MapScreen), so they
 * were removed from here.
 *
 * TODO: to be implemented later - actual settings content (notifications,
 * account, language, etc.) once that's designed.
 */
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(24.dp)
    ) {
        Text(text = "Settings")
        Text(
            text = "No settings options yet - this screen is a placeholder.",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
