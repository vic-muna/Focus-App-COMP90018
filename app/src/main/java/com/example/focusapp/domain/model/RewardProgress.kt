package com.example.focusapp.domain.model

/**
 * What the Rewards page shows - three independent parts:
 *  - today's milestones: focus time added up since midnight (resets every day)
 *  - the daily streak: days in a row that reached [streakGoalMinutes]
 *  - the best streak ever: unlocks new backgrounds (never goes down)
 */
data class RewardProgress(
    /** Focus time since midnight today. */
    val todayFocusMinutes: Long = 0,
    /** Every milestone in order, each marked reached or not for today. */
    val milestones: List<DailyMilestone> = emptyList(),
    /** Days in a row that reached the goal - today included once it's reached. */
    val currentStreakDays: Int = 0,
    /** The daily focus time a day needs to count for the streak (set in Settings). */
    val streakGoalMinutes: Int = RewardRules.DEFAULT_STREAK_GOAL_MINUTES,
    /** The longest run of days in a row that reached the goal, at any time. */
    val bestStreakDays: Int = 0,
) {
    /** True once today has reached [streakGoalMinutes]. */
    val todayCountsForStreak: Boolean get() = todayFocusMinutes >= streakGoalMinutes

    /** The first milestone not reached yet today, or null once all are. */
    val nextMilestone: DailyMilestone? get() = milestones.firstOrNull { !it.reached }
}

/** One of today's focus-time milestones, e.g. "1 hour". */
data class DailyMilestone(val minutes: Int, val reached: Boolean)
