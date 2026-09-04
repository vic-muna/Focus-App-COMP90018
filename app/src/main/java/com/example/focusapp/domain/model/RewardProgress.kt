package com.example.focusapp.domain.model

/**
 * RewardProgress
 * -----------------
 * Snapshot of the user's gamification state (streak, points). The exact
 * visual metaphor (growing tree / progress ring / etc.) is still an open
 * design decision - see plan-review notes - and does not affect this data
 * model either way.
 *
 * TODO: to be implemented later - real values are produced by
 * CalculateFocusRewardUseCase.
 */
data class RewardProgress(
    val currentStreakDays: Int = 0,
    val totalPoints: Int = 0
)
