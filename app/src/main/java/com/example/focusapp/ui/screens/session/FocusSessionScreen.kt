package com.example.focusapp.ui.screens.session

import android.os.SystemClock
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.R
import com.example.focusapp.data.accessibility.AccessibilityBridge
import com.example.focusapp.data.audio.FocusMusicPlayer
import com.example.focusapp.data.preferences.FocusMusicStorage
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.data.sensor.MotionSensorDataSource
import com.example.focusapp.domain.model.FocusMusics
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.usecase.ShakeDetector
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.common.vibrateShort
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.example.focusapp.ui.components.button.InfoButton

// Matches the hint bubble's "Hold for 3 seconds" copy - change both together.
private const val CANCEL_HOLD_DURATION_MILLIS = 3_000L
private const val EXIT_HINT = "Hold for 3 seconds or shake\nthe phone to exit the focus mode"
private const val CANCEL_HOLD_STEP_MILLIS = 50L
private const val HINT_AUTO_HIDE_MILLIS = 5_000L

/** What started a focus session. */
sealed class FocusSessionSource {
    data object Manual : FocusSessionSource()
    data object Party : FocusSessionSource()
    /** Flipping the phone face-down (Settings -> Flip to Focus). */
    data object Flip : FocusSessionSource()
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
 * button that shows how to leave. Holding anywhere for 3 seconds, shaking the
 * phone (or pressing Back) ends the session, which is saved first.
 */
@Composable
fun FocusSessionScreen(
    session: ActiveFocusSession,
    onEndSessionClick: () -> Unit,
    // The picked background theme's Focus Mode art (see BackgroundThemes).
    @DrawableRes backgroundArt: Int = R.drawable.img_focus_background,
    // Its looping video, if it has one - plays over [backgroundArt].
    @RawRes backgroundVideo: Int? = null,
    // The timer and noise label colour for that background (BackgroundTheme.focusTextColor).
    textColor: Color = FocusTheme.colors.sessionTimer,
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

    val lifecycleOwner = LocalLifecycleOwner.current

    // Focus Music: plays the picked track while this screen is open. The note under the "i"
    // turns it on or off - saved, so it's the same switch as in Settings.
    val musicSettings = remember { FocusMusicStorage(context) }
    var musicOn by remember { mutableStateOf(musicSettings.isEnabled()) }
    DisposableEffect(musicOn) {
        val player = if (musicOn) {
            FocusMusicPlayer(context, FocusMusics.byId(musicSettings.getSelectedId())).also { it.start() }
        } else {
            null
        }
        onDispose { player?.stop() }
    }

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

    // Shaking the phone ends the session too, like holding (see ShakeDetector).
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            val shakeDetector = ShakeDetector()
            MotionSensorDataSource(context).gForceFlow().collect { gForce ->
                if (shakeDetector.onReading(gForce, SystemClock.elapsedRealtime()) && !isEnding) {
                    vibrateShort(context)
                    cancelSession()
                }
            }
        }
    }

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
        if (backgroundVideo != null) {
            VideoBackground(video = backgroundVideo, modifier = Modifier.fillMaxSize())
        }

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

        // Under the "i": 56 dp from the top + the 43 dp "i" + a small gap. A bit bigger than the
        // "i" (its art has a transparent margin) and centred under it: 24 dp end - (48 - 43) / 2.
        Image(
            painter = painterResource(if (musicOn) R.drawable.ic_music_on else R.drawable.ic_music_off),
            contentDescription = if (musicOn) "Mute music" else "Play music",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 56.dp + 43.dp + 10.dp, end = 21.5.dp)
                .size(48.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button) {
                    musicOn = !musicOn
                    musicSettings.saveEnabled(musicOn)
                },
        )

        // NoiseAlert reads sessionTimer too, so swap it in for this background.
        FocusAppTheme(colors = colors.copy(sessionTimer = textColor)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 150.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = elapsedLabel,
                    style = FocusTheme.typography.timer,
                    color = textColor
                )
                NoiseAlert()
            }
        }

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
