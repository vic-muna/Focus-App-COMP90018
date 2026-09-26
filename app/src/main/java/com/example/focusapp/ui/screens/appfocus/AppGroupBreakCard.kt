package com.example.focusapp.ui.screens.appfocus

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.BackButton
import com.example.focusapp.ui.components.ConfirmButton
import com.example.focusapp.ui.components.FocusNumberField
import com.example.focusapp.ui.components.FocusTextField
import com.example.focusapp.ui.components.WheelPicker
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** "Relaxing minutes" choices: 5-minute steps up to an hour. */
val BreakMinuteOptions = (5..60 step 5).toList()

/**
 * Figma: "App Focuse" add-group step 3 (last) - how many breaks a session
 * allows ([breakAllowance], typed; null while empty), how long each is
 * ([breakMinutes], a wheel), and the group's [name]. The check finishes
 * creating the group once a name is entered.
 */
@Composable
fun AppGroupBreakCard(
    breakAllowance: Int?,
    onBreakAllowanceChange: (Int?) -> Unit,
    breakMinutes: Int,
    onBreakMinutesChange: (Int) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            BackButton(onClick = onBack)
            ConfirmButton(
                onClick = onConfirm,
                contentDescription = "Create group",
                enabled = name.isNotBlank(),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LabeledColumn(title = "Unfrozen\nTimes") {
                FocusNumberField(value = breakAllowance, onValueChange = onBreakAllowanceChange)
            }
            LabeledColumn(title = "Relaxing\nminutes") {
                WheelPicker(
                    values = BreakMinuteOptions,
                    selected = breakMinutes,
                    onValueChange = onBreakMinutesChange,
                    label = { "%02d".format(it) },
                )
            }
        }

        FocusTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Enter Group Name",
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

@Composable
private fun LabeledColumn(title: String, content: @Composable () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = title,
            style = FocusTheme.typography.tileTitle,
            color = FocusTheme.colors.onSurface,
            textAlign = TextAlign.Center,
        )
        content()
    }
}

@Preview(widthDp = 360)
@Composable
private fun AppGroupBreakCardPreview() {
    FocusAppTheme {
        AppGroupBreakCard(
            breakAllowance = null,
            onBreakAllowanceChange = {},
            breakMinutes = 10,
            onBreakMinutesChange = {},
            name = "",
            onNameChange = {},
            onBack = {},
            onConfirm = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
