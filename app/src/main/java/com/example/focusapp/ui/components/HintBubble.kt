package com.example.focusapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val TailWidth = 10.dp
private val TailHeight = 16.dp

/**
 * Figma: the pill-shaped tooltip next to Focus Mode's "i" button, with a
 * small tail on its end side pointing at whatever it explains - place it
 * directly to the start side of that element. [width] x [height] is the
 * whole bubble, tail included (Figma: 260 x 42).
 */
@Composable
fun HintBubble(
    text: String,
    modifier: Modifier = Modifier,
    width: Dp = 260.dp,
    height: Dp = 42.dp,
    containerColor: Color = FocusTheme.colors.primaryAction,
    contentColor: Color = FocusTheme.colors.onPrimaryAction,
) {
    Row(
        modifier = modifier.size(width = width, height = height),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(containerColor, CircleShape)
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = FocusTheme.typography.hint,
                color = contentColor,
                textAlign = TextAlign.Center,
            )
        }
        // Tail: a small triangle pointing toward the end side.
        Canvas(modifier = Modifier.size(width = TailWidth, height = TailHeight)) {
            val tail = Path().apply {
                moveTo(0f, 0f)
                lineTo(size.width, size.height / 2)
                lineTo(0f, size.height)
                close()
            }
            drawPath(tail, containerColor)
        }
    }
}

@Preview
@Composable
private fun HintBubblePreview() {
    FocusAppTheme {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(FocusTheme.colors.surface)
                .padding(12.dp),
        ) {
            HintBubble(text = "Hold for 5 seconds to exit\nthe focus mode")
            InfoButton(onClick = {}, modifier = Modifier.padding(start = 4.dp))
        }
    }
}
