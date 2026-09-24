package com.example.focusapp.ui.screens.rewards

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
 * RewardsScreen
 * ---------------
 * Placeholder for the gamification screen described in the plan (e.g.
 * "growing a tree" / "climbing a mountain" / progress rings - the exact
 * visual has not been decided yet, see plan-review notes).
 *
 * @param viewModel unused for now beyond proving the wiring works.
 *
 * TODO: to be implemented later - once CalculateFocusRewardUseCase and the
 * final reward-visual decision are in place, replace the static Text below
 * with the real streak/points display and its chosen visual metaphor.
 */
@Composable
fun RewardsScreen(viewModel: RewardsViewModel = viewModel()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Rewards", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Streak: 0 days   |   Points: 0",
            modifier = Modifier.padding(top = 8.dp)
        )
        Text(
            text = "(Placeholder values - reward calculation not implemented yet.)",
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
