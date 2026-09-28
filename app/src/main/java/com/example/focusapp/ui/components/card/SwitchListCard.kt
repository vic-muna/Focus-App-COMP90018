package com.example.focusapp.ui.components.card

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.input.FocusSwitch
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * One row in a list of groups (a time slot, a Wi-Fi, a location):
 * [content] on the left, an on/off switch on the right.
 * Tap = [onClick]; hold = [onLongClick] (the phone vibrates briefly).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwitchListCard(
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
    spacing: Dp = 6.dp,
    verticalPadding: Dp = 14.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val haptics = LocalHapticFeedback.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(FocusTheme.colors.surface)
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                },
            )
            .padding(horizontal = 12.dp, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(spacing),
            content = content,
        )
        Spacer(Modifier.width(8.dp))
        FocusSwitch(checked = enabled, onCheckedChange = onEnabledChange)
    }
}

@Preview(widthDp = 360)
@Composable
private fun SwitchListCardPreview() {
    FocusAppTheme {
        SwitchListCard(
            enabled = true,
            onEnabledChange = {},
            modifier = Modifier.padding(16.dp),
        ) {
            Text(text = "Name", style = FocusTheme.typography.body, color = FocusTheme.colors.onSurface)
            Text(text = "Details", style = FocusTheme.typography.tileTitle, color = FocusTheme.colors.onSurface)
        }
    }
}
