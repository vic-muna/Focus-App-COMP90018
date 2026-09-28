package com.example.focusapp.ui.screens.timefocus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.button.BackButton
import com.example.focusapp.ui.components.button.ConfirmButton
import com.example.focusapp.ui.components.card.CardButtonRow
import com.example.focusapp.ui.components.card.FocusCard
import com.example.focusapp.ui.components.input.FocusTextField

/** "Max Minutes" choices: no limit (null), then 5-minute steps up to 4 hours. */
val LimitMinuteOptions: List<Int?> = listOf<Int?>(null) + (5..240 step 5)

/**
 * Figma: "App Focuse" add-group step 3 (last), reused for a time slot's
 * daily limits (David's usage limits, applied to each app on its own during
 * the slot): [maxOpens] - how many times a day each app may be opened
 * (typed; null = no limit), [maxMinutes] - how long each app may be used a
 * day (a wheel; null = no limit), and the group's [name]. The check saves
 * once a name is entered.
 */
@Composable
fun TimeSlotLimitsCard(
    maxOpens: Int?,
    onMaxOpensChange: (Int?) -> Unit,
    maxMinutes: Int?,
    onMaxMinutesChange: (Int?) -> Unit,
    name: String,
    onNameChange: (String) -> Unit,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

    FocusCard(modifier = modifier) {
        CardButtonRow(
            left = { BackButton(onClick = onBack) },
            right = { ConfirmButton(onClick = onConfirm, contentDescription = "Save time slot", enabled = name.isNotBlank()) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LabeledColumn(title = "Max Open\nTimes") {
                FocusNumberField(value = maxOpens, onValueChange = onMaxOpensChange)
            }
            LabeledColumn(title = "Max\nMinutes") {
                WheelPicker(
                    values = LimitMinuteOptions,
                    selected = maxMinutes,
                    onValueChange = onMaxMinutesChange,
                    label = { it?.let { minutes -> "%02d".format(minutes) } ?: "--" },
                )
            }
        }

        Text(
            text = "Per app, per day. Leave empty or \"--\" for no limit.",
            style = FocusTheme.typography.caption,
            color = colors.onSurfaceMuted,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )

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
private fun TimeSlotLimitsCardPreview() {
    FocusAppTheme {
        TimeSlotLimitsCard(
            maxOpens = 3,
            onMaxOpensChange = {},
            maxMinutes = 30,
            onMaxMinutesChange = {},
            name = "",
            onNameChange = {},
            onBack = {},
            onConfirm = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
