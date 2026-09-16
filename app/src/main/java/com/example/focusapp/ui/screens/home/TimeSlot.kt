package com.example.focusapp.ui.screens.home

import java.util.Calendar

/**
 * Canonical Mon-Sun keys/ordering used anywhere a day set is compared,
 * iterated, or displayed. "Wed" is the correct abbreviation - do not
 * reintroduce the old "Wen" typo.
 */
val DAY_KEYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** A point in time on a 24-hour clock (hour 0-23, minute 0-59). */
data class ClockTime(val hour: Int, val minute: Int) {
    /** Returns e.g. ("07:20", "AM") - the split-size display used on the card. */
    fun formatted(): Pair<String, String> {
        val suffix = if (hour < 12) "AM" else "PM"
        val displayHour = when {
            hour == 0 -> 12
            hour > 12 -> hour - 12
            else -> hour
        }
        return "%02d:%02d".format(displayHour, minute) to suffix
    }
}

/** One schedule: which days it's active, plus start/end clock time. */
data class TimeSlot(
    val activeDays: Set<String>,
    val start: ClockTime,
    val end: ClockTime
)

fun generateFakeTimeSlot(): TimeSlot = TimeSlot(
    activeDays = setOf("Mon", "Tue", "Thu", "Sat", "Sun"),
    start = ClockTime(16, 0),
    end = ClockTime(18, 0)
)

/**
 * Pure check: is this slot active at [hour]:[minute] on [dayKey]? Handles
 * an overnight-wrapping slot (e.g. 22:00-06:00) by also treating it as
 * active past midnight if [previousDayKey] was in [activeDays] - the slot
 * started "yesterday" and hasn't ended yet.
 */
fun TimeSlot.isActiveAt(hour: Int, minute: Int, dayKey: String, previousDayKey: String): Boolean {
    val nowMinutes = hour * 60 + minute
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute
    return when {
        startMinutes < endMinutes ->
            dayKey in activeDays && nowMinutes >= startMinutes && nowMinutes < endMinutes
        startMinutes > endMinutes ->
            (dayKey in activeDays && nowMinutes >= startMinutes) ||
                (previousDayKey in activeDays && nowMinutes < endMinutes)
        else -> false // start == end - zero-length slot, never active
    }
}

/** Convenience overload driven off a real clock, for actual "is it active now" checks. */
fun TimeSlot.isActiveNow(calendar: Calendar = Calendar.getInstance()): Boolean {
    val hour = calendar.get(Calendar.HOUR_OF_DAY)
    val minute = calendar.get(Calendar.MINUTE)
    // Calendar.DAY_OF_WEEK is Sunday=1..Saturday=7; DAY_KEYS is Mon-first (index 0..6).
    val dayIndex = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7
    return isActiveAt(hour, minute, DAY_KEYS[dayIndex], DAY_KEYS[(dayIndex + 6) % 7])
}
