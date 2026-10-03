package com.example.focusapp.data.preferences

import android.content.Context
import com.example.focusapp.domain.model.RewardProgress

/**
 * Remembers the daily focus time a day needs to count for the streak (changed in Settings).
 */
class RewardSettingsStorage(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getStreakGoalMinutes(): Int =
        prefs.getInt(KEY_STREAK_GOAL_MINUTES, RewardProgress.DEFAULT_STREAK_GOAL_MINUTES)

    fun saveStreakGoalMinutes(minutes: Int) {
        prefs.edit().putInt(KEY_STREAK_GOAL_MINUTES, minutes).apply()
    }

    companion object {
        /** The goal goes up and down in these steps, between [MIN_GOAL_MINUTES] and [MAX_GOAL_MINUTES]. */
        const val GOAL_STEP_MINUTES = 30
        const val MIN_GOAL_MINUTES = 30
        const val MAX_GOAL_MINUTES = 8 * 60

        private const val PREFS_NAME = "focus_rewards"
        private const val KEY_STREAK_GOAL_MINUTES = "streak_goal_minutes"
    }
}
