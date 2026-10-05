package com.example.focusapp.domain.model

/**
 * Every number that decides how rewards work, in one place.
 * Change the game rules here - the Rewards page, Settings, the theme picker
 * and the focus music all read from this file.
 */
object RewardRules {

    // --- Daily milestones (Rewards page, back to zero at midnight) ---

    /** Today's focus time milestones: 5 min, 30 min, 1 h, 5 h, 10 h. */
    val DAILY_MILESTONE_MINUTES = listOf(5, 30, 60, 300, 600)

    // --- Daily streak (a day counts once its focus time reaches the daily goal) ---

    /** The focus time a day needs to count for the streak (used by the Focus Coach summary). */
    const val DEFAULT_STREAK_GOAL_MINUTES = 120

    // --- Points (earned by focusing, spent in the Rewards shop) ---

    /** Points earned for every whole minute of focus. */
    const val POINTS_PER_MINUTE = 1

    // TODO(rewards): every price is 1 point for testing - set the real prices before release.

    /** Price of each reward background (see BackgroundThemes). */
    const val BACKGROUND_PRICE_POINTS = 1

    /** Price of each paid focus music track (see FocusMusics). */
    const val MUSIC_PRICE_POINTS = 1
}
