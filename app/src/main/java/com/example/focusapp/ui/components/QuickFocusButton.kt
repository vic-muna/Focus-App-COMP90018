package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusBlobShape
import com.example.focusapp.ui.theme.FocusTheme

/** Opacity of the outer blob in Figma (layer 0.31 x fill 0.6). */
private const val OUTER_BLOB_ALPHA = 0.31f * 0.6f

/** Inner blob rotation in Figma, so its corners sit between the outer blob's. */
private const val INNER_BLOB_ROTATION = 39.16f

/**
 * Figma: "Focuse Buttom" - two stacked blobs (a faint outer halo and a
 * solid, rotated inner one) with a centered label.
 */
@Composable
fun QuickFocusButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Quick\nFocus",
) {
    val colors = FocusTheme.colors

    Box(
        modifier = modifier
            .size(175.dp)
            .clip(FocusBlobShape)
            .background(colors.primaryAction.copy(alpha = OUTER_BLOB_ALPHA))
            .clickable(role = Role.Button, onClick = onClick),
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
            QuickFocusButton(onClick = {})
        }
    }
}
