package com.example.focusapp.domain.usecase

/** One processed reading for the Focus Mode screen. */
data class NoiseLevel(
    /** The reading as it came from the mic. */
    val rawDb: Double,
    /** Moving average over the last [NoiseLevelTracker.WINDOW_MILLIS]; null until the window fills. */
    val averageDb: Double?,
    /** Whether the "too loud" banner should be showing. */
    val isTooLoud: Boolean,
)

/**
 * Turns raw mic readings into a steadier level and decides when the "too loud"
 * banner shows. Feed it every reading through [onReading]; make a new one each
 * time the mic starts, so old readings don't carry over.
 *  - The level is the average of the last [WINDOW_MILLIS] of readings, so a
 *    cough or a dropped pen barely moves it.
 *  - The banner shows as soon as the average goes above [thresholdDb], and hides
 *    only once it has stayed at or below it for [COOLDOWN_MILLIS], so it doesn't
 *    flicker while the level hovers around the threshold.
 */
class NoiseLevelTracker(private val thresholdDb: Int) {

    private val window = ArrayDeque<Double>()
    private var isTooLoud = false
    // When the average last dropped to the threshold or below while the banner was up.
    private var quietSinceMillis: Long? = null

    fun onReading(rawDb: Double, nowMillis: Long): NoiseLevel {
        window.addLast(rawDb)
        if (window.size > WINDOW_READINGS) window.removeFirst()
        val averageDb = if (window.size == WINDOW_READINGS) window.average() else null

        if (averageDb != null && averageDb > thresholdDb) {
            isTooLoud = true
            quietSinceMillis = null
        } else if (isTooLoud) {
            val quietSince = quietSinceMillis ?: nowMillis.also { quietSinceMillis = it }
            if (nowMillis - quietSince >= COOLDOWN_MILLIS) {
                isTooLoud = false
                quietSinceMillis = null
            }
        }

        return NoiseLevel(rawDb = rawDb, averageDb = averageDb, isTooLoud = isTooLoud)
    }

    companion object {
        /** How often the mic gives a reading (matches NoiseLevelDataSource). */
        const val READING_INTERVAL_MILLIS = 500L
        const val WINDOW_MILLIS = 10_000L
        const val COOLDOWN_MILLIS = 10_000L

        private const val WINDOW_READINGS = (WINDOW_MILLIS / READING_INTERVAL_MILLIS).toInt()
    }
}
