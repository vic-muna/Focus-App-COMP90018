package com.example.focusapp.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.focusapp.BlockedActivity
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
import com.example.focusapp.data.blocking.watchedSsids
import com.example.focusapp.data.wifi.WifiWatcher
import com.example.focusapp.data.usagestats.hasUsageAccessPermission
import com.example.focusapp.data.usagestats.queryAppUsageInWindow
import com.example.focusapp.domain.usecase.EvaluateUsageLimitUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.windowOn
import com.example.focusapp.data.notification.TimeFocusNotification

/**
 * The app blocker. Android runs it in the background after the user turns
 * it on in Settings > Accessibility, and calls [onAccessibilityEvent]
 * every time the user switches to another app
 * (see res/xml/accessibility_service_config.xml).
 *
 * An app is blocked (by opening [BlockedActivity] on top of it) when:
 *  1. a focus session, Location zone or Wi-Fi blocks it ([AccessibilityBridge.restrictedPackages]), or
 *  2. it went over a Time Focus daily limit ([EvaluateUsageLimitUseCase]).
 *
 * It also follows the phone's Wi-Fi ([WifiWatcher]): while the phone is on a
 * network a switched-on Wi-Fi entry watches, that entry's apps are blocked -
 * no focus session needed - and unblocked when the phone leaves it.
 */
class FocusAccessibilityService : AccessibilityService() {

    // Limit checks read the usage log, so they run in the background.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var limitCheckJob: Job? = null
    private val groupStorage by lazy { BlockedAppGroupStorage(this) }
    private val evaluateUsageLimit = EvaluateUsageLimitUseCase()

    // For the Time Focus notification: the apps with a limit, and the last one of them opened.
    @Volatile private var limitedPackages: Set<String> = emptySet()
    @Volatile private var lastOpenedPackage: String? = null

    // The app on screen now, so joining a Wi-Fi can block it straight away.
    @Volatile private var foregroundPackage: String? = null
    private val wifiGroupStorage by lazy { BlockedAppGroupStorage.forWifiNetworks(this) }
    private var wifiWatcher: WifiWatcher? = null

    /** The user turned the service on. */
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.setServiceConnected(true)
        scheduleLimitCheck(0L) // An app may already be open and over its limit.
        wifiWatcher = WifiWatcher(this) { onWifiChanged() }.also { it.start() }
    }

    /** The user switched apps; [event]'s packageName is the app now on screen. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return
        foregroundPackage = packageName
        if (packageName in limitedPackages) lastOpenedPackage = packageName

        // Picks up Wi-Fi entries switched on/off or edited since the last app switch.
        wifiWatcher?.retryIfUnknown()
        updateWifiBlocks()

        if (packageName in AccessibilityBridge.restrictedPackages.value) {
            AccessibilityBridge.recordBlockedOpen(packageName) // Only counted during a focus session.
            launchBlockedScreen(packageName, AccessibilityBridge.getReasonFor(packageName))
            return
        }

        // Re-check the daily limits (but not for Focus's own screens).
        if (packageName != this.packageName) {
            scheduleLimitCheck(LIMIT_CHECK_DELAY_MILLIS)
        }
    }

    /** The phone joined or left a Wi-Fi: update the blocks, and block the app on screen if it's now blocked. */
    private fun onWifiChanged() {
        updateWifiBlocks()
        val onScreen = foregroundPackage ?: return
        if (onScreen != packageName && onScreen in AccessibilityBridge.restrictedPackages.value) {
            launchBlockedScreen(onScreen, AccessibilityBridge.getReasonFor(onScreen))
        }
    }

    /** Blocks the apps of every switched-on Wi-Fi entry that watches the current network. */
    private fun updateWifiBlocks() {
        val ssid = wifiWatcher?.currentSsid
        if (ssid == null) {
            AccessibilityBridge.clearBlocks(BlockSource.WIFI)
            return
        }
        val reason = "Blocked while you're connected to $ssid Wi-Fi."
        val packages = wifiGroupStorage.getGroupsWithoutIcons().orEmpty()
            .filter { it.enabled && ssid in it.watchedSsids }
            .flatMap { group -> group.apps.map { it.packageName } }
            .filter { it != packageName } // never block Focus itself
        AccessibilityBridge.setBlocks(BlockSource.WIFI, packages.associateWith { reason })
    }

    /**
     * Runs a limit check after [delayMillis], replacing any check still waiting.
     * The short delay gives Android time to write the app switch into the usage log.
     */
    @Synchronized
    private fun scheduleLimitCheck(delayMillis: Long) {
        limitCheckJob?.cancel()
        limitCheckJob = serviceScope.launch {
            delay(delayMillis)
            val nextCheckInMillis = runLimitCheck() ?: return@launch
            rescheduleIfStillCurrent(coroutineContext[Job], nextCheckInMillis)
        }
    }

    // Only reschedule if no newer check replaced this one in the meantime.
    @Synchronized
    private fun rescheduleIfStillCurrent(job: Job?, delayMillis: Long) {
        if (limitCheckJob === job) scheduleLimitCheck(delayMillis)
    }

    /** Blocks every app that is over its limit. Returns when to check again, or null. */
    private fun runLimitCheck(): Long? {
        if (!hasUsageAccessPermission(this)) return null
        val groups = groupStorage.getGroupsWithoutIcons() ?: return null

        val result = evaluateUsageLimit.execute(
            groups = groups,
            usageInWindow = { start, end -> queryAppUsageInWindow(this, start, end) }
        )
        updateTimeFocusNotifications(groups)

        val sessionBlocked = AccessibilityBridge.restrictedPackages.value
        result.violations
            .filter { it.packageName != packageName }        // never block Focus itself
            .filter { it.packageName !in sessionBlocked }    // the session's block (and reason) wins
            .forEach { violation -> launchBlockedScreen(violation.packageName, violation.reason) }
        return result.nextCheckInMillis
    }

    /** Shows a status-bar card for every Time Focus slot that is on right now, and removes the others. */
    private fun updateTimeFocusNotifications(groups: List<BlockedAppGroup>) {
        val now = System.currentTimeMillis()
        limitedPackages = groups
            .filter { it.maxOpensPerApp != null || it.maxMinutesPerApp != null }
            .flatMap { group -> group.apps.map { it.packageName } }
            .toSet()
        for (group in groups) {
            val hasLimits = group.maxOpensPerApp != null || group.maxMinutesPerApp != null
            val window = group.schedule.windowOn()
            val isOn = group.enabled && hasLimits && window != null && now >= window.startMillis && now < window.endMillis
            if (isOn && window != null) {
                val usage = queryAppUsageInWindow(this, window.startMillis, now)
                TimeFocusNotification.show(this, group, usage, lastOpenedPackage, window.endMillis)
            } else {
                TimeFocusNotification.cancel(this, group.id)
            }
        }
    }

    /**
     * Android doesn't let an app close another app, so we open the blocked
     * screen on top of it instead. NEW_TASK is needed to start a screen from a service.
     */
    private fun launchBlockedScreen(packageName: String, reason: String? = null) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, packageName)
            reason?.let { putExtra(BlockedActivity.EXTRA_BLOCK_REASON, it) }
        }
        startActivity(intent)
    }

    // Required by Android; this service gives no feedback, so there is nothing to interrupt.
    override fun onInterrupt() = Unit

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        wifiWatcher?.stop()
        wifiWatcher = null
        AccessibilityBridge.clearBlocks(BlockSource.WIFI)
        AccessibilityBridge.setServiceConnected(false)
    }

    private companion object {
        const val LIMIT_CHECK_DELAY_MILLIS = 300L
    }
}
