package com.example.focusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.accessibility.FocusRestManager
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.ui.screens.blocked.BlockedScreen
import com.example.focusapp.ui.theme.FocusAppTheme

/**
 * BlockedActivity
 * -----------------
 * The "you can't use this app right now" screen. [com.example.focusapp.data.accessibility.FocusAccessibilityService]
 * launches this instead of silently bouncing the user to the home screen
 * (the old `GLOBAL_ACTION_HOME` behaviour) whenever a restricted app
 * comes to the foreground.
 *
 * WHY A WHOLE SEPARATE ACTIVITY, NOT A TOAST/SNACKBAR: a Toast or Snackbar
 * can only be shown from inside a currently-visible Activity of the app
 * that requests it - FocusAccessibilityService isn't an Activity, and by
 * the time it runs, Focus itself may not be on screen at all (the user
 * could be deep inside Instagram). Launching an Activity is the one thing
 * that can put content on screen no matter what else was open - it's the
 * same mechanism a phone call or alarm uses to interrupt whatever you're doing.
 *
 * HOW THIS AVOIDS BOUNCING THE USER RIGHT BACK INTO THE BLOCKED APP: this
 * screen is launched into its OWN task (see AndroidManifest.xml's
 * `launchMode="singleTask"` + `taskAffinity=""` on this Activity), kept
 * separate from the blocked app's task. Both the Reject button and the
 * system Back gesture ([BackHandler] below) explicitly navigate back to Focus
 * rather than just calling `finish()` - if they only called finish(),
 * Android's default back-stack behaviour could reveal the blocked app's
 * task underneath, defeating the whole point of this screen.
 */
class BlockedActivity : ComponentActivity() {

    // A plain Compose `State`, not `remember`ed - it's created once per
    // Activity instance (as a property, not inside a Composable) so that
    // [onNewIntent] can update it even while this screen is already on
    // screen (see that override below for why that case can happen).
    private val blockedAppLabelState = mutableStateOf("This app")
    private var blockedPackageName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateBlockedLabelFrom(intent)

        setContent {
            FocusAppTheme {
                // [Claude, 2026-09-26] Rests come from the running session's app group -
                // see FocusRestManager and NavGraph.sessionGroupFor().
                val restState by FocusRestManager.state.collectAsState()
                // The system Back gesture does the same as Reject - see the
                // class doc comment for why it can't be left to the default.
                BackHandler { returnToFocusAndFinish() }
                BlockedScreen(
                    appLabel = blockedAppLabelState.value,
                    restsLeft = restState?.restsLeft,
                    restsTotal = restState?.restsTotal,
                    onTakeRest = ::takeRestAndOpenApp,
                    onReject = ::returnToFocusAndFinish
                )
            }
        }
    }

    /**
     * Because this Activity is `launchMode="singleTask"`, Android reuses
     * the existing instance (calling THIS, not [onCreate] again) if the
     * user manages to trigger another restricted app while this screen is
     * already showing. Without this override, the message would keep
     * showing whichever app was blocked FIRST, even after a second,
     * different app gets blocked.
     */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateBlockedLabelFrom(intent)
    }

    private fun updateBlockedLabelFrom(intent: Intent) {
        blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        blockedAppLabelState.value = blockedPackageName?.let { getAppLabel(this, it) } ?: "This app"
    }

    /**
     * Confirm: spend one rest (which lifts blocking for the rest's length),
     * then open the app the user was trying to use. When the rest ends, if
     * they're still in a restricted app, this screen comes straight back -
     * the accessibility service only reacts to app switches, so it wouldn't
     * notice on its own.
     */
    private fun takeRestAndOpenApp() {
        val appContext = applicationContext
        val started = FocusRestManager.takeRest { restored ->
            val foreground = AccessibilityBridge.currentForegroundApp.value
            if (foreground != null && foreground in restored) {
                appContext.startActivity(
                    Intent(appContext, BlockedActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra(EXTRA_BLOCKED_PACKAGE, foreground)
                    }
                )
            }
        }
        if (!started) return
        val launch = blockedPackageName?.let { packageManager.getLaunchIntentForPackage(it) }
        if (launch != null) {
            startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        } else {
            returnToFocusAndFinish()
        }
    }

    /**
     * Reject / Back: bring Focus itself (showing the running session) to the
     * front instead of the blocked app - see the class doc comment for why
     * just finish()ing isn't safe. Falls back to the home screen when no
     * session is running.
     */
    private fun returnToFocusAndFinish() {
        val focusApp = packageManager.getLaunchIntentForPackage(packageName)
        if (FocusRestManager.state.value != null && focusApp != null) {
            startActivity(focusApp.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            finish()
        } else {
            goHomeAndFinish()
        }
    }

    /**
     * Explicitly navigates to the home screen, THEN finishes this
     * Activity - see the class doc comment for why "just finish()" isn't
     * safe enough here.
     */
    private fun goHomeAndFinish() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        /** Intent extra key FocusAccessibilityService uses to say which app got blocked. */
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package_name"
    }
}
