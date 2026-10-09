package com.example.focusapp.ui.components.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Fly card opened by tapping a locked background or music track ([itemName]) in its picker:
 * the user's [points], the [pricePoints] it costs (in red), and a tick that buys it
 * ([onConfirm]) - disabled while there aren't enough points. [preview] (e.g. a music
 * preview player) shows above the title.
 */
@Composable
fun UnlockCard(
    itemName: String,
    points: Long,
    pricePoints: Int,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    preview: (@Composable () -> Unit)? = null,
) {
    val colors = FocusTheme.colors
    val canAfford = points >= pricePoints

    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose) },
            right = { ConfirmButton(onClick = onConfirm, contentDescription = "Unlock", enabled = canAfford) },
        )
        preview?.invoke()
        CardTitle("Unlock \"$itemName\"")
        UnlockRow(label = "Your points", value = "$points pts", valueColor = colors.onSurface)
        UnlockRow(label = "Cost", value = "-$pricePoints pts", valueColor = colors.rejection)
        if (!canAfford) {
            Text(
                text = "Not enough points yet - every 10 minutes of focus earns 1 point.",
                style = FocusTheme.typography.caption,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

@Composable
private fun UnlockRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = FocusTheme.typography.body, color = FocusTheme.colors.onSurface)
        Text(text = value, style = FocusTheme.typography.body, color = valueColor)
    }
}

@Preview(widthDp = 360)
@Composable
private fun UnlockCardPreview() {
    FocusAppTheme {
        UnlockCard(
            itemName = "Ocean",
            points = 42,
            pricePoints = 1,
            onConfirm = {},
            onClose = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
