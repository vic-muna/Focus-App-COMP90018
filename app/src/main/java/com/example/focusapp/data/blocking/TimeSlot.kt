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

/** Formats [ClockTime] into "04:00 PM" / "09:30 AM". */
fun formatClockTime(time: ClockTime): String {
    val (formattedTime, suffix) = time.formatted()
    return "$formattedTime $suffix"
}

/** A single time range within a schedule from [start] to [end]. */
data class TimeRange(
    val start: ClockTime,
    val end: ClockTime
)

/** Draft representing a start/end time range in minutes after midnight for UI editing. */
data class TimeRangeDraft(
    val startMinutes: Int,
    val endMinutes: Int
)

/** A weekly schedule: which days, and one or more time frames ([timeRanges]). */
data class TimeSlot(
    val activeDays: Set<String>,
    val timeRanges: List<TimeRange>
) {
    val start: ClockTime get() = timeRanges.firstOrNull()?.start ?: ClockTime(16, 0)
    val end: ClockTime get() = timeRanges.firstOrNull()?.end ?: ClockTime(18, 0)

    constructor(
        activeDays: Set<String>,
        start: ClockTime,
        end: ClockTime
    ) : this(activeDays, listOf(TimeRange(start, end)))
}

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
 * Calculates a window for [range] on [now], or null if not active today.
 */
fun TimeRange.windowOn(now: Calendar = Calendar.getInstance(), activeDays: Set<String>): TimeWindow? {
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
 * Today's window for this slot, or null if the slot isn't active today.
 */
fun TimeSlot.windowOn(now: Calendar = Calendar.getInstance()): TimeWindow? {
    val nowMillis = now.timeInMillis
    val windows = timeRanges.mapNotNull { it.windowOn(now, activeDays) }
    return windows.find { nowMillis >= it.startMillis && nowMillis < it.endMillis }
        ?: windows.filter { it.startMillis > nowMillis }.minByOrNull { it.startMillis }
        ?: windows.firstOrNull()
}

/**
 * Whether the slot is active at this moment in any of its [timeRanges].
 */
fun TimeSlot.isActiveNow(calendar: Calendar = Calendar.getInstance()): Boolean {
    val now = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
    val today = DAY_KEYS[dayIndexOf(calendar)]
    val yesterday = DAY_KEYS[(dayIndexOf(calendar) + 6) % 7]

    return timeRanges.any { range ->
        val startMinutes = range.start.toMinutes()
        val endMinutes = range.end.toMinutes()
        when {
            startMinutes < endMinutes -> today in activeDays && now >= startMinutes && now < endMinutes
            startMinutes > endMinutes -> (today in activeDays && now >= startMinutes) ||
                (yesterday in activeDays && now < endMinutes)
            else -> false
        }
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
