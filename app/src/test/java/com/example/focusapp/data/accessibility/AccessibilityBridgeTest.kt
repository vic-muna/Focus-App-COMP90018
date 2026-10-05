package com.example.focusapp.data.accessibility

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AccessibilityBridgeTest {

    @Before
    fun setUp() {
        AccessibilityBridge.clearBlocks(BlockSource.SESSION)
        AccessibilityBridge.clearBlocks(BlockSource.LOCATION)
        AccessibilityBridge.clearBlocks(BlockSource.WIFI)
    }

    @Test
    fun locationBlocks_takePrecedenceAndIdentifyLocationBlockedApps() {
        assertFalse(AccessibilityBridge.isLocationBlocked("com.zhiliaoapp.musically"))

        AccessibilityBridge.setBlocks(
            BlockSource.LOCATION,
            listOf("com.zhiliaoapp.musically"),
            "Blocked while inside 'Library Zone'"
        )

        assertTrue(AccessibilityBridge.isLocationBlocked("com.zhiliaoapp.musically"))
        assertEquals("Blocked while inside 'Library Zone'", AccessibilityBridge.getReasonFor("com.zhiliaoapp.musically"))

        AccessibilityBridge.clearBlocks(BlockSource.LOCATION)
        assertFalse(AccessibilityBridge.isLocationBlocked("com.zhiliaoapp.musically"))
    }
}
