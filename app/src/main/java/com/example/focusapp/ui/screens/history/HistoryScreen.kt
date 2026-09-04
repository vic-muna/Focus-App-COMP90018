package com.example.focusapp.ui.screens.history

// NOTE: not currently reachable from anywhere - see the same note in
// ui/screens/focus/FocusModeScreen.kt and ui/navigation/Destinations.kt.

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * HistoryScreen
 * ---------------
 * Placeholder for "Focus History". Will eventually show a scrollable list
 * of past focus sessions (duration, distracting-app open count, whether it
 * was completed) pulled from the Local Data Source via [HistoryViewModel].
 *
 * @param viewModel unused for now beyond proving the wiring works; kept as
 *                  a parameter so the ViewModel is easy to hook up for real
 *                  later without changing this screen's signature.
 *
 * TODO: to be implemented later - replace the static Text below with a
 * LazyColumn of session rows once FocusRepository.getSessionHistory() is
 * implemented.
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Focus History", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "No session history yet - this screen is a placeholder.",
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}
