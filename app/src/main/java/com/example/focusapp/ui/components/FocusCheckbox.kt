package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val CheckboxShape = RoundedCornerShape(4.dp)

/**
 * Figma: the square checkbox in the app picker - an outlined box that fills
 * with the check glyph when selected.
 */
@Composable
fun FocusCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = modifier
            .size(size)
            .clip(CheckboxShape)
            .then(
                if (checked) Modifier.background(colors.onSurface)
                else Modifier.border(1.5.dp, colors.onSurface, CheckboxShape)
            )
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onCheckedChange),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                imageVector = FocusGlyphs.Check,
                contentDescription = null,
                tint = colors.surfaceSunken,
                // The check glyph sits inside a circle-sized viewport - scale it up to fill the box.
                modifier = Modifier.size(size * 1.6f),
            )
        }
    }
}

@Preview
@Composable
private fun FocusCheckboxPreview() {
    FocusAppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .background(FocusTheme.colors.surfaceSunken)
                .padding(12.dp),
        ) {
            FocusCheckbox(checked = false, onCheckedChange = {})
            FocusCheckbox(checked = true, onCheckedChange = {})
        }
    }
}
