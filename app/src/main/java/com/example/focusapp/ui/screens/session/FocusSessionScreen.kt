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
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.annotation.DrawableRes
import com.example.focusapp.ui.components.button.InfoButton

// Matches the hint bubble's "Hold for 5 seconds" copy - change both together.
private const val CANCEL_HOLD_DURATION_MILLIS = 5_000L
private const val EXIT_HINT = "Hold for 5 seconds to exit\nthe focus mode"
private const val CANCEL_HOLD_STEP_MILLIS = 50L
private const val HINT_AUTO_HIDE_MILLIS = 5_000L

/** What started a focus session. */
sealed class FocusSessionSource {
    data object Manual : FocusSessionSource()
    data object Party : FocusSessionSource()
    data class Location(val zoneName: String, val zoneId: String) : FocusSessionSource()
    data class Wifi(val ssid: String) : FocusSessionSource()
}

/** The running focus session (kept in NavGraph.kt). */
data class ActiveFocusSession(
    val startTimeMillis: Long,
    val source: FocusSessionSource
)

/**
 * The focus timer screen: the background art, the elapsed time, and an "i"
 * button that shows how to leave. Holding anywhere for 5 seconds (or pressing
 * Back) ends the session, which is saved first.
 */
@Composable
fun FocusSessionScreen(
    session: ActiveFocusSession,
    onEndSessionClick: () -> Unit,
    // The picked background theme's Focus Mode art (see BackgroundThemes).
    @DrawableRes backgroundArt: Int = R.drawable.img_focus_background,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Set once the session is ending, so the timer stops updating while the screen fades out.
    var isEnding by remember { mutableStateOf(false) }

    // Set if saving the session fails; shown with a "Continue" button instead of crashing.
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
    // Closes itself after a few seconds (tapping the i again still closes it sooner).
    LaunchedEffect(showExitHint) {
        if (showExitHint) {
            delay(HINT_AUTO_HIDE_MILLIS)
            showExitHint = false
        }
    }
    val colors = FocusTheme.colors

    fun saveAndFinish() {
        scope.launch {
            val completed = FocusSession(
                id = "session_${System.currentTimeMillis()}",
                startTimeMillis = session.startTimeMillis,
                endTimeMillis = System.currentTimeMillis(),
                // Blocked apps the user tried to open (counted by FocusAccessibilityService).
                distractingAppOpenCount = AccessibilityBridge.blockedOpensSoFar(),
                wasCompletedSuccessfully = true,
                groupId = null
            )
            try {
                withContext(Dispatchers.IO) {
                    FocusRepositoryProvider.get(context).saveFocusSession(completed)
                }
                onEndSessionClick()
            } catch (e: Exception) {
                // Show the error and wait for "Continue", so the message isn't missed.
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
                        painter = painterResource(backgroundArt),
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
                source = FocusSessionSource.Manual
            ),
            onEndSessionClick = {}
        )
    }
}
