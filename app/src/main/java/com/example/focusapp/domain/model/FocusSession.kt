package com.example.focusapp.domain.model

/**
 * FocusSession
 * --------------
 * A single completed (or in-progress) focus period. Used for both the
 * History screen and the reward/streak calculation.
 *
 * TODO: to be implemented later - populate real values when a session
 * starts/stops, and persist it via FocusRepository.
 */
data class FocusSession(
    val id: String,
    val startTimeMillis: Long,
    val endTimeMillis: Long? = null,
    val distractingAppOpenCount: Int = 0,
    val wasCompletedSuccessfully: Boolean = false
)
