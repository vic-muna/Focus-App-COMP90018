package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Figma: the sunken pill input ("Enter Group Name"), single line, centered text. */
@Composable
fun FocusTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val textStyle = FocusTheme.typography.inputLarge.copy(
        color = colors.onSurface,
        textAlign = TextAlign.Center,
    )

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.accent),
        modifier = modifier
            .fillMaxWidth()
            .height(42.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .background(colors.surfaceSunken, CircleShape)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = textStyle.copy(color = colors.onSurfaceMuted),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                innerTextField()
            }
        },
    )
}

@Preview
@Composable
private fun FocusTextFieldPreview() {
    FocusAppTheme {
        Column(
            modifier = Modifier
                .width(280.dp)
                .background(FocusTheme.colors.surface)
                .padding(8.dp),
        ) {
            FocusTextField(value = "", onValueChange = {}, placeholder = "Enter Group Name")
            FocusTextField(value = "Library", onValueChange = {}, placeholder = "Enter Group Name", modifier = Modifier.padding(top = 8.dp))
        }
    }
}
