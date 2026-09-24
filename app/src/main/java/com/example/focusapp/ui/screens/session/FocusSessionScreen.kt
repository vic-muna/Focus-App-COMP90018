package com.example.focusapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.common.friendlyErrorMessage
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CANCEL_HOLD_DURATION_MILLIS = 3_000L
private const val CANCEL_HOLD_STEP_MILLIS = 50L

// Same fixed duration Party Mode's own wipe uses (see PartyModeScreen.kt),
// applied symmetrically to both the entrance reveal and the cancel/reverse-cover -
// no separate "calculated"/synced enter-vs-exit timing.
private const val WIPE_DURATION_MILLIS = 800

/** How long this screen waits, unchanged, after cancellation is triggered before
 *  actually starting the reverse wipe/leaving - time for a background animation to
 *  play first (not built yet). Mirrors Home's own delay before entering. */
private const val FOCUS_SESSION_END_DELAY_MILLIS = 2_000L

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
 * Owns the same diagonal-wipe reveal/cover animation as Home -> Party Mode (see
 * PartyModeScreen.kt's doc comment for the full rationale of why it's a sweeping
 * gradient band rather than a translated/scaled box) - an independent copy of that
 * mechanism rather than shared code, matching how other small self-contained UI
 * pieces in this project are duplicated per-screen, using the exact same fixed
 * [WIPE_DURATION_MILLIS] (800ms) for both the entrance reveal and the cancel's
 * reverse-cover. The NavGraph-level transition into/out of this screen is also
 * just a plain fade (see NavGraph.kt's partyModeEnter/partyModeExit, reused
 * as-is) - no synced durations or special-cased transitions of its own.
 *
 * Deliberately minimal otherwise: just the elapsed time at the top, on a full-bleed
 * placeholder background - the real "idle-game style" animation is separate future
 * visual work, not designed here. There's no button to end the session - holding
 * anywhere on screen for CANCEL_HOLD_DURATION_MILLIS cancels focus (mirrors a "hold
 * to confirm" pattern so it can't be triggered by an accidental tap), which plays
 * the same wipe in reverse before actually leaving - same as the system back
 * gesture, intercepted via BackHandler so it can't skip the animation either.
 */


@Composable
fun FocusSessionScreen(
    session: ActiveFocusSession,
    onEndSessionClick: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 0f = fully covered by the wipe layer, 1f = fully revealed.
    val revealProgress = remember { Animatable(0f) }
    var screenWidthPx by remember { mutableIntStateOf(0) }
    var screenHeightPx by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        revealProgress.animateTo(1f, tween(WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }

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
    val elapsedLabel = "%02d:%02d:%02d".format(
        elapsedSeconds / 3600,
        (elapsedSeconds % 3600) / 60,
        elapsedSeconds % 60
    )

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
        scope.launch {
            delay(FOCUS_SESSION_END_DELAY_MILLIS)
            revealProgress.animateTo(0f, tween(WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
            saveAndFinish()
        }
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
            .background(WireframeColors.Background)
            .onSizeChanged {
                screenWidthPx = it.width
                screenHeightPx = it.height
            }
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
        Text(
            text = elapsedLabel,
            fontSize = 25.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 32.dp)
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
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
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
                    .background(Color.Black.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(holdProgress)
                        .fillMaxHeight()
                        .background(Color.Black, RoundedCornerShape(3.dp))
                )
            }
        }

        // Wipe layer - stays put, fillMaxSize, never translated or scaled;
        // only the gradient's own transition band sweeps along the
        // bottom-left -> top-right diagonal as revealProgress goes 0 -> 1.
        // The "covering" color MUST match the real screen background
        // (WireframeColors.Background, the same color the outer Box below
        // uses) - using anything else (e.g. plain white) means the wipe
        // never actually blends into the background it's supposed to be
        // covering/revealing, showing a mismatched color instead.
        val bandWidth = 0.18f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            (revealProgress.value - bandWidth).coerceIn(0f, 1f) to
                                WireframeColors.Background.copy(alpha = 0f),
                            revealProgress.value.coerceIn(0f, 1f) to
                                WireframeColors.Background
                        ),
                        start = Offset(0f, screenHeightPx.toFloat()),
                        end = Offset(screenWidthPx.toFloat(), 0f)
                    )
                )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun FocusSessionScreenPreview() {
    FocusSessionScreen(
        session = ActiveFocusSession(
            startTimeMillis = System.currentTimeMillis() - 65_000,
            source = FocusSessionSource.Schedule(groupId = "group_study", groupName = "Study Group"),
            groupId = "group_study"
        ),
        onEndSessionClick = {}
    )
}
