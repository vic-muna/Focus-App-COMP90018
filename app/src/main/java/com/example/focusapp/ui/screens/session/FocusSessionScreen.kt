package com.example.focusapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.components.HintBubble
import com.example.focusapp.ui.components.InfoButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Matches the hint bubble's "Hold for 5 seconds" copy - change both together.
private const val CANCEL_HOLD_DURATION_MILLIS = 5_000L
private const val EXIT_HINT = "Hold for 5 seconds to exit\nthe focus mode"
private const val CANCEL_HOLD_STEP_MILLIS = 50L

/** Where a focus session was started from - lets a saved [FocusSession] carry which
 *  group triggered it, for a schedule match. */
sealed class FocusSessionSource {
    data object Manual : FocusSessionSource()
    data object Party : FocusSessionSource()
    data class Schedule(val groupId: String, val groupName: String) : FocusSessionSource()
    data class Location(val zoneName: String) : FocusSessionSource()
    data class Wifi(val ssid: String) : FocusSessionSource()
}

/** The one hoisted piece of state (in NavGraph.kt) for "is a focus session running right now". */
data class ActiveFocusSession(
    val startTimeMillis: Long,
    val source: FocusSessionSource,
    val groupId: String? = null
)

/**
 * Full-screen "in a focus session" screen. Reached from three places - Home's Quick
 * Focus button, Party Mode's "Go Focus Mode" button, and Home's auto-suggestion banner
 * (see HomeScreenWithSheet.kt) - all of which funnel into the same [ActiveFocusSession]
 * hoisted in NavGraph.kt.
 *
 * Has no entrance/exit animation of its own - moving into and out of this
 * screen is just NavGraph's dissolve (see NavGraph.kt's partyModeEnter /
 * partyModeExit).
 *
 * Layout follows the Figma "Focuse Mode" frames: a full-bleed illustration,
 * the elapsed time near the top, and an "i" button (top-right) that toggles a
 * hint bubble explaining how to leave. There's no button to end the session -
 * holding anywhere on screen for CANCEL_HOLD_DURATION_MILLIS cancels focus
 * (mirrors a "hold to confirm" pattern so it can't be triggered by an
 * accidental tap) - same as the system back gesture, intercepted via
 * BackHandler so leaving always saves the session first.
 */
@Composable
fun FocusSessionScreen(
    session: ActiveFocusSession,
    onEndSessionClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // True from the moment cancellation starts until this screen is gone. Stops
    // the tick loop below (and further hold-gesture handling) from continuing to
    // recompose this screen while it's mid pop-exit - AnimatedContent's exit
    // transition (see NavGraph.kt) doesn't dispose this composable until the
    // transition finishes, so without this guard the tick loop keeps mutating
    // state and recomposing this screen for the whole fade-out, which is what
    // was producing the "reopening" artifact on the way back to Home (Party
    // Mode has no such ongoing ticker, which is why it never showed this).
    var isEnding by remember { mutableStateOf(false) }

    // Set when saveFocusSession() throws (a Room-level failure - Firebase push failures are
    // already swallowed inside FocusRepositoryImpl, so this only fires for a genuinely rare
    // local-storage error). Shown via ErrorBanner with a manual "Continue" tap, rather than
    // either crashing or silently losing the session and leaving the user with no idea.
    var saveError by remember { mutableStateOf<String?>(null) }

    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (!isEnding) {
            delay(1000)
            tick = System.currentTimeMillis()
        }
    }
    val elapsedSeconds = ((tick - session.startTimeMillis) / 1000).coerceAtLeast(0)
    val elapsedLabel = formatElapsed(elapsedSeconds)
    var showExitHint by remember { mutableStateOf(false) }
    val colors = FocusTheme.colors

    fun saveAndFinish() {
        scope.launch {
            val completed = FocusSession(
                id = "session_${System.currentTimeMillis()}",
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = System.currentTimeMillis(),
                wasCompletedSuccessfully = true,
                groupId = session.groupId
            )
            try {
                withContext(Dispatchers.IO) {
                    FocusRepositoryProvider.get(context).saveFocusSession(completed)
                }
                onEndSessionClick()
            } catch (e: Exception) {
                // Previously uncaught - crashed the app right as the session ended.
                // Surface it and let the user explicitly continue instead (see
                // saveError's doc comment above) - don't call onEndSessionClick()
                // here so the message doesn't flash by unseen.
                saveError = friendlyErrorMessage(e, "Saving the session")
            }
        }
    }

    fun cancelSession() {
        if (isEnding) return
        isEnding = true
        saveAndFinish()
    }

    BackHandler(onBack = ::cancelSession)

    var isHolding by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableStateOf(0f) }

    LaunchedEffect(isHolding) {
        if (isHolding) {
            var elapsed = 0L
            while (isHolding && elapsed < CANCEL_HOLD_DURATION_MILLIS) {
                delay(CANCEL_HOLD_STEP_MILLIS)
                elapsed += CANCEL_HOLD_STEP_MILLIS
                holdProgress = (elapsed.toFloat() / CANCEL_HOLD_DURATION_MILLIS).coerceAtMost(1f)
            }
            if (isHolding) {
                cancelSession()
            }
        } else {
            holdProgress = 0f
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        if (!isEnding) {
                            isHolding = true
                            try {
                                tryAwaitRelease()
                            } finally {
                                isHolding = false
                            }
                        }
                    }
                )
            }
    ) {
        Image(
            painter = painterResource(R.drawable.img_focus_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp, end = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showExitHint) {
                HintBubble(text = EXIT_HINT, modifier = Modifier.padding(end = 4.dp))
            }
            InfoButton(onClick = { showExitHint = !showExitHint })
        }

        Text(
            text = elapsedLabel,
            style = FocusTheme.typography.timer,
            color = colors.sessionTimer,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 150.dp)
        )

        if (saveError != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth(0.85f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ErrorBanner(saveError!!)
                Text(
                    text = "Continue",
                    style = FocusTheme.typography.body,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            saveError = null
                            onEndSessionClick()
                        }
                        .padding(vertical = 8.dp)
                )
            }
        }

        if (isHolding) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 48.dp)
                    .fillMaxWidth(0.6f)
                    .height(6.dp)
                    .background(colors.primaryAction.copy(alpha = 0.3f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(holdProgress)
                        .fillMaxHeight()
                        .background(colors.primaryAction, RoundedCornerShape(3.dp))
                )
            }
        }
    }
}

/** "mm:ss", switching to "h:mm:ss" once a session passes an hour. */
private fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds)
    else "%02d:%02d".format(minutes, seconds)
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun FocusSessionScreenPreview() {
    FocusAppTheme {
        FocusSessionScreen(
            session = ActiveFocusSession(
                startTimeMillis = System.currentTimeMillis() - 65_000,
                source = FocusSessionSource.Schedule(groupId = "group_study", groupName = "Study Group"),
                groupId = "group_study"
            ),
            onEndSessionClick = {}
        )
    }
}
