package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.DailyMilestone
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardProgress
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Turns the saved focus sessions into the Rewards page's [RewardProgress]:
 *  - Daily milestones: today's focus time against [DAILY_MILESTONE_MINUTES]. Only today counts,
 *    so everything is back to zero at midnight.
 *  - Daily streak: a day counts when its focus time reaches the streak goal. The streak is the
 *    number of such days in a row, ending today - or yesterday, while today hasn't reached the
 *    goal yet (today can still save the streak until midnight).
 * A session that runs past midnight is split between the two days.
 * Days follow the phone's time zone. Nothing is stored: everything is worked out from the
 * sessions, so changing the goal applies to past days too.
 */
class CalculateFocusRewardUseCase {

    fun execute(
        sessions: List<FocusSession>,
        streakGoalMinutes: Int,
        now: Calendar = Calendar.getInstance()
    ): RewardProgress {
        val minutesByDay = focusMinutesByDay(sessions, now.timeInMillis)

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
            milestones = DAILY_MILESTONE_MINUTES.map { DailyMilestone(it, reached = todayMinutes >= it) },
            currentStreakDays = streak,
            streakGoalMinutes = streakGoalMinutes,
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

    private fun startOfDay(millis: Long): Calendar =
        startOfDay(Calendar.getInstance().apply { timeInMillis = millis })

    private fun startOfDay(calendar: Calendar): Calendar = (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    companion object {
        /** 5 min, 30 min, 1 h, 5 h, 10 h of focus in one day. */
        val DAILY_MILESTONE_MINUTES = listOf(5, 30, 60, 300, 600)
    }
}
