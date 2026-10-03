package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.model.TimeOfDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class BuildWeeklyFocusSummaryUseCaseTest {

    private val useCase = BuildWeeklyFocusSummaryUseCase()

    /** "Now" for every test: today at 20:00. */
    private val now: Calendar = at(daysAgo = 0, hour = 20)

    private val rewards = RewardProgress(todayFocusMinutes = 50, currentStreakDays = 3, streakGoalMinutes = 120)

    private fun at(daysAgo: Int, hour: Int): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun session(daysAgo: Int, hour: Int, minutes: Int, opens: Int = 0): FocusSession {
        val start = at(daysAgo, hour).timeInMillis
        return FocusSession(
            id = "$daysAgo-$hour",
            startTimeMillis = start,
            endTimeMillis = start + minutes * 60_000L,
            distractingAppOpenCount = opens
        )
    }

    @Test
    fun `no sessions - empty summary`() {
        val summary = useCase.execute(emptyList(), rewards, now)

        assertFalse(summary.hasSessionsThisWeek)
        assertEquals(7, summary.dailyMinutes.size)
        assertTrue(summary.dailyMinutes.all { it.second == 0L })
    }

    @Test
    fun `sessions are split into this week and last week`() {
        val sessions = listOf(
            session(daysAgo = 0, hour = 9, minutes = 30),
            session(daysAgo = 6, hour = 9, minutes = 60),
            session(daysAgo = 8, hour = 9, minutes = 45),
            session(daysAgo = 20, hour = 9, minutes = 90), // older than two weeks - ignored
        )

        val summary = useCase.execute(sessions, rewards, now)

        assertEquals(2, summary.thisWeek.sessionCount)
        assertEquals(90L, summary.thisWeek.totalMinutes)
        assertEquals(45L, summary.thisWeek.averageSessionMinutes)
        assertEquals(60L, summary.thisWeek.longestSessionMinutes)
        assertEquals(1, summary.lastWeek.sessionCount)
        assertEquals(45L, summary.lastWeek.totalMinutes)
    }

    @Test
    fun `daily minutes run oldest to newest and end today`() {
        val sessions = listOf(session(daysAgo = 0, hour = 9, minutes = 30), session(daysAgo = 6, hour = 9, minutes = 60))

        val daily = useCase.execute(sessions, rewards, now).dailyMinutes

        assertEquals(60L, daily.first().second)
        assertEquals(30L, daily.last().second)
    }

    @Test
    fun `focus is grouped by the time of day a session starts`() {
        val sessions = listOf(
            session(daysAgo = 1, hour = 9, minutes = 30),
            session(daysAgo = 2, hour = 14, minutes = 40),
            session(daysAgo = 3, hour = 22, minutes = 50),
            session(daysAgo = 4, hour = 2, minutes = 10),
        )

        val byTime = useCase.execute(sessions, rewards, now).minutesByTimeOfDay

        assertEquals(30L, byTime[TimeOfDay.MORNING])
        assertEquals(40L, byTime[TimeOfDay.AFTERNOON])
        assertEquals(null, byTime[TimeOfDay.EVENING])
        assertEquals(60L, byTime[TimeOfDay.NIGHT])
    }

    @Test
    fun `distracting opens per focused hour`() {
        val sessions = listOf(session(daysAgo = 1, hour = 9, minutes = 90, opens = 3))

        assertEquals(2.0, useCase.execute(sessions, rewards, now).thisWeek.distractingOpensPerHour, 0.001)
    }

    @Test
    fun `prompt text has the key numbers and no ids`() {
        val sessions = listOf(session(daysAgo = 1, hour = 9, minutes = 80, opens = 2))

        val text = useCase.execute(sessions, rewards, now).toPromptText()

        assertTrue(text.contains("Sessions: 1 (before: 0)"))
        assertTrue(text.contains("Total focus time: 1h 20m"))
        assertTrue(text.contains("Daily streak: 3 days"))
        assertTrue(text.contains("today so far: 50m"))
        assertFalse(text.contains("1-9")) // the session id
    }
}
