package com.example.focusapp.data.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Shared memory between the screens and [FocusAccessibilityService].
 *
 * Android creates the service by itself, so the screens have no reference to it.
 * Instead, both sides read and write this single object:
 *  - the screens say which apps to block ([setRestrictedPackages])
 *  - the service says whether it is turned on ([isServiceConnected])
 */
object AccessibilityBridge {

    private val _isServiceConnected = MutableStateFlow(false)

    /** True while the user has App Blocking turned on in Android's Accessibility settings. */
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    private val _restrictedPackages = MutableStateFlow<Set<String>>(emptySet())

    /** The apps blocked by the current focus session. */
    val restrictedPackages: StateFlow<Set<String>> = _restrictedPackages.asStateFlow()

    /** Why they are blocked (shown on the blocked screen), or null for the default message. */
    @Volatile
    var restrictionReason: String? = null
        private set

    /** Called when a focus session starts: block exactly these apps. */
    fun setRestrictedPackages(packageNames: Collection<String>, reason: String? = null) {
        restrictionReason = reason
        _restrictedPackages.value = packageNames.map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    /** Called when a focus session ends: nothing stays blocked. */
    fun clearRestrictedPackages() {
        restrictionReason = null
        _restrictedPackages.value = emptySet()
    }
}
