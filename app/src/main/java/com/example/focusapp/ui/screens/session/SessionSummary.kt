package com.example.focusapp.ui.screens.session

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.model.RewardRules
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import kotlinx.coroutines.launch

/**
 * Shown over the Focus Mode screen once a session is saved: how long it lasted, the points it
 * earned and how many blocked apps were tried, with a confetti burst. "Done" leaves Focus Mode.
 */
@Composable
fun SessionSummaryOverlay(session: FocusSession, onDone: () -> Unit) {
    val colors = FocusTheme.colors
    val minutes = (session.durationMillis() / 60_000).toInt()
    val points = minutes / RewardRules.MINUTES_PER_POINT

    // The card pops in with a little bounce while the backdrop fades in.
    val scale = remember { Animatable(0.6f) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { fade.animateTo(1f, tween(250)) }
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(Color.Black.copy(alpha = 0.55f))
            // Swallow taps, so holding here doesn't reach the session screen below.
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.82f)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                }
                .background(colors.surface, RoundedCornerShape(24.dp))
                .padding(horizontal = 20.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (minutes > 0) "Great focus!" else "Session ended",
                style = FocusTheme.typography.cardTitle,
                color = colors.onSurface,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = formatSummaryDuration(session.durationMillis()),
                style = FocusTheme.typography.timer,
                color = colors.accent,
            )
            Text(text = "focused", style = FocusTheme.typography.caption, color = colors.onSurfaceMuted)
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                SummaryStat(value = "+$points", label = "points", modifier = Modifier.weight(1f))
                SummaryStat(
                    value = session.distractingAppOpenCount.toString(),
                    label = if (session.distractingAppOpenCount == 1) "app blocked" else "apps blocked",
                    modifier = Modifier.weight(1f),
                )
            }
            if (minutes == 0) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Focus for at least a minute to earn points.",
                    style = FocusTheme.typography.caption,
                    color = colors.onSurfaceMuted,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(Modifier.height(24.dp))
            FocusPillButton(
                label = "Done",
                containerColor = colors.primaryAction,
                contentColor = colors.onPrimaryAction,
                onClick = onDone,
                width = 120.dp,
                height = 40.dp,
            )
        }

        // Only celebrate a session that earned something.
        if (minutes > 0) {
            ConfettiBurst(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SummaryStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(FocusTheme.colors.surfaceSunken, RoundedCornerShape(16.dp))
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = value, style = FocusTheme.typography.statValue, color = FocusTheme.colors.onSurface)
        Text(text = label, style = FocusTheme.typography.caption, color = FocusTheme.colors.onSurfaceMuted)
    }
}

private fun FocusSession.durationMillis(): Long =
    ((endTimeMillis ?: startTimeMillis) - startTimeMillis).coerceAtLeast(0)

/** "1 h 05 min", "25 min" or "40 s" for very short sessions. */
private fun formatSummaryDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    return when {
        hours > 0 -> "%d h %02d min".format(hours, minutes)
        minutes > 0 -> "$minutes min"
        else -> "$totalSeconds s"
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun SessionSummaryOverlayPreview() {
    FocusAppTheme {
        SessionSummaryOverlay(
            session = FocusSession(
                id = "preview",
                startTimeMillis = 0,
                endTimeMillis = 25 * 60_000L + 13_000,
                distractingAppOpenCount = 3,
                wasCompletedSuccessfully = true,
                groupId = null,
            ),
            onDone = {},
        )
    }
}
