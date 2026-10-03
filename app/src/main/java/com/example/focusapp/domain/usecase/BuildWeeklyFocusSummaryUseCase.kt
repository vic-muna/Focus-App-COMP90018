package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.model.TimeOfDay
import com.example.focusapp.domain.model.WeekStats
import com.example.focusapp.domain.model.WeeklyFocusSummary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Turns the saved focus sessions into the [WeeklyFocusSummary] Focus Coach sends to Gemini.
 * Weeks are rolling, like the dashboard: "this week" is the last 7 days ending now, "last week"
 * the 7 days before. A session belongs to the day and time of day it started in (also like the
 * dashboard's chart). Streak numbers come from [CalculateFocusRewardUseCase]'s [RewardProgress].
 */
class BuildWeeklyFocusSummaryUseCase {

    fun execute(
        sessions: List<FocusSession>,
        rewards: RewardProgress,
        now: Calendar = Calendar.getInstance()
    ): WeeklyFocusSummary {
        val nowMillis = now.timeInMillis
        val weekMillis = TimeUnit.DAYS.toMillis(7)
        val thisWeek = sessions.filter { it.startTimeMillis in (nowMillis - weekMillis)..nowMillis }
        val lastWeek = sessions.filter { it.startTimeMillis in (nowMillis - 2 * weekMillis) until (nowMillis - weekMillis) }

        return WeeklyFocusSummary(
            thisWeek = statsOf(thisWeek),
            lastWeek = statsOf(lastWeek),
            dailyMinutes = dailyMinutes(thisWeek, now),
            minutesByTimeOfDay = thisWeek
                .groupBy { TimeOfDay.ofHour(calendarAt(it.startTimeMillis).get(Calendar.HOUR_OF_DAY)) }
                .mapValues { (_, inPart) -> inPart.sumOf { minutesOf(it) } },
            currentStreakDays = rewards.currentStreakDays,
            streakGoalMinutes = rewards.streakGoalMinutes,
            todayFocusMinutes = rewards.todayFocusMinutes,
        )
    }

    private fun statsOf(sessions: List<FocusSession>) = WeekStats(
        sessionCount = sessions.size,
        totalMinutes = sessions.sumOf { minutesOf(it) },
        longestSessionMinutes = sessions.maxOfOrNull { minutesOf(it) } ?: 0L,
        distractingOpens = sessions.sumOf { it.distractingAppOpenCount },
    )

    /** The 7 calendar days ending today, oldest first - days without focus included as 0. */
    private fun dailyMinutes(sessions: List<FocusSession>, now: Calendar): List<Pair<String, Long>> {
        val dayName = SimpleDateFormat("EEE", Locale.ENGLISH)
        val day = startOfDay(now).apply { add(Calendar.DAY_OF_YEAR, -6) }
        return (0 until 7).map {
            val dayStart = day.timeInMillis
            day.add(Calendar.DAY_OF_YEAR, 1)
            val minutes = sessions.filter { it.startTimeMillis in dayStart until day.timeInMillis }.sumOf { minutesOf(it) }
            dayName.format(dayStart) to minutes
        }
    }

    /** A session's length in whole minutes; 0 if it has no end. */
    private fun minutesOf(session: FocusSession): Long {
        val end = session.endTimeMillis ?: return 0L
        return TimeUnit.MILLISECONDS.toMinutes(end - session.startTimeMillis).coerceAtLeast(0L)
    }

    private fun calendarAt(millis: Long): Calendar = Calendar.getInstance().apply { timeInMillis = millis }

    private fun startOfDay(calendar: Calendar): Calendar = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
