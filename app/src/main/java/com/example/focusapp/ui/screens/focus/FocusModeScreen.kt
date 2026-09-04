package com.example.focusapp.ui.screens.focus

// NOTE: not currently reachable from anywhere. The teammate-provided
// wireframes (Apps / Map / Settings) don't show a Focus Mode tab, so this
// screen was removed from the bottom nav in ui/navigation/NavGraph.kt.
// It's kept here, not deleted, until the team decides whether/where a
// "runtime" screen like this fits into the new navigation - see
// ui/navigation/Destinations.kt for more context.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * FocusModeScreen
 * -----------------
 * Placeholder UI for the main "Runtime mode" screen from the process-flow
 * diagram (Restriction Rule Active -> Trigger -> Location/Face-down
 * Detection -> Trigger Focus/App Restriction -> Restrict Selected Apps).
 *
 * Right now this screen only shows a status label and a "Start Focus"
 * button that updates placeholder state - no sensors, timers, or actual
 * app restrictions are wired up.
 *
 * @param viewModel Supplies UI state. Defaults to a fresh
 *                  [FocusModeViewModel] via the standard Compose
 *                  ViewModel factory (`viewModel()`), so callers don't
 *                  need to pass one in manually.
 */
@Composable
fun FocusModeScreen(viewModel: FocusModeViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Focus Mode", style = MaterialTheme.typography.headlineMedium)

        Text(
            text = "Status: ${uiState.statusLabel}",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        Button(onClick = { viewModel.onStartFocusClicked() }) {
            Text("Start Focus")
        }

        // TODO: to be implemented later:
        //  - A picker for which saved Focus Zone / time slot to use.
        //  - A "Stop Focus" action once a session is actually active.
        //  - A live countdown / current-restriction-list display.
    }
}
