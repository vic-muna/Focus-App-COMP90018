package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class CalculateFocusRewardUseCaseTest {

    private val useCase = CalculateFocusRewardUseCase()

    /** "Now" for every test: today at 20:00. */
    private val now: Calendar = at(daysAgo = 0, hour = 20)

    /** [daysAgo] days before today, at [hour]:[minute]. */
    private fun at(daysAgo: Int, hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -daysAgo)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    /** A session starting [daysAgo] days ago at [hour]:00 and lasting [minutes]. */
    private fun session(daysAgo: Int, hour: Int, minutes: Int): FocusSession {
        val start = at(daysAgo, hour).timeInMillis
        return FocusSession(id = "$daysAgo-$hour", startTimeMillis = start, endTimeMillis = start + minutes * 60_000L)
    }

    @Test
    fun `no sessions - nothing reached and no streak`() {
        val result = useCase.execute(emptyList(), streakGoalMinutes = 120, now = now)

        assertEquals(0L, result.todayFocusMinutes)
        assertTrue(result.milestones.none { it.reached })
        assertEquals(0, result.currentStreakDays)
    }

    @Test
    fun `today's sessions add up towards the milestones`() {
        val sessions = listOf(session(0, 9, 20), session(0, 14, 45))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertEquals(65L, result.todayFocusMinutes)
        assertEquals(listOf(true, true, true, false, false), result.milestones.map { it.reached })
        assertEquals(300, result.nextMilestone?.minutes)
    }

    @Test
    fun `yesterday's focus doesn't count towards today's milestones`() {
        val result = useCase.execute(listOf(session(1, 9, 600)), streakGoalMinutes = 120, now = now)

        assertEquals(0L, result.todayFocusMinutes)
        assertTrue(result.milestones.none { it.reached })
    }

    @Test
    fun `a session over midnight is split between the two days`() {
        // Yesterday 23:00 to today 01:00.
        val result = useCase.execute(listOf(session(1, 23, 120)), streakGoalMinutes = 60, now = now)

        assertEquals(60L, result.todayFocusMinutes)
        // Yesterday got 60 minutes too, so both days reach a 60-minute goal.
        assertEquals(2, result.currentStreakDays)
    }

    @Test
    fun `streak counts consecutive days including today once reached`() {
        val sessions = listOf(session(0, 9, 120), session(1, 9, 130), session(2, 9, 125))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertTrue(result.todayCountsForStreak)
        assertEquals(3, result.currentStreakDays)
    }

    @Test
    fun `today not reached yet keeps yesterday's streak`() {
        val sessions = listOf(session(0, 9, 30), session(1, 9, 120), session(2, 9, 120))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertFalse(result.todayCountsForStreak)
        assertEquals(2, result.currentStreakDays)
    }

    @Test
    fun `a missed day breaks the streak`() {
        // Two days ago is short, so only today and yesterday count.
        val sessions = listOf(session(0, 9, 120), session(1, 9, 120), session(2, 9, 60), session(3, 9, 120))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertEquals(2, result.currentStreakDays)
    }

    @Test
    fun `several short sessions in a day add up for the streak`() {
        val sessions = listOf(session(1, 9, 60), session(1, 13, 30), session(1, 18, 30))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertEquals(1, result.currentStreakDays)
    }

    @Test
    fun `the streak follows the goal from settings`() {
        val sessions = listOf(session(0, 9, 60), session(1, 9, 60))

        assertEquals(0, useCase.execute(sessions, streakGoalMinutes = 120, now = now).currentStreakDays)
        assertEquals(2, useCase.execute(sessions, streakGoalMinutes = 60, now = now).currentStreakDays)
    }

    @Test
    fun `the best streak keeps the longest run even after it was broken`() {
        // A 3-day run a while ago, a missed day, then today and yesterday.
        val sessions = listOf(
            session(0, 9, 120), session(1, 9, 120),
            session(3, 9, 120), session(4, 9, 120), session(5, 9, 120),
        )

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertEquals(2, result.currentStreakDays)
        assertEquals(3, result.bestStreakDays)
    }

    @Test
    fun `no day reached the goal - the best streak is zero`() {
        val result = useCase.execute(listOf(session(0, 9, 30)), streakGoalMinutes = 120, now = now)

        assertEquals(0, result.bestStreakDays)
    }

    @Test
    fun `every minute of focus ever earns a point`() {
        val sessions = listOf(session(0, 9, 30), session(3, 9, 45), session(10, 9, 25))

        val result = useCase.execute(sessions, streakGoalMinutes = 120, now = now)

        assertEquals(100L, result.totalFocusMinutes)
        assertEquals(100L, result.points)
    }

    @Test
    fun `spent points come off the total but never below zero`() {
        val sessions = listOf(session(1, 9, 200))

        assertEquals(80L, useCase.execute(sessions, streakGoalMinutes = 120, spentPoints = 120, now = now).points)
        assertEquals(0L, useCase.execute(sessions, streakGoalMinutes = 120, spentPoints = 500, now = now).points)
        assertEquals(200L, useCase.execute(sessions, streakGoalMinutes = 120, spentPoints = 500, now = now).totalFocusMinutes)
    }
}
