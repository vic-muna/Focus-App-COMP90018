package com.example.focusapp.ui.screens.rewards

import androidx.lifecycle.ViewModel

/**
 * RewardsViewModel
 * -------------------
 * Will eventually expose the user's current RewardProgress (streak,
 * points), computed via CalculateFocusRewardUseCase (see
 * domain/usecase/CalculateFocusRewardUseCase.kt), for [RewardsScreen] to
 * display.
 *
 * NOTE: the team has not finalized the exact reward *formula* yet (see
 * the plan-review notes on streak/completion-based rewards vs.
 * leniency-based-on-distractibility) - resolve that before implementing
 * this for real.
 *
 * TODO: to be implemented later - intentionally empty for now.
 */
class RewardsViewModel : ViewModel()
