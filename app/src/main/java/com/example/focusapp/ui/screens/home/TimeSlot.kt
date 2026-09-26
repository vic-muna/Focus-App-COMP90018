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

private const val LAST_MINUTE_OF_DAY = 23 * 60 + 59

private fun ClockTime.toMinutes(): Int = hour * 60 + minute
private fun clockTimeOf(minutes: Int): ClockTime = ClockTime(minutes / 60, minutes % 60)

/**
 * [David Shiau, 2026-09-26] The schedule editor's "start is always before
 * end" rule (so a slot never wraps past midnight): a start picked at or
 * after the end is pulled back to 1 minute before the end. The end is only
 * touched for an already-invalid slot (end at 00:00), bumped to 00:01.
 */
fun TimeSlot.withStartClamped(candidate: ClockTime): TimeSlot {
    val endMinutes = end.toMinutes().coerceAtLeast(1)
    val startMinutes = candidate.toMinutes().coerceAtMost(endMinutes - 1)
    return copy(start = clockTimeOf(startMinutes), end = clockTimeOf(endMinutes))
}

/**
 * Counterpart of [withStartClamped]: an end picked at or before the start is
 * pushed forward to 1 minute after the start. The start is only touched for
 * an already-invalid slot (start at 23:59), pulled back to 23:58.
 */
fun TimeSlot.withEndClamped(candidate: ClockTime): TimeSlot {
    val startMinutes = start.toMinutes().coerceAtMost(LAST_MINUTE_OF_DAY - 1)
    val endMinutes = candidate.toMinutes().coerceAtLeast(startMinutes + 1)
    return copy(start = clockTimeOf(startMinutes), end = clockTimeOf(endMinutes))
}

/** A [start, end) span of epoch millis. */
data class TimeWindow(val startMillis: Long, val endMillis: Long)

/**
 * [David Shiau, 2026-09-26] This slot's window on [now]'s calendar day, or
 * null if [now]'s day isn't one of [TimeSlot.activeDays]. Also null for an
 * old overnight slot (start after end, e.g. 22:00-06:00, saved before the
 * editor enforced start-before-end) - those aren't limit-enforced until
 * re-edited.
 */
fun TimeSlot.windowOn(now: Calendar = Calendar.getInstance()): TimeWindow? {
    if (start.toMinutes() >= end.toMinutes()) return null
    val dayIndex = (now.get(Calendar.DAY_OF_WEEK) + 5) % 7
    if (DAY_KEYS[dayIndex] !in activeDays) return null

    fun millisAt(time: ClockTime): Long = (now.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, time.hour)
        set(Calendar.MINUTE, time.minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return TimeWindow(millisAt(start), millisAt(end))
}

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
