package com.example.focusapp.ui.components.card

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Same footprint as a list card (Figma time-slot / Wi-Fi cards). */
private val AddCardHeight = 84.dp

/**
 * Figma: the last card in a list - a muted circle with a plus, for adding
 * another item. [onClickLabel] is what screen readers announce, e.g.
 * "Add time slot".
 */
@Composable
fun AddItemCard(
    onClick: () -> Unit,
    onClickLabel: String,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(AddCardHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = onClickLabel, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colors.onSurfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(20.dp)) {
                val stroke = 3.dp.toPx()
                val mid = size.width / 2
                drawLine(colors.surface, Offset(mid, 0f), Offset(mid, size.height), stroke, StrokeCap.Round)
                drawLine(colors.surface, Offset(0f, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun AddItemCardPreview() {
    FocusAppTheme {
        AddItemCard(
            onClick = {},
            onClickLabel = "Add item",
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        )
    }
}