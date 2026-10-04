package com.example.focusapp.domain.model

/**
 * Every number that decides how rewards work, in one place.
 * Change the game rules here - the Rewards page, Settings, the theme picker
 * and the "unlocked" dialog all read from this file.
 */
object RewardRules {

    // --- Daily milestones (Rewards page, back to zero at midnight) ---

    /** Today's focus time milestones: 5 min, 30 min, 1 h, 5 h, 10 h. */
    val DAILY_MILESTONE_MINUTES = listOf(5, 30, 60, 300, 600)

    // --- Daily streak (a day counts once its focus time reaches the daily goal) ---

    /** The daily goal before the user changes it in Settings. */
    const val DEFAULT_STREAK_GOAL_MINUTES = 120

    /** Settings lets the goal move in these steps, between the smallest and largest goal. */
    const val GOAL_STEP_MINUTES = 30
    const val MIN_GOAL_MINUTES = 30
    const val MAX_GOAL_MINUTES = 8 * 60

    // --- Reward backgrounds (unlocked by the best daily streak so far) ---

    /** Days in a row reaching the daily goal to unlock the Valley background. */
    const val VALLEY_UNLOCK_STREAK_DAYS = 7

    // --- Testing shortcut ---

    /**
     * For testing and demos: tap a locked background [TEST_UNLOCK_TAPS] times in the
     * theme picker, then type this code to unlock it without the streak.
     */
    const val TEST_UNLOCK_CODE = "Unimelb_90018"
    const val TEST_UNLOCK_TAPS = 3
}
