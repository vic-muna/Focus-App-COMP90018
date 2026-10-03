package com.example.focusapp.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.preferences.CoachFollowUp
import com.example.focusapp.data.preferences.FocusCoachStorage
import com.example.focusapp.ui.common.ErrorBanner
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Focus Coach's popup, opened from the dashboard's bottom-right button. Laid out like the week
 * detail dialog: title and X, then a scrollable body that depends on [FocusCoachState.stage].
 */
@Composable
fun FocusCoachDialogContent(
    state: FocusCoachState,
    onAcceptConsent: () -> Unit,
    onRequestReport: () -> Unit,
    onAsk: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Focus Coach", style = typography.cardTitle, color = colors.onSurface)
            RejectButton(onClick = onDismiss, contentDescription = "Close Focus Coach")
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${state.requestsLeft} of ${FocusCoachStorage.DAILY_LIMIT} AI answers left today",
            style = typography.caption,
            color = colors.onSurfaceMuted
        )
        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .heightIn(max = 480.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (state.stage) {
                CoachStage.LOADING -> Thinking(label = null)
                CoachStage.NO_SESSIONS -> BodyText(
                    "No focus sessions in the last 7 days yet. Finish a session, then come back for feedback."
                )
                CoachStage.CONSENT -> {
                    BodyText(
                        "Focus Coach sends a summary of your last 7 days of focus - times, totals, distracting-app " +
                            "attempts and your streak - to Google's Gemini AI to write your feedback. " +
                            "Your name and account aren't sent."
                    )
                    FocusPillButton(label = "Continue", containerColor = colors.accent, onClick = onAcceptConsent)
                }
                CoachStage.READY -> ReadyBody(state, onRequestReport, onAsk)
            }

            state.error?.let { ErrorBanner(message = it) }
            if (state.requestsLeft == 0 && !state.isThinking) {
                Text(
                    text = "You've used today's ${FocusCoachStorage.DAILY_LIMIT} answers. Come back tomorrow.",
                    style = typography.caption,
                    color = colors.onSurfaceMuted
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(text = "AI-generated - it can get things wrong.", style = typography.caption, color = colors.onSurfaceMuted)
    }
}

@Composable
private fun ReadyBody(state: FocusCoachState, onRequestReport: () -> Unit, onAsk: (String) -> Unit) {
    val report = state.report
    if (report == null) {
        BodyText("Get feedback and suggestions on your last 7 days of focus.")
        if (state.isThinking) {
            Thinking(label = "Looking at your week…")
        } else {
            FocusPillButton(
                label = "Get my feedback",
                containerColor = FocusTheme.colors.accent,
                onClick = onRequestReport,
                enabled = state.canAsk,
            )
        }
        return
    }

    AnswerCard(report)
    state.followUps.forEach { followUp ->
        Text(text = followUp.question, style = FocusTheme.typography.listLabel, color = FocusTheme.colors.accent)
        AnswerCard(followUp.answer)
    }
    if (state.isThinking) {
        Thinking(label = "Thinking…")
    } else if (state.questionsLeft.isNotEmpty()) {
        Text(text = "Ask a follow-up", style = FocusTheme.typography.listLabel, color = FocusTheme.colors.onSurface)
        state.questionsLeft.forEach { question ->
            QuestionChip(question = question, enabled = state.canAsk, onClick = { onAsk(question) })
        }
    }
}

/** One of Focus Coach's answers, on a surface card like the dashboard's. */
@Composable
private fun AnswerCard(text: String) {
    Text(
        text = text,
        style = FocusTheme.typography.body,
        color = FocusTheme.colors.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FocusTheme.colors.surface)
            .padding(16.dp)
    )
}

/** A preset follow-up question - tap to ask it. */
@Composable
private fun QuestionChip(question: String, enabled: Boolean, onClick: () -> Unit) {
    Text(
        text = question,
        style = FocusTheme.typography.body,
        color = if (enabled) FocusTheme.colors.onSurface else FocusTheme.colors.onSurfaceMuted,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FocusTheme.colors.surfaceSelected.copy(alpha = 0.25f))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun Thinking(label: String?) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(color = FocusTheme.colors.accent, strokeWidth = 3.dp, modifier = Modifier.size(24.dp))
        if (label != null) {
            Spacer(Modifier.width(12.dp))
            Text(text = label, style = FocusTheme.typography.body, color = FocusTheme.colors.onSurfaceMuted)
        }
    }
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = FocusTheme.typography.body, color = FocusTheme.colors.onSurface)
}

@Preview(widthDp = 360)
@Composable
private fun FocusCoachDialogPreview() {
    FocusAppTheme {
        Box(Modifier.padding(16.dp)) {
            FocusCoachDialogContent(
                state = FocusCoachState(
                    stage = CoachStage.READY,
                    report = "Nice work this week - 6h 20m of focus, up from 4h.\n" +
                        "- Most of your focus was in the evening; try one short morning session too.\n" +
                        "- Distracting-app attempts dropped to 2.1 per hour - keep your phone face down.",
                    followUps = listOf(CoachFollowUp("How do I keep my streak going?", "Aim for two 1-hour sessions a day.")),
                    requestsLeft = 3,
                ),
                onAcceptConsent = {},
                onRequestReport = {},
                onAsk = {},
                onDismiss = {},
            )
        }
    }
}
