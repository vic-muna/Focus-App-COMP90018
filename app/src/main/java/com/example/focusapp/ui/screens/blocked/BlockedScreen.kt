package com.example.focusapp.ui.screens.blocked

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.FocusPillButton
import com.example.focusapp.ui.components.WarningSign
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Figma: the interception screen shown when a restricted app is opened
 * during Focus Mode - a warning, "take a rest?" and how many rests are left.
 *
 * The button colors are swapped on purpose (Confirm uses the rejection
 * color, Reject the confirm color) to nudge the user away from taking a
 * break. [restsLeft] / [restsTotal] are null while break data isn't
 * available; the "Left" count is then hidden and Confirm is disabled - as
 * it also is when no rests are left.
 */
@Composable
fun BlockedScreen(
    appLabel: String,
    restsLeft: Int?,
    restsTotal: Int?,
    onTakeRest: () -> Unit,
    onReject: () -> Unit,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val hasRestData = restsLeft != null && restsTotal != null
    val canRest = hasRestData && restsLeft!! > 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .semantics { contentDescription = "$appLabel is blocked during Focus Mode" },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(bottom = 80.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WarningSign()
            Text(
                text = "Do you want to take a rest ?",
                style = typography.prompt,
                color = colors.notification,
                modifier = Modifier.padding(top = 12.dp),
            )
            if (hasRestData) {
                Text(
                    text = "Left",
                    style = typography.counterLabel,
                    color = colors.notification,
                    modifier = Modifier.padding(top = 45.dp),
                )
                Text(
                    text = "$restsLeft/$restsTotal",
                    style = typography.counterValue,
                    color = colors.notification,
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 45.dp, end = 45.dp, bottom = 72.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            FocusPillButton(
                label = "Confirm",
                containerColor = colors.rejection,
                onClick = onTakeRest,
                enabled = canRest,
            )
            FocusPillButton(
                label = "Reject",
                containerColor = colors.confirm,
                onClick = onReject,
            )
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun BlockedScreenPreview() {
    FocusAppTheme {
        BlockedScreen(appLabel = "Instagram", restsLeft = 4, restsTotal = 5, onTakeRest = {}, onReject = {})
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun BlockedScreenNoRestDataPreview() {
    FocusAppTheme {
        BlockedScreen(appLabel = "Instagram", restsLeft = null, restsTotal = null, onTakeRest = {}, onReject = {})
    }
}
