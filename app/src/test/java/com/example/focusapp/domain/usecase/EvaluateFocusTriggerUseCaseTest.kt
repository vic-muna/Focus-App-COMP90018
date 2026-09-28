package com.example.focusapp.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.TimeSlot
import com.example.focusapp.data.blocking.ClockTime
import com.example.focusapp.data.blocking.DAY_KEYS

class EvaluateFocusTriggerUseCaseTest {

    private val useCase = EvaluateFocusTriggerUseCase()

    private val now = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 17)
        set(Calendar.MINUTE, 0)
    }

    private val activeGroup = BlockedAppGroup(
        id = "g",
        name = "Study",
        apps = emptyList(),
        schedule = TimeSlot(DAY_KEYS.toSet(), ClockTime(16, 0), ClockTime(18, 0))
    )

    @Test
    fun scheduleMatch_showsAlongsideOtherTrigger() {
        val result = useCase.executeAll(
            groups = listOf(activeGroup),
            taggedWifiSsids = listOf("Library"),
            currentWifiSsid = "Library",
            now = now
        )
        assertEquals(
            listOf(FocusTriggerResult.ScheduleMatch("g", "Study"), FocusTriggerResult.WifiMatch("Library")),
            result
        )
    }

    @Test
    fun execute_keepsFirstMatchBehavior() {
        val result = useCase.execute(
            groups = listOf(activeGroup),
            taggedWifiSsids = listOf("Library"),
            currentWifiSsid = "Library",
            now = now
        )
        assertEquals(FocusTriggerResult.ScheduleMatch("g", "Study"), result)
    }

    @Test
    fun nothingMatches_isEmpty() {
        assertEquals(emptyList<FocusTriggerResult>(), useCase.executeAll(now = now))
    }
}
