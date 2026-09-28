package com.example.focusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.runtime.mutableStateOf
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.ui.screens.blocked.BlockedScreen
import com.example.focusapp.ui.theme.FocusAppTheme

/**
 * The "you can't use this app right now" screen, opened by FocusAccessibilityService
 * on top of a blocked app. It is a separate Activity because the service can't
 * show anything on screen by itself.
 *
 * It runs in its own task (see the manifest). "Got it" and Back never just close it
 * (that could show the blocked app again):
 *  - during a focus session: back to Focus's timer screen
 *  - otherwise (a daily limit was reached): to the phone's home screen
 */
class BlockedActivity : ComponentActivity() {

    // Kept outside Compose so onNewIntent can update it while the screen is showing.
    private val blockedAppLabelState = mutableStateOf("This app")

    // Why it was blocked (e.g. "You've reached your limit of 3 times ..."), or null for the default message.
    private val blockReasonState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateBlockedLabelFrom(intent)

        setContent {
            FocusAppTheme {
                // Back does the same as "Got it".
                BackHandler { leave() }
                BlockedScreen(
                    appLabel = blockedAppLabelState.value,
                    reason = blockReasonState.value,
                    onGotItClick = { leave() }
                )
            }
        }
    }

    /** Called instead of onCreate when another app gets blocked while this screen is already open. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        updateBlockedLabelFrom(intent)
    }

    private fun updateBlockedLabelFrom(intent: Intent) {
        val blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        blockedAppLabelState.value = blockedPackageName?.let { getAppLabel(this, it) } ?: "This app"
        blockReasonState.value = intent.getStringExtra(EXTRA_BLOCK_REASON)
    }

    /** Leaves this screen: back to the focus timer if a session is running, else to the phone's home screen. */
    private fun leave() {
        val isFocusing = AccessibilityBridge.restrictedPackages.value.isNotEmpty()
        val nextScreen = if (isFocusing) {
            // Brings the open Focus app (still on its timer screen) back to the front.
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
        } else {
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
        startActivity(nextScreen)
        finish()
    }

    companion object {
        /** Intent extra key FocusAccessibilityService uses to say which app got blocked. */
        const val EXTRA_BLOCKED_PACKAGE = "blocked_package_name"

        /** Optional Intent extra: a human-readable reason, shown instead of the default message. */
        const val EXTRA_BLOCK_REASON = "block_reason"
    }
}