package com.example.focusapp.domain.usecase

/**
 * Spots a shake: [SHAKE_COUNT] jolts above [THRESHOLD_G] within [WINDOW_MILLIS].
 * A jolt is the moment the phone's total acceleration rises above the threshold;
 * it has to drop back below before the next one counts, so one long push is one jolt.
 * Feed it every accelerometer reading through [onReading].
 */
class ShakeDetector {

    private val joltTimes = ArrayDeque<Long>()
    private var isAboveThreshold = false

    /** True when this reading completes a shake (the count then starts over). */
    fun onReading(gForce: Float, nowMillis: Long): Boolean {
        val wasAbove = isAboveThreshold
        isAboveThreshold = gForce > THRESHOLD_G
        if (!isAboveThreshold || wasAbove) return false

        joltTimes.addLast(nowMillis)
        while (nowMillis - joltTimes.first() > WINDOW_MILLIS) joltTimes.removeFirst()
        if (joltTimes.size < SHAKE_COUNT) return false

        joltTimes.clear()
        return true
    }

    companion object {
        const val THRESHOLD_G = 2.5f
        const val SHAKE_COUNT = 3
        const val WINDOW_MILLIS = 1_000L
    }
}
