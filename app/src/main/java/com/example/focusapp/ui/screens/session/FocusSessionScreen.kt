package com.example.focusapp.ui.screens.session

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CANCEL_HOLD_DURATION_MILLIS = 3_000L
private const val CANCEL_HOLD_STEP_MILLIS = 50L

// Slower than Party Mode's own 800ms wipe (see PartyModeScreen.kt) - this
// screen's entrance/exit is meant to be noticeably played, not a quick cut.
// Non-private: NavGraph.kt reuses it so Home's own fade-out (see
// homeExitTransition there) lasts exactly as long as this reveal, instead
// of Home cutting away quickly before this finishes revealing.
const val FOCUS_SESSION_WIPE_DURATION_MILLIS = 3_000

/** Where a focus session was started from - lets a saved [FocusSession] carry which
 *  group triggered it, for a schedule match. */
sealed class FocusSessionSource {
    data object Manual : FocusSessionSource()
    data object Party : FocusSessionSource()
    data class Schedule(val groupId: String, val groupName: String) : FocusSessionSource()
    data class Location(val zoneName: String) : FocusSessionSource()
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
 * pieces in this project are duplicated per-screen. Here it runs over
 * [FOCUS_SESSION_WIPE_DURATION_MILLIS] (3s, slower than Party Mode's 800ms) so it's
 * actually visible as its own moment rather than a quick cut - and NavGraph.kt fades
 * Home out over that exact same duration, so Home's disappearance and this screen's
 * reveal are one continuous 3s motion instead of Home cutting away quickly first.
 * Home also waits its own separate ~3s beat BEFORE that transition even starts (see
 * HomeScreenWithSheet.kt's startFocusSessionAfterDelay), so the full sequence from
 * tapping Quick Focus is: Home unchanged (~3s) -> Home fades out / this screen wipes
 * in together (~3s).
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
        revealProgress.animateTo(1f, tween(FOCUS_SESSION_WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
    }

    var tick by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
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
            withContext(Dispatchers.IO) {
                FocusRepositoryProvider.get(context).saveFocusSession(completed)
            }
            onEndSessionClick()
        }
    }

    fun cancelSession() {
        scope.launch {
            revealProgress.animateTo(0f, tween(FOCUS_SESSION_WIPE_DURATION_MILLIS, easing = FastOutSlowInEasing))
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
                        isHolding = true
                        try {
                            tryAwaitRelease()
                        } finally {
                            isHolding = false
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
