package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Figma: "Effective Range" slider - a sunken track with a light thumb. */
@Composable
fun FocusSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    modifier: Modifier = Modifier,
    steps: Int = 0,
) {
    val colors = FocusTheme.colors
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        modifier = modifier,
        colors = SliderDefaults.colors(
            thumbColor = colors.surfaceSelected,
            activeTrackColor = colors.surfaceSunken,
            inactiveTrackColor = colors.surfaceSunken,
            activeTickColor = colors.surfaceSunken,
            inactiveTickColor = colors.surfaceSunken,
        ),
    )
}

@Preview
@Composable
private fun FocusSliderPreview() {
    FocusAppTheme {
        FocusSlider(
            value = 100f,
            onValueChange = {},
            valueRange = 50f..500f,
            modifier = Modifier
                .width(280.dp)
                .background(FocusTheme.colors.surface),
        )
    }
}
