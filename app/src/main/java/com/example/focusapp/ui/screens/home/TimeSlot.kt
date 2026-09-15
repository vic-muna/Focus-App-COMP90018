package com.example.focusapp.ui.screens.home

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
