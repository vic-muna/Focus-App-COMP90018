package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Figma: the dark square you type a small count into ("Unfrozen Times").
 * Digits only, at most [maxDigits]; [value] is null while it's empty.
 */
@Composable
fun FocusNumberField(
    value: Int?,
    onValueChange: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    boxSize: Dp = 96.dp,
    maxDigits: Int = 2,
) {
    val colors = FocusTheme.colors
    val textStyle = FocusTheme.typography.timer.copy(color = colors.onSurface, textAlign = TextAlign.Center)

    BasicTextField(
        value = value?.toString().orEmpty(),
        onValueChange = { text ->
            val digits = text.filter { it.isDigit() }.take(maxDigits)
            onValueChange(digits.toIntOrNull())
        },
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.accent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        modifier = modifier.size(boxSize),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(colors.surfaceSunken, RoundedCornerShape(12.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { innerTextField() }
            }
        },
    )
}

@Preview
@Composable
private fun FocusNumberFieldPreview() {
    FocusAppTheme {
        Box(Modifier.background(FocusTheme.colors.surface).padding(16.dp)) {
            FocusNumberField(value = 3, onValueChange = {})
        }
    }
}
