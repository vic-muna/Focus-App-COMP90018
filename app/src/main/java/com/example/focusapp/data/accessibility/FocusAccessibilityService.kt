package com.example.focusapp.data.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.focusapp.BlockedActivity

/**
 * FocusAccessibilityService
 * ----------------------------
 * A real, working AccessibilityService - the prototype for the
 * "AccessibilityService: App Restriction" technical decision from the
 * project plan. This is genuinely how apps like parental-control /
 * screen-time-limit apps detect and restrict other apps on Android,
 * since there is no ordinary API for "tell me what app is in the
 * foreground" or "close that other app for me".
 *
 * HOW ANDROID DRIVES THIS CLASS (you never call these methods yourself -
 * the OS calls them):
 *  1. The user manually turns this service on, in
 *     Settings > Accessibility > [your app]. There's a shortcut button
 *     for this on AppsScreen's test panel. Android deliberately does NOT
 *     let an app silently enable its own AccessibilityService - since
 *     these services can read screen content system-wide, turning one on
 *     always requires an explicit, informed action in system Settings.
 *  2. Once enabled, Android instantiates this class in the background
 *     and calls [onServiceConnected] exactly once.
 *  3. From then on, whenever an event matching the filter in
 *     res/xml/accessibility_service_config.xml fires ANYWHERE on the
 *     device, Android calls [onAccessibilityEvent]. We filtered to just
 *     `typeWindowStateChanged`, which fires whenever the foreground
 *     window changes - in practice, whenever the user switches apps.
 *  4. If the user turns the service off again, Android calls [onDestroy].
 */
class FocusAccessibilityService : AccessibilityService() {

    /**
     * Called once, when the OS finishes connecting this service after the
     * user enables it in Settings - the AccessibilityService equivalent
     * of Activity.onCreate(). We only use it to flip a flag in
     * [AccessibilityBridge] so the UI can show "Connected".
     */
    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityBridge.setServiceConnected(true)
    }

    /**
     * THE core function - this is "how the app knows other apps are
     * running". Android calls this every time a system-wide accessibility
     * event fires that matches our config's event-type filter.
     *
     * @param event carries `event.packageName`: the package that owns the
     *        window that just came to the front. That single field is the
     *        entire detection mechanism - no polling loop, no guessing,
     *        no separate "check what's running" API call needed.
     */
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val packageName = event?.packageName?.toString() ?: return

        // Report every foreground-app change to the UI, whether or not it
        // ends up being restricted, so you can watch detection happen
        // live on AppsScreen's test panel.
        AccessibilityBridge.reportForegroundApp(packageName)

        // THE restriction check: is this package one the UI told us to block?
        if (packageName in AccessibilityBridge.restrictedPackages.value) {
            AccessibilityBridge.recordBlockEvent(packageName)
            launchBlockedScreen(packageName)
        }
    }

    /**
     * [David Shiau, 2026-09-20] We can't literally "close" another app -
     * Android does not allow that without root/device-owner privileges, for
     * good reason. The standard technique real parental-control apps use
     * instead is to immediately put something else on top of it - here,
     * [BlockedActivity], launched into its own task (FLAG_ACTIVITY_NEW_TASK)
     * so it doesn't join the blocked app's back stack. Starting an Activity
     * from a Service (not itself an Activity) requires this flag.
     * Replaces the earlier `performGlobalAction(GLOBAL_ACTION_HOME)`
     * approach, which only bounced the user home with no explanation.
     */
    private fun launchBlockedScreen(packageName: String) {
        val intent = Intent(this, BlockedActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            putExtra(BlockedActivity.EXTRA_BLOCKED_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    /**
     * Required override with no useful hook for this use case - Android
     * calls this if it needs to interrupt whatever accessibility feedback
     * the service was giving (more relevant to services like screen
     * readers, which speak/vibrate). We don't produce any feedback, so
     * there is nothing to interrupt.
     */
    override fun onInterrupt() {
        // Intentionally empty - see doc comment above.
    }

    override fun onDestroy() {
        super.onDestroy()
        AccessibilityBridge.setServiceConnected(false)
    }
}
