package com.example.focusapp.ui.screens.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

/** Shown when the block didn't come with a reason (e.g. a plain focus session). */
private const val DEFAULT_REASON = "This app is in your Focus restricted list right now."

/**
 * Figma: the interception screen shown when a restricted app is opened -
 * Version-2's layout (warning sign, then the message, pill buttons at the
 * bottom) with David's wording: "[appLabel] is blocked" and why ([reason]).
 * If [maxOpensLeft] is provided, an extra button allows consuming 1 open to unlock
 * the app for a set duration.
 */
@Composable
fun BlockedScreen(
    appLabel: String,
    reason: String?,
    maxOpensLeft: Int? = null,
    onGotItClick: () -> Unit,
    onUseOpenClick: () -> Unit = {},
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            // This screen is its own Activity (no Scaffold), so keep content clear of the system bars here.
            .systemBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(start = 45.dp, end = 45.dp, bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WarningSign()
            Text(
                text = "$appLabel is blocked",
                style = typography.cardTitle,
                color = colors.notification,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 20.dp),
            )
            Text(
                text = reason ?: DEFAULT_REASON,
                style = typography.prompt,
                color = colors.notification,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = FocusSpacing.ScreenBottom),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (maxOpensLeft != null) {
                val hasOpensLeft = maxOpensLeft > 0
                val openButtonLabel = if (hasOpensLeft) {
                    val openWord = if (maxOpensLeft == 1) "1 left today" else "$maxOpensLeft left today"
                    "Use 1 open ($openWord)"
                } else {
                    "0 opens left today"
                }
                FocusPillButton(
                    label = openButtonLabel,
                    containerColor = if (hasOpensLeft) colors.rejection else colors.surfaceSunken,
                    enabled = hasOpensLeft,
                    onClick = onUseOpenClick,
                )
            }

            FocusPillButton(
                label = "Got it",
                containerColor = colors.confirm,
                onClick = onGotItClick,
            )
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun BlockedScreenPreview() {
    FocusAppTheme {
        BlockedScreen(
            appLabel = "Instagram",
            reason = "You've reached your limit of 3 times for Instagram during Study Group's scheduled time today.",
            maxOpensLeft = 3,
            onGotItClick = {},
            onUseOpenClick = {},
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun BlockedScreenDefaultPreview() {
    FocusAppTheme {
        BlockedScreen(appLabel = "Instagram", reason = null, onGotItClick = {})
    }
}
