package com.example.focusapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.ui.theme.WireframeColors

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
 * separate from the blocked app's task. Both the "Got it" button and the
 * system Back gesture ([BackHandler] below) explicitly navigate Home
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

    // [David Shiau, 2026-09-26] Why it was blocked, for a Scheduled Limits
    // block (e.g. "You've reached your limit of 3 times ..."); null for a
    // focus-session block, which keeps the original message.
    private val blockReasonState = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateBlockedLabelFrom(intent)

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    BlockedScreen(
                        appLabel = blockedAppLabelState.value,
                        reason = blockReasonState.value,
                        onGotItClick = { goHomeAndFinish() }
                    )
                }
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
        val blockedPackageName = intent.getStringExtra(EXTRA_BLOCKED_PACKAGE)
        blockedAppLabelState.value = blockedPackageName?.let { getAppLabel(this, it) } ?: "This app"
        blockReasonState.value = intent.getStringExtra(EXTRA_BLOCK_REASON)
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

        /** Optional Intent extra: a human-readable reason, shown instead of the default message. */
        const val EXTRA_BLOCK_REASON = "block_reason"
    }
}

/**
 * BlockedScreen
 * ---------------
 * The actual UI: an icon, the blocked app's real name, a short
 * explanation, and a "Got it" pill button - styled with the same
 * [WireframeColors] as the rest of the app for visual consistency.
 */
@Composable
private fun BlockedScreen(appLabel: String, reason: String?, onGotItClick: () -> Unit) {
    // Handles the system Back button/gesture the same way as the "Got it"
    // button - see BlockedActivity's class doc comment for why this can't
    // just be left to the default Back behaviour.
    BackHandler(onBack = onGotItClick)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "🚫", fontSize = 64.sp)

        Text(
            text = "$appLabel is blocked",
            color = WireframeColors.OnLight,
            fontSize = 22.sp,
            modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
        )

        Text(
            text = reason ?: "This app is in your Focus restricted list right now.",
            textAlign = TextAlign.Center,
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Text(
            text = "Got it",
            color = WireframeColors.OnDark,
            modifier = Modifier
                .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                .clickable(onClick = onGotItClick)
                .padding(horizontal = 32.dp, vertical = 14.dp)
        )
    }
}
