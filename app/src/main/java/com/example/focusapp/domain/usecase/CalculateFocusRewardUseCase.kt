package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.DailyMilestone
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.model.RewardRules
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Turns the saved focus sessions into the Rewards page's [RewardProgress]:
 *  - Daily milestones: today's focus time against [RewardRules.DAILY_MILESTONE_MINUTES]. Only today counts,
 *    so everything is back to zero at midnight.
 *  - Daily streak: a day counts when its focus time reaches the streak goal. The streak is the
 *    number of such days in a row, ending today - or yesterday, while today hasn't reached the
 *    goal yet (today can still save the streak until midnight).
 *  - Best streak: the longest run of such days at any time. Unlike the daily streak it
 *    doesn't drop after a missed day.
 *  - Points: 1 for every [RewardRules.MINUTES_PER_POINT] minutes of focus ever, minus [spentPoints].
 * A session that runs past midnight is split between the two days.
 * Days follow the phone's time zone. Nothing is stored: everything is worked out from the
 * sessions, so changing the goal applies to past days too.
 */
class CalculateFocusRewardUseCase {

    fun execute(
        sessions: List<FocusSession>,
        streakGoalMinutes: Int,
        spentPoints: Long = 0,
        now: Calendar = Calendar.getInstance()
    ): RewardProgress {
        val minutesByDay = focusMinutesByDay(sessions, now.timeInMillis)
        val totalMinutes = minutesByDay.values.sum()

        val day = startOfDay(now)
        val todayMinutes = minutesByDay[day.timeInMillis] ?: 0L

        // Today not reached yet doesn't break the streak - start counting from yesterday.
        if (todayMinutes < streakGoalMinutes) day.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while ((minutesByDay[day.timeInMillis] ?: 0L) >= streakGoalMinutes) {
            streak++
            day.add(Calendar.DAY_OF_YEAR, -1)
        }

        return RewardProgress(
            todayFocusMinutes = todayMinutes,
            milestones = RewardRules.DAILY_MILESTONE_MINUTES.map { DailyMilestone(it, reached = todayMinutes >= it) },
            currentStreakDays = streak,
            streakGoalMinutes = streakGoalMinutes,
            bestStreakDays = bestStreakDays(minutesByDay, streakGoalMinutes),
            totalFocusMinutes = totalMinutes,
            points = (totalMinutes / RewardRules.MINUTES_PER_POINT - spentPoints).coerceAtLeast(0),
        )
    }

    /** Whole minutes of focus per day, keyed by the day's midnight. Unfinished sessions count up to [nowMillis]. */
    private fun focusMinutesByDay(sessions: List<FocusSession>, nowMillis: Long): Map<Long, Long> {
        val millisByDay = mutableMapOf<Long, Long>()
        sessions.forEach { session ->
            val end = session.endTimeMillis ?: nowMillis
            var start = session.startTimeMillis
            while (start < end) {
                val dayStart = startOfDay(start)
                val nextDay = (dayStart.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }.timeInMillis
                val pieceEnd = minOf(end, nextDay)
                millisByDay[dayStart.timeInMillis] = (millisByDay[dayStart.timeInMillis] ?: 0L) + (pieceEnd - start)
                start = pieceEnd
            }
        }
        return millisByDay.mapValues { TimeUnit.MILLISECONDS.toMinutes(it.value) }
    }

    /** The longest run of days in a row with at least [goalMinutes] of focus. */
    private fun bestStreakDays(minutesByDay: Map<Long, Long>, goalMinutes: Int): Int {
        val goalDays = minutesByDay.filterValues { it >= goalMinutes }.keys
        var best = 0
        goalDays.forEach { dayMillis ->
            // Count each run once, from its first day.
            val dayBefore = startOfDay(dayMillis).apply { add(Calendar.DAY_OF_YEAR, -1) }
            if (dayBefore.timeInMillis in goalDays) return@forEach

            var length = 0
            val day = startOfDay(dayMillis)
            while (day.timeInMillis in goalDays) {
                length++
                day.add(Calendar.DAY_OF_YEAR, 1)
            }
            best = maxOf(best, length)
        }
        return best
    }

    private fun startOfDay(millis: Long): Calendar =
        startOfDay(Calendar.getInstance().apply { timeInMillis = millis })

    private fun startOfDay(calendar: Calendar): Calendar = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}
