package com.example.focusapp.data.blocking

import java.util.Calendar

/** Weekday keys, Monday first. Schedules store their days with these keys. */
val DAY_KEYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

/** A time on a 24-hour clock (hour 0-23, minute 0-59). */
data class ClockTime(val hour: Int, val minute: Int) {
    /** For example ("07:20", "AM"). */
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

/** A weekly schedule: which days, and from [start] to [end]. */
data class TimeSlot(
    val activeDays: Set<String>,
    val start: ClockTime,
    val end: ClockTime
)

/** The schedule a new group starts with (Location and Wi-Fi groups don't use it). */
fun defaultTimeSlot(): TimeSlot = TimeSlot(
    activeDays = setOf("Mon", "Tue", "Thu", "Sat", "Sun"),
    start = ClockTime(16, 0),
    end = ClockTime(18, 0)
)

/** A time range in epoch milliseconds, from [startMillis] up to (not including) [endMillis]. */
data class TimeWindow(val startMillis: Long, val endMillis: Long)

private fun ClockTime.toMinutes(): Int = hour * 60 + minute

/** Index into [DAY_KEYS] for a calendar's day (Calendar counts Sunday as 1). */
private fun dayIndexOf(calendar: Calendar): Int = (calendar.get(Calendar.DAY_OF_WEEK) + 5) % 7

/**
 * Today's window for this slot, or null if the slot isn't active today.
 * Also null for an overnight slot (start after end), which limits skip.
 */
fun TimeSlot.windowOn(now: Calendar = Calendar.getInstance()): TimeWindow? {
    if (start.toMinutes() >= end.toMinutes()) return null
    if (DAY_KEYS[dayIndexOf(now)] !in activeDays) return null

    fun millisAt(time: ClockTime): Long {
        val calendar = now.clone() as Calendar
        calendar.set(Calendar.HOUR_OF_DAY, time.hour)
        calendar.set(Calendar.MINUTE, time.minute)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    return TimeWindow(millisAt(start), millisAt(end))
}

/**
 * Whether the slot is active at this moment.
 * An overnight slot (e.g. 22:00-06:00) also counts after midnight if it started yesterday.
 */
fun TimeSlot.isActiveNow(calendar: Calendar = Calendar.getInstance()): Boolean {
    val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val today = DAY_KEYS[dayIndexOf(calendar)]
    val yesterday = DAY_KEYS[(dayIndexOf(calendar) + 6) % 7]
    val startMinutes = start.toMinutes()
    val endMinutes = end.toMinutes()
    return when {
        startMinutes < endMinutes -> today in activeDays && now >= startMinutes && now < endMinutes
        startMinutes > endMinutes -> (today in activeDays && now >= startMinutes) ||
            (yesterday in activeDays && now < endMinutes)
        else -> false
    }
}

/** "1 time" / "3 times". */
fun formatOpenTimes(count: Int): String = if (count == 1) "1 time" else "$count times"

/** "45 min" / "1 h" / "1 h 30 min". */
fun formatLimitMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}
