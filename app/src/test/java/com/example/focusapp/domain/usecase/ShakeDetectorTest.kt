package com.example.focusapp.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShakeDetectorTest {

    private val detector = ShakeDetector()

    /** One jolt at [atMillis]: a reading above the threshold, then one back at rest. Returns the jolt's result. */
    private fun jolt(atMillis: Long, gForce: Float = 3f): Boolean {
        val result = detector.onReading(gForce, atMillis)
        detector.onReading(1f, atMillis + 20)
        return result
    }

    @Test
    fun threeJoltsWithinOneSecond_isAShake() {
        assertFalse(jolt(0))
        assertFalse(jolt(300))
        assertTrue(jolt(600))
    }

    @Test
    fun joltsSpreadOverMoreThanOneSecond_isNotAShake() {
        jolt(0)
        jolt(600)
        assertFalse(jolt(1_200))
    }

    @Test
    fun oldJoltsDropOut_soALaterBurstStillCounts() {
        jolt(0)
        jolt(600)
        jolt(1_200)
        assertTrue(jolt(1_500))
    }

    @Test
    fun readingsAtOrBelowThreshold_areNotJolts() {
        jolt(0, gForce = 2.5f)
        jolt(200, gForce = 2.0f)
        jolt(400, gForce = 2.5f)
        assertFalse(jolt(600, gForce = 1f))
    }

    @Test
    fun stayingAboveThreshold_isOneJolt() {
        assertFalse(detector.onReading(3f, 0))
        assertFalse(detector.onReading(3f, 20))
        assertFalse(detector.onReading(3f, 40))
        assertFalse(detector.onReading(3f, 60))
    }

    @Test
    fun countStartsOverAfterAShake() {
        jolt(0)
        jolt(100)
        assertTrue(jolt(200))
        assertFalse(jolt(300))
        assertFalse(jolt(400))
        assertTrue(jolt(500))
    }
}
