package com.example.focusapp.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Light-blue tint shared with the Auto Blocking sheet's own palette. */
private val CircleCheckboxColor = Color(0xFF90CAF9)

/**
 * A round checkbox (outline ring, filled center dot when checked) instead
 * of Material's default square Checkbox - used anywhere the Auto Blocking
 * flow needs a checkable row (app picker, day picker), to match that
 * sheet's light-blue, rounded visual style.
 */
@Composable
fun CircleCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .border(2.dp, CircleCheckboxColor, CircleShape)
            .clickable { onCheckedChange(!checked) },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(CircleCheckboxColor)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CircleCheckboxPreview() {
    Row {
        CircleCheckbox(checked = false, onCheckedChange = {})
        CircleCheckbox(checked = true, onCheckedChange = {})
    }
}
