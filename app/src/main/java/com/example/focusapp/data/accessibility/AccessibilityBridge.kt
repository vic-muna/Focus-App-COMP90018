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

    private val packageReasons = mutableMapOf<String, String>()

    /** Why they are blocked (shown on the blocked screen), or null for the default message. */
    @Volatile
    var restrictionReason: String? = null
        private set

    /** Maps each blocked package name to its specific blocking reason. */
    @Synchronized
    fun setRestrictedPackages(packageToReasonMap: Map<String, String>) {
        packageReasons.clear()
        packageToReasonMap.forEach { (pkg, reason) ->
            if (pkg.isNotBlank()) {
                packageReasons[pkg.trim()] = reason
            }
        }
        restrictionReason = packageToReasonMap.values.firstOrNull()
        _restrictedPackages.value = packageReasons.keys.toSet()
    }

    /** Called when a focus session starts: block exactly these apps. */
    @Synchronized
    fun setRestrictedPackages(packageNames: Collection<String>, reason: String? = null) {
        val defaultReason = reason ?: "This app is in your Focus restricted list right now."
        val map = packageNames.map { it.trim() }.filter { it.isNotEmpty() }.associateWith { defaultReason }
        setRestrictedPackages(map)
    }

    /** Gets the specific blocking reason for a package, falling back to [restrictionReason]. */
    @Synchronized
    fun getReasonFor(packageName: String): String? {
        return packageReasons[packageName] ?: restrictionReason
    }

    /** Called when a focus session ends: nothing stays blocked. */
    @Synchronized
    fun clearRestrictedPackages() {
        restrictionReason = null
        packageReasons.clear()
        _restrictedPackages.value = emptySet()
    }
}
