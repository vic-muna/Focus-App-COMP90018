package com.example.focusapp.data.accessibility

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What asked for an app to be blocked. Each one keeps its own list in [AccessibilityBridge]. */
enum class BlockSource { SESSION, LOCATION, WIFI }

/**
 * Shared memory between the screens and [FocusAccessibilityService].
 *
 * Android creates the service by itself, so the screens have no reference to it.
 * Instead, both sides read and write this single object:
 *  - each [BlockSource] says which apps it blocks ([setBlocks] / [clearBlocks])
 *  - the service says whether it is turned on ([isServiceConnected])
 *
 * The sources never touch each other's list: e.g. ending a focus session
 * doesn't unblock the apps of the Wi-Fi the phone is still on.
 */
object AccessibilityBridge {

    private val _isServiceConnected = MutableStateFlow(false)

    /** True while the user has App Blocking turned on in Android's Accessibility settings. */
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    // Per source: blocked package name -> why (shown on the blocked screen).
    private val blocks = mutableMapOf<BlockSource, Map<String, String>>()

    private val _restrictedPackages = MutableStateFlow<Set<String>>(emptySet())

    /** Every app blocked right now, by any source. */
    val restrictedPackages: StateFlow<Set<String>> = _restrictedPackages.asStateFlow()

    /** Replaces [source]'s blocked apps with [packageToReason] (package name -> why). */
    @Synchronized
    fun setBlocks(source: BlockSource, packageToReason: Map<String, String>) {
        val cleaned = packageToReason
            .mapKeys { it.key.trim() }
            .filterKeys { it.isNotEmpty() }
        if (cleaned.isEmpty()) blocks.remove(source) else blocks[source] = cleaned
        _restrictedPackages.value = blocks.values.flatMap { it.keys }.toSet()
    }

    /** Blocks [packageNames] for [source], all with the same [reason] (null = the default message). */
    fun setBlocks(source: BlockSource, packageNames: Collection<String>, reason: String? = null) {
        val message = reason ?: "This app is in your Focus restricted list right now."
        setBlocks(source, packageNames.associateWith { message })
    }

    /** [source] blocks nothing any more; the other sources' apps stay blocked. */
    fun clearBlocks(source: BlockSource) = setBlocks(source, emptyMap())

    /** Why [packageName] is blocked - the first source in [BlockSource] order that blocks it. */
    @Synchronized
    fun getReasonFor(packageName: String): String? =
        BlockSource.entries.firstNotNullOfOrNull { blocks[it]?.get(packageName) }

    /** Whether [packageName] is currently blocked specifically by location (GPS focus zone). */
    @Synchronized
    fun isLocationBlocked(packageName: String): Boolean =
        blocks[BlockSource.LOCATION]?.containsKey(packageName) == true

    // --- Distracting app opens: how often the user tried to open a blocked app during a focus session. ---

    // null = no focus session running, so nothing is counted.
    private var blockedOpenCount: Int? = null
    private var lastCountedPackage: String? = null
    private var lastCountedAtMillis = 0L

    /** Called when a focus session starts: counting starts from 0. */
    @Synchronized
    fun startCountingBlockedOpens() {
        blockedOpenCount = 0
        lastCountedPackage = null
    }

    /** The opens counted in the running session so far (0 if none is running). */
    @Synchronized
    fun blockedOpensSoFar(): Int = blockedOpenCount ?: 0

    /** Called when a focus session ends: counting stops. */
    @Synchronized
    fun stopCountingBlockedOpens() {
        blockedOpenCount = null
    }

    /**
     * The user opened blocked [packageName] (any source's block counts).
     * One open can send Android's "app on screen" event several times, so the
     * same app again within [SAME_OPEN_WINDOW_MILLIS] isn't counted twice.
     */
    @Synchronized
    fun recordBlockedOpen(packageName: String, nowMillis: Long = System.currentTimeMillis()) {
        val count = blockedOpenCount ?: return
        if (packageName == lastCountedPackage && nowMillis - lastCountedAtMillis < SAME_OPEN_WINDOW_MILLIS) return
        blockedOpenCount = count + 1
        lastCountedPackage = packageName
        lastCountedAtMillis = nowMillis
    }

    private const val SAME_OPEN_WINDOW_MILLIS = 2_000L
}
