package com.example.focusapp.ui.screens.history

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.CardLabel
import com.example.focusapp.ui.components.card.CardTitle
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Fly card for testing: type the unlock code to open a locked background
 * ([themeName]) without the streak. Opened by tapping a locked tile
 * three times in the theme picker. [showError] is true after a wrong code.
 */
@Composable
fun ThemeUnlockCodeCard(
    themeName: String,
    code: String,
    onCodeChange: (String) -> Unit,
    showError: Boolean,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { RejectButton(onClick = onClose) },
            right = { ConfirmButton(onClick = onConfirm, contentDescription = "Unlock", enabled = code.isNotBlank()) },
        )
        CardTitle("Unlock \"$themeName\"")
        CardLabel("Unlock code")
        FocusTextField(value = code, onValueChange = onCodeChange, placeholder = "Enter code")
        if (showError) {
            Text(
                text = "That code isn't right.",
                style = FocusTheme.typography.caption,
                color = FocusTheme.colors.rejection,
                modifier = Modifier.padding(start = 4.dp, top = 8.dp),
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun ThemeUnlockCodeCardPreview() {
    FocusAppTheme {
        ThemeUnlockCodeCard(
            themeName = "Valley",
            code = "abc",
            onCodeChange = {},
            showError = true,
            onConfirm = {},
            onClose = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
