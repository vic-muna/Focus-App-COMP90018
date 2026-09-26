package com.example.focusapp.ui.screens.home

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeSlotClampTest {

    private val slot = TimeSlot(activeDays = setOf("Mon"), start = ClockTime(16, 0), end = ClockTime(18, 0))

    @Test
    fun startBeforeEnd_isKept() {
        val result = slot.withStartClamped(ClockTime(9, 30))
        assertEquals(ClockTime(9, 30), result.start)
        assertEquals(ClockTime(18, 0), result.end)
    }

    @Test
    fun startAtOrAfterEnd_isPulledBackToOneMinuteBeforeEnd() {
        assertEquals(ClockTime(17, 59), slot.withStartClamped(ClockTime(18, 0)).start)
        assertEquals(ClockTime(17, 59), slot.withStartClamped(ClockTime(20, 0)).start)
    }

    @Test
    fun endAtOrBeforeStart_isPushedToOneMinuteAfterStart() {
        assertEquals(ClockTime(16, 1), slot.withEndClamped(ClockTime(16, 0)).end)
        assertEquals(ClockTime(16, 1), slot.withEndClamped(ClockTime(6, 0)).end)
    }

    @Test
    fun endAfterStart_isKept() {
        val result = slot.withEndClamped(ClockTime(23, 59))
        assertEquals(ClockTime(16, 0), result.start)
        assertEquals(ClockTime(23, 59), result.end)
    }

    @Test
    fun legacyEndAtMidnight_isRepairedWhenStartChanges() {
        val legacy = slot.copy(end = ClockTime(0, 0))
        val result = legacy.withStartClamped(ClockTime(10, 0))
        assertEquals(ClockTime(0, 0), result.start)
        assertEquals(ClockTime(0, 1), result.end)
    }
}
