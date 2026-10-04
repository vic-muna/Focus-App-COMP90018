package com.example.focusapp.domain.usecase

import com.example.focusapp.domain.usecase.NoiseLevelTracker.Companion.COOLDOWN_MILLIS
import com.example.focusapp.domain.usecase.NoiseLevelTracker.Companion.READING_INTERVAL_MILLIS
import com.example.focusapp.domain.usecase.NoiseLevelTracker.Companion.WINDOW_MILLIS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NoiseLevelTrackerTest {

    private val windowReadings = (WINDOW_MILLIS / READING_INTERVAL_MILLIS).toInt()
    private val tracker = NoiseLevelTracker(thresholdDb = 55)
    private var now = 0L

    /** Feeds [count] readings of [db], one interval apart, and returns the last result. */
    private fun feed(db: Double, count: Int = 1): NoiseLevel {
        var last: NoiseLevel? = null
        repeat(count) {
            last = tracker.onReading(db, now)
            now += READING_INTERVAL_MILLIS
        }
        return last!!
    }

    @Test
    fun average_isNullUntilWindowFills() {
        assertNull(feed(40.0, count = windowReadings - 1).averageDb)
        assertEquals(40.0, feed(40.0).averageDb!!, 0.001)
    }

    @Test
    fun average_isOfTheLatestWindowOnly() {
        feed(80.0, count = windowReadings)
        assertEquals(40.0, feed(40.0, count = windowReadings).averageDb!!, 0.001)
    }

    @Test
    fun aSingleSpike_doesNotShowTheBanner() {
        feed(45.0, count = windowReadings)
        assertFalse(feed(90.0).isTooLoud)
    }

    @Test
    fun banner_showsWhenAverageGoesAboveThreshold() {
        feed(45.0, count = windowReadings)
        assertFalse(feed(55.0, count = windowReadings).isTooLoud) // equal isn't above
        assertTrue(feed(65.0, count = windowReadings).isTooLoud)
    }

    /** Feeds [db] until the average is at or below the threshold; returns that reading. */
    private fun feedUntilQuiet(db: Double): NoiseLevel {
        var result = feed(db)
        while (result.averageDb!! > 55) result = feed(db)
        return result
    }

    @Test
    fun banner_staysForCooldownAfterItGetsQuiet() {
        val cooldownReadings = (COOLDOWN_MILLIS / READING_INTERVAL_MILLIS).toInt()
        feed(70.0, count = windowReadings)

        assertTrue(feedUntilQuiet(30.0).isTooLoud)
        assertTrue(feed(30.0, count = cooldownReadings - 1).isTooLoud)
        assertFalse(feed(30.0).isTooLoud)
    }

    @Test
    fun banner_cooldownRestartsIfItGetsLoudAgain() {
        val cooldownReadings = (COOLDOWN_MILLIS / READING_INTERVAL_MILLIS).toInt()
        feed(70.0, count = windowReadings)
        feedUntilQuiet(30.0)

        // Loud again before the cooldown ends, then quiet again: the cooldown starts over.
        while (feed(90.0).averageDb!! <= 55) Unit
        assertTrue(feedUntilQuiet(30.0).isTooLoud)
        assertTrue(feed(30.0, count = cooldownReadings - 1).isTooLoud)
        assertFalse(feed(30.0).isTooLoud)
    }
}
