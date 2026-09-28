package com.example.focusapp.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.focusapp.BlockedActivity
import com.example.focusapp.data.blocking.BlockedAppGroupStorage
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

/**
 * The app blocker. Android runs it in the background after the user turns
 * it on in Settings > Accessibility, and calls [onAccessibilityEvent]
 * every time the user switches to another app
 * (see res/xml/accessibility_service_config.xml).
 *
 * An app is blocked (by opening [BlockedActivity] on top of it) when:
 *  1. the current focus session blocks it ([AccessibilityBridge.restrictedPackages]), or
 *  2. it went over a Time Focus daily limit ([EvaluateUsageLimitUseCase]).
 */
class FocusAccessibilityService : AccessibilityService() {

    // Limit checks read the usage log, so they run in the background.
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var limitCheckJob: Job? = null
    private val groupStorage by lazy { BlockedAppGroupStorage(this) }
    private val evaluateUsageLimit = EvaluateUsageLimitUseCase()

    /** The user turned the service on. */
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.setServiceConnected(true)
        scheduleLimitCheck(0L) // An app may already be open and over its limit.
    }

    /** The user switched apps; [event]'s packageName is the app now on screen. */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        if (packageName in AccessibilityBridge.restrictedPackages.value) {
            launchBlockedScreen(packageName, AccessibilityBridge.restrictionReason)
            return
        }

        // Re-check the daily limits (but not for Focus's own screens).
        if (packageName != this.packageName) {
            scheduleLimitCheck(LIMIT_CHECK_DELAY_MILLIS)
        }
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
        val sessionBlocked = AccessibilityBridge.restrictedPackages.value
        result.violations
            .filter { it.packageName != packageName }        // never block Focus itself
            .filter { it.packageName !in sessionBlocked }    // the session's block (and reason) wins
            .forEach { violation -> launchBlockedScreen(violation.packageName, violation.reason) }
        return result.nextCheckInMillis
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
        AccessibilityBridge.setServiceConnected(false)
    }

    private companion object {
        const val LIMIT_CHECK_DELAY_MILLIS = 300L
    }
}
