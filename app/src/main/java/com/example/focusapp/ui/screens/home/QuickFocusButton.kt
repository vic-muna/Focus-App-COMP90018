package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusBlobShape
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Opacity of the outer blob in Figma (layer 0.31 x fill 0.6). */
private const val OUTER_BLOB_ALPHA = 0.31f * 0.6f

/** Inner blob rotation in Figma, so its corners sit between the outer blob's. */
private const val INNER_BLOB_ROTATION = 39.16f

/** Hold this long, then let go, to start focusing. */
const val QUICK_FOCUS_START_HOLD_MILLIS = 1_000L

/** Hold this long to open the Quick Focus app settings. */
const val QUICK_FOCUS_SETTINGS_HOLD_MILLIS = 3_000L

/** Shorter presses than this still count as a tap (the label doesn't change). */
private const val TAP_MILLIS = 200L

/**
 * Figma: "Focuse Buttom" - two stacked blobs with a centered label.
 * It works by how long it is held:
 *  - tap (under 1 s): [onTap] (Home shows a hint)
 *  - hold 1-3 s, then let go: [onHoldStart] (start focusing)
 *  - hold 3 s: [onHoldSettings] (pick the apps to block), right away
 * The label changes while holding, and the phone vibrates at 1 s and 3 s.
 */
@Composable
fun QuickFocusButton(
    onTap: () -> Unit,
    onHoldStart: () -> Unit,
    onHoldSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val haptics = LocalHapticFeedback.current

    // How long the button has been held so far (0 = not pressed).
    var heldMillis by remember { mutableLongStateOf(0L) }

    // The gesture below keeps running across recompositions, so it reads the latest callbacks through these.
    val currentOnTap by rememberUpdatedState(onTap)
    val currentOnHoldStart by rememberUpdatedState(onHoldStart)
    val currentOnHoldSettings by rememberUpdatedState(onHoldSettings)

    val label = when {
        heldMillis >= QUICK_FOCUS_START_HOLD_MILLIS -> "Let go\nto focus"
        heldMillis >= TAP_MILLIS -> "Keep\nholding"
        else -> "Quick\nFocus"
    }

    Box(
        modifier = modifier
            .size(175.dp)
            .clip(FocusBlobShape)
            .background(colors.primaryAction.copy(alpha = OUTER_BLOB_ALPHA))
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        coroutineScope {
                            // Counts up while the finger is down.
                            val timer = launch {
                                val pressedAt = System.currentTimeMillis()
                                var vibratedAtStart = false
                                while (heldMillis < QUICK_FOCUS_SETTINGS_HOLD_MILLIS) {
                                    delay(50)
                                    heldMillis = System.currentTimeMillis() - pressedAt
                                    if (!vibratedAtStart && heldMillis >= QUICK_FOCUS_START_HOLD_MILLIS) {
                                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                        vibratedAtStart = true
                                    }
                                }
                                // Held for 3 s: open the settings without waiting for the finger to lift.
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                currentOnHoldSettings()
                            }
                            tryAwaitRelease()
                            timer.cancel()
                        }
                        val held = heldMillis
                        heldMillis = 0L
                        when {
                            held >= QUICK_FOCUS_SETTINGS_HOLD_MILLIS -> Unit // Settings already opened.
                            held >= QUICK_FOCUS_START_HOLD_MILLIS -> currentOnHoldStart()
                            else -> currentOnTap()
                        }
                    },
                )
            }
            // For screen readers, which can't hold: a double-tap starts focusing.
            .semantics {
                role = Role.Button
                onClick(label = "Start Quick Focus") {
                    currentOnHoldStart()
                    true
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(145.dp)
                .rotate(INNER_BLOB_ROTATION)
                .background(colors.primaryAction, FocusBlobShape)
        )
        Text(
            text = label,
            style = FocusTheme.typography.primaryActionLabel,
            color = colors.onPrimaryAction,
            textAlign = TextAlign.Center,
        )
    }
}

@Preview
@Composable
private fun QuickFocusButtonPreview() {
    FocusAppTheme {
        Box(Modifier.background(FocusTheme.colors.background)) {
            QuickFocusButton(onTap = {}, onHoldStart = {}, onHoldSettings = {})
        }
    }
}
