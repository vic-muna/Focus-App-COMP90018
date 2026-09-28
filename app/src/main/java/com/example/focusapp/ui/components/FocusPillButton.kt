package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Opacity of a disabled [FocusPillButton]. */
private const val DISABLED_ALPHA = 0.4f

/**
 * A filled pill with a short label (Figma blocking screen's Confirm / Reject).
 * At least [width] x [height]; grows wider if the label needs it.
 */
@Composable
fun FocusPillButton(
    label: String,
    containerColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = FocusTheme.colors.pure,
    enabled: Boolean = true,
    width: Dp = 84.dp,
    height: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(CircleShape)
            .background(containerColor)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minWidth = width, minHeight = height)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, style = FocusTheme.typography.confirmation, color = contentColor)
    }
}

@Preview
@Composable
private fun FocusPillButtonPreview() {
    FocusAppTheme {
        val colors = FocusTheme.colors
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(colors.background)
                .padding(12.dp),
        ) {
            FocusPillButton(label = "Confirm", containerColor = colors.rejection, onClick = {})
            FocusPillButton(label = "Reject", containerColor = colors.confirm, onClick = {})
            FocusPillButton(label = "Confirm", containerColor = colors.rejection, onClick = {}, enabled = false)
        }
    }
}
