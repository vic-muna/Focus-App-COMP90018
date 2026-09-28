package com.example.focusapp.domain.usecase

import com.example.focusapp.data.usagestats.AppWindowUsage
import com.example.focusapp.ui.screens.home.AppItem
import com.example.focusapp.ui.screens.home.BlockedAppGroup
import com.example.focusapp.ui.screens.home.ClockTime
import com.example.focusapp.ui.screens.home.DAY_KEYS
import com.example.focusapp.ui.screens.home.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class EvaluateUsageLimitUseCaseTest {

    private val useCase = EvaluateUsageLimitUseCase()

    /** Today at [hour]:[minute]. */
    private fun todayAt(hour: Int, minute: Int = 0): Calendar = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    private fun group(maxOpens: Int? = null, maxMinutes: Int? = null, days: Set<String> = DAY_KEYS.toSet()) =
        BlockedAppGroup(
            id = "g",
            name = "Study",
            apps = listOf(
                AppItem(packageName = "facebook", name = "Facebook", isBlocked = true),
                AppItem(packageName = "instagram", name = "Instagram", isBlocked = true)
            ),
            schedule = TimeSlot(activeDays = days, start = ClockTime(16, 0), end = ClockTime(18, 0)),
            maxOpensPerApp = maxOpens,
            maxMinutesPerApp = maxMinutes
        )

    private fun usage(vararg entries: Pair<String, AppWindowUsage>) =
        { _: Long, _: Long -> mapOf(*entries) }

    @Test
    fun opensUpToLimit_areAllowed_nextOneIsBlocked() {
        val allowed = useCase.execute(
            listOf(group(maxOpens = 3)),
            usage("facebook" to AppWindowUsage(3, 0, isInForeground = true)),
            todayAt(17)
        )
        assertTrue(allowed.violations.isEmpty())

        val blocked = useCase.execute(
            listOf(group(maxOpens = 3)),
            usage("facebook" to AppWindowUsage(4, 0, isInForeground = true)),
            todayAt(17)
        )
        assertEquals(listOf("facebook"), blocked.violations.map { it.packageName })
    }

    @Test
    fun eachAppHasItsOwnLimit() {
        val result = useCase.execute(
            listOf(group(maxOpens = 3)),
            usage(
                "facebook" to AppWindowUsage(3, 0, isInForeground = true),
                "instagram" to AppWindowUsage(3, 0, isInForeground = true)
            ),
            todayAt(17)
        )
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun durationLimitReached_blocks() {
        val result = useCase.execute(
            listOf(group(maxMinutes = 30)),
            usage("facebook" to AppWindowUsage(1, 30 * 60_000L, isInForeground = true)),
            todayAt(17)
        )
        assertEquals(listOf("facebook"), result.violations.map { it.packageName })
    }

    @Test
    fun durationUnderLimit_schedulesCheckWhenItRunsOut() {
        val result = useCase.execute(
            listOf(group(maxMinutes = 30)),
            usage("facebook" to AppWindowUsage(1, 29 * 60_000L + 30_000L, isInForeground = true)),
            todayAt(17)
        )
        assertTrue(result.violations.isEmpty())
        assertEquals(30_000L, result.nextCheckInMillis)
    }

    @Test
    fun overLimitButNotOnScreen_isNotBlocked() {
        val result = useCase.execute(
            listOf(group(maxOpens = 1)),
            usage("facebook" to AppWindowUsage(5, 0, isInForeground = false)),
            todayAt(17)
        )
        assertTrue(result.violations.isEmpty())
    }

    @Test
    fun outsideWindow_isNotBlocked() {
        val overLimit = usage("facebook" to AppWindowUsage(5, 99 * 60_000L, isInForeground = true))
        assertTrue(useCase.execute(listOf(group(maxOpens = 1)), overLimit, todayAt(18, 30)).violations.isEmpty())

        val beforeWindow = useCase.execute(listOf(group(maxOpens = 1)), overLimit, todayAt(15, 30))
        assertTrue(beforeWindow.violations.isEmpty())
        // Wakes up when the window opens, in case an app is already on screen then.
        assertEquals(30 * 60_000L, beforeWindow.nextCheckInMillis)
    }

    @Test
    fun notAnActiveDay_orNoLimits_doesNothing() {
        val overLimit = usage("facebook" to AppWindowUsage(5, 99 * 60_000L, isInForeground = true))
        val now = todayAt(17)
        val today = DAY_KEYS[(now.get(Calendar.DAY_OF_WEEK) + 5) % 7]

        val otherDay = useCase.execute(listOf(group(maxOpens = 1, days = DAY_KEYS.toSet() - today)), overLimit, now)
        assertTrue(otherDay.violations.isEmpty())
        assertNull(otherDay.nextCheckInMillis)

        assertTrue(useCase.execute(listOf(group()), overLimit, now).violations.isEmpty())
    }
}
