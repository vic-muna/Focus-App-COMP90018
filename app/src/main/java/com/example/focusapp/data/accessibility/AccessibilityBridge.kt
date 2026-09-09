package com.example.focusapp.data.accessibility

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * AccessibilityBridge
 * ----------------------
 * A same-process, in-memory "mailbox" connecting [FocusAccessibilityService]
 * (which Android creates and drives on its own - no screen ever holds a
 * reference to it) to the Compose UI (AppsScreen's test panel).
 *
 * WHY THIS EXISTS: an AccessibilityService is not an Activity or a
 * ViewModel. Android instantiates it independently of any screen, so
 * there's no direct call path in either direction:
 *   - The UI can't call methods on "the running service instance" -
 *     it doesn't have a reference to one.
 *   - The service can't call setState() on a Composable - Compose only
 *     recomposes in response to State/StateFlow changes it's watching.
 *
 * The fix used here is the standard one for same-process communication:
 * a single, app-wide `object` (Kotlin singletons are per-process) holding
 * [StateFlow]s. The service WRITES to it whenever something happens
 * on-device; the UI READS it via `collectAsState()`, which makes Compose
 * automatically redraw whenever a value changes - no manual polling.
 *
 * IMPORTANT LIMITATION: this only works because the service runs in the
 * SAME process as the rest of the app (we did not add
 * `android:process=":something"` to the <service> tag in the manifest).
 * If the service were ever moved to its own process, a plain singleton
 * would stop working, since each process gets its own separate copy of
 * every Kotlin object - you'd need a Messenger, AIDL, or a bound service
 * instead.
 */
object AccessibilityBridge {

    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    // ---------------------------------------------------------------
    // 1) Is the service currently enabled and running?
    // ---------------------------------------------------------------
    private val _isServiceConnected = MutableStateFlow(false)

    /** Read-only for the UI: true once Android has connected the service. */
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    /** Called by FocusAccessibilityService.onServiceConnected() / onDestroy(). */
    fun setServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    // ---------------------------------------------------------------
    // 2) Whichever app's window most recently came to the foreground
    // ---------------------------------------------------------------
    private val _currentForegroundApp = MutableStateFlow<String?>(null)

    /** Read-only for the UI: the last package name reported by the service. */
    val currentForegroundApp: StateFlow<String?> = _currentForegroundApp.asStateFlow()

    /**
     * Called by the service every time it sees a window-state-changed
     * event. Besides updating [currentForegroundApp], this is also where
     * per-app usage-duration tracking (section 5 below) gets its data -
     * both come from the exact same event stream, so it's handled in one
     * place rather than requiring the service to make two separate calls.
     */
    fun reportForegroundApp(packageName: String) {
        _currentForegroundApp.value = packageName
        onForegroundAppChangedForUsageTracking(packageName)
    }

    // ---------------------------------------------------------------
    // 3) Which packages the UI has asked the service to block
    // ---------------------------------------------------------------
    private val _restrictedPackages = MutableStateFlow<Set<String>>(emptySet())

    /** Read-only for the service (it only ever checks membership) and the UI (to list them). */
    val restrictedPackages: StateFlow<Set<String>> = _restrictedPackages.asStateFlow()

    /** Called by the UI's "Block" button. */
    fun addRestrictedPackage(packageName: String) {
        val trimmed = packageName.trim()
        if (trimmed.isEmpty()) return
        _restrictedPackages.value = _restrictedPackages.value + trimmed
    }

    /**
     * Called by the UI's "Select Group to Block" flow: adds every package
     * in a saved [com.example.focusapp.domain.model.AppGroup] at once, in
     * a single StateFlow update, rather than looping and calling
     * [addRestrictedPackage] once per package (which would work, but would
     * trigger a separate recomposition for each individual package).
     */
    fun addRestrictedPackages(packageNames: Collection<String>) {
        val trimmed = packageNames.map { it.trim() }.filter { it.isNotEmpty() }
        if (trimmed.isEmpty()) return
        _restrictedPackages.value = _restrictedPackages.value + trimmed
    }

    /** Called by the UI's "Remove" link next to a restricted package. */
    fun removeRestrictedPackage(packageName: String) {
        _restrictedPackages.value = _restrictedPackages.value - packageName
    }

    // ---------------------------------------------------------------
    // 4) A short rolling log of "this package got blocked at this time"
    // ---------------------------------------------------------------
    private val _blockLog = MutableStateFlow<List<String>>(emptyList())

    /** Read-only for the UI: most recent block events, oldest first. */
    val blockLog: StateFlow<List<String>> = _blockLog.asStateFlow()

    /** Called by the service every time it actually blocks something. */
    fun recordBlockEvent(packageName: String) {
        val entry = "${timeFormatter.format(Date())}  blocked  $packageName"
        // Keep only the most recent 20 entries so this can't grow forever.
        _blockLog.value = (_blockLog.value + entry).takeLast(20)
    }

    // ---------------------------------------------------------------
    // 5) Per-app usage duration, tracked since the app process started
    //    (i.e. "since opening Focus") - built entirely from the same
    //    foreground-change events used in section 2 above, so no extra
    //    permission or API (like UsageStatsManager) is needed for this.
    // ---------------------------------------------------------------

    /** Which package is the "open session" below currently timing. */
    private var sessionPackage: String? = null

    /** When [sessionPackage] most recently came to the foreground. */
    private var sessionStartMillis: Long = 0L

    /**
     * Total confirmed (i.e. already-finished-session) milliseconds spent
     * in each package. This does NOT include whatever session is
     * currently in progress - see [getUsageMillisSoFar] for that.
     */
    private val _finishedUsageMillis = MutableStateFlow<Map<String, Long>>(emptyMap())

    /** Read-only for the UI: exposed mainly so `collectAsState()` can trigger recomposition when a session finishes. */
    val finishedUsageMillis: StateFlow<Map<String, Long>> = _finishedUsageMillis.asStateFlow()

    /**
     * Called internally, from [reportForegroundApp], every time the
     * foreground app changes. This is a simple "stopwatch handoff":
     * whenever a NEW package appears, we close out the previous
     * package's stopwatch (add its elapsed time to [_finishedUsageMillis])
     * and start a fresh stopwatch for the new one. If the same package
     * fires the event again (e.g. an in-app dialog also triggers a
     * window-state-changed event), we deliberately do nothing, so the
     * stopwatch isn't reset mid-session.
     */
    private fun onForegroundAppChangedForUsageTracking(packageName: String) {
        if (packageName == sessionPackage) return // still the same app - let its stopwatch keep running

        val now = System.currentTimeMillis()
        val previousPackage = sessionPackage
        if (previousPackage != null) {
            val elapsed = now - sessionStartMillis
            _finishedUsageMillis.value = _finishedUsageMillis.value.toMutableMap().apply {
                this[previousPackage] = (this[previousPackage] ?: 0L) + elapsed
            }
        }

        sessionPackage = packageName
        sessionStartMillis = now
    }

    /**
     * Total time [packageName] has been in the foreground since this
     * process started, INCLUDING whatever session is currently in
     * progress if [packageName] is the app presently on screen - this is
     * what makes a "how long have you used Facebook" counter feel live
     * rather than only updating once you switch away from it.
     *
     * This is a plain function, not a StateFlow, because "now" keeps
     * changing every millisecond even with no new event - the caller
     * (AppsScreen's usage panel) re-invokes this on a timer instead of
     * collecting it as state. See that panel's doc comment for why.
     */
    fun getUsageMillisSoFar(packageName: String): Long {
        val finished = _finishedUsageMillis.value[packageName] ?: 0L
        val ongoing = if (packageName == sessionPackage) {
            System.currentTimeMillis() - sessionStartMillis
        } else {
            0L
        }
        return finished + ongoing
    }

    /** Every package with any recorded usage so far, finished or ongoing. */
    fun getTrackedPackages(): Set<String> {
        return _finishedUsageMillis.value.keys + setOfNotNull(sessionPackage)
    }
}
