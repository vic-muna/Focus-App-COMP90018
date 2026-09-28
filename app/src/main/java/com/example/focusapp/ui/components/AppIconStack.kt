package com.example.focusapp.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * A row of overlapping app icons previewing which apps are picked - the
 * first [maxVisible], then a "+N" tile for the rest. A null icon (not
 * resolved) shows as a plain tile. Each tile gets a [ringColor] outline so
 * overlapping icons stay apart; match it to whatever is behind the row.
 */
@Composable
fun AppIconStack(
    icons: List<Bitmap?>,
    modifier: Modifier = Modifier,
    iconSize: Dp = 28.dp,
    maxVisible: Int = 4,
    ringColor: Color = FocusTheme.colors.surfaceSunken,
) {
    val colors = FocusTheme.colors
    val shape = RoundedCornerShape(iconSize / 4)
    val tile = Modifier
        .size(iconSize)
        .clip(shape)
        .border(2.dp, ringColor, shape)
    val extra = icons.size - maxVisible

    Row(
        modifier = modifier,
        // Negative spacing overlaps each icon onto the previous one.
        horizontalArrangement = Arrangement.spacedBy(-iconSize / 4),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icons.take(maxVisible).forEach { icon ->
            if (icon != null) {
                Box(modifier = tile.background(ringColor)) {
                    Image(
                        bitmap = icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(iconSize),
                    )
                }
            } else {
                Box(modifier = tile.background(colors.surface))
            }
        }
        if (extra > 0) {
            Box(
                modifier = tile.background(colors.surface),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "+$extra", style = FocusTheme.typography.listLabel, color = colors.onSurface)
            }
        }
    }
}

@Preview
@Composable
private fun AppIconStackPreview() {
    FocusAppTheme {
        AppIconStack(
            icons = List(6) { null },
            modifier = Modifier
                .background(FocusTheme.colors.surfaceSunken)
                .size(width = 160.dp, height = 48.dp),
        )
    }
}