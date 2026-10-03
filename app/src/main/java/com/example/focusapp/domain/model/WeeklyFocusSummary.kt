package com.example.focusapp.domain.model

import kotlin.math.roundToInt

/**
 * The numbers Focus Coach sends to Gemini instead of the raw sessions (see BuildWeeklyFocusSummaryUseCase).
 * Holds no names or ids - only focus statistics.
 */
data class WeeklyFocusSummary(
    /** The last 7 days, ending now. */
    val thisWeek: WeekStats,
    /** The 7 days before that. */
    val lastWeek: WeekStats,
    /** This week's minutes per day, oldest to newest, e.g. ("Sat", 45). */
    val dailyMinutes: List<Pair<String, Long>>,
    /** This week's minutes per part of the day. */
    val minutesByTimeOfDay: Map<TimeOfDay, Long>,
    val currentStreakDays: Int,
    val streakGoalMinutes: Int,
    val todayFocusMinutes: Long,
) {
    val hasSessionsThisWeek: Boolean get() = thisWeek.sessionCount > 0

    /** The summary as plain text, ready to put in the prompt. */
    fun toPromptText(): String = buildString {
        appendLine("My focus sessions over the last 7 days (compared with the 7 days before):")
        appendLine("- Sessions: ${thisWeek.sessionCount} (before: ${lastWeek.sessionCount})")
        appendLine("- Total focus time: ${formatMinutes(thisWeek.totalMinutes)} (before: ${formatMinutes(lastWeek.totalMinutes)})")
        appendLine("- Average session: ${formatMinutes(thisWeek.averageSessionMinutes)}; longest: ${formatMinutes(thisWeek.longestSessionMinutes)}")
        appendLine(
            "- Tries to open blocked distracting apps: ${thisWeek.distractingOpens} " +
                "(${thisWeek.distractingOpensPerHour} per focused hour; before: ${lastWeek.distractingOpensPerHour} per hour)"
        )
        appendLine("- Focus per day: " + dailyMinutes.joinToString { (day, minutes) -> "$day ${formatMinutes(minutes)}" })
        appendLine("- Focus by time of day: " + TimeOfDay.entries.joinToString { "${it.label} ${formatMinutes(minutesByTimeOfDay[it] ?: 0L)}" })
        append("- Daily streak: $currentStreakDays days (a day counts at ${formatMinutes(streakGoalMinutes.toLong())} of focus); ")
        append("today so far: ${formatMinutes(todayFocusMinutes)}")
    }
}

/** Totals for one 7-day period. */
data class WeekStats(
    val sessionCount: Int = 0,
    val totalMinutes: Long = 0,
    val longestSessionMinutes: Long = 0,
    val distractingOpens: Int = 0,
) {
    val averageSessionMinutes: Long get() = if (sessionCount == 0) 0 else totalMinutes / sessionCount

    /** Rounded to one decimal place; 0 when there was no focus time. */
    val distractingOpensPerHour: Double
        get() = if (totalMinutes == 0L) 0.0 else (distractingOpens * 600.0 / totalMinutes).roundToInt() / 10.0
}

/** Parts of the day, by the hour a session starts. */
enum class TimeOfDay(val label: String) {
    MORNING("Morning (5-12)"),
    AFTERNOON("Afternoon (12-17)"),
    EVENING("Evening (17-21)"),
    NIGHT("Night (21-5)");

    companion object {
        fun ofHour(hour: Int): TimeOfDay = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..20 -> EVENING
            else -> NIGHT
        }
    }
}
