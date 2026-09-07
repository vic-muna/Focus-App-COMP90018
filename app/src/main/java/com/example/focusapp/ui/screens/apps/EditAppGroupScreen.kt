package com.example.focusapp.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.WireframeColors

/**
 * EditAppGroupScreen
 * ---------------------
 * Reached by tapping "Edit" on an [AppsScreen] group row (matches the
 * teammate-provided wireframe "Image 2"). Shows the group's app icons
 * again, then 3 free-text fields for the restriction schedule
 * ("Block during", "Daily opens", "Open duration"), and a "Save" pill.
 *
 * NONE of this is wired to real data:
 *  - The 3 fields are local, UI-only state (typing works, but nothing is
 *    read from or written to AppGroup / FocusRepository).
 *  - "Save" just navigates back - it does not persist anything.
 *  - The schedule fields are free text for now; a real implementation will
 *    likely want time pickers / number pickers instead of raw text.
 *
 * @param groupName display name shown as "Apps in Group <groupName>",
 *        passed in from [AppsScreen] via navigation. Defaults to the
 *        literal "X" placeholder used in the wireframe if none is supplied.
 * @param onSaveClick called when "Save" is tapped (expected to just
 *        navigate back for now - see NavGraph.kt).
 */
@Composable
fun EditAppGroupScreen(
    groupName: String = "X",
    onSaveClick: () -> Unit
) {
    // TODO: to be implemented later - load the real AppGroup (by id) from
    // FocusRepository instead of just receiving a display name, and load
    // its actual saved schedule values into these fields.
    var blockDuring by remember { mutableStateOf("") }
    var dailyOpens by remember { mutableStateOf("") }
    var openDuration by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        Text(
            text = "Apps in Group $groupName",
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        // Same 3-icon-placeholder pill used on AppsScreen, but without the
        // "Edit" link since we're already on the edit screen.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            repeat(3) { index ->
                AppIconPlaceholder()
                if (index != 2) {
                    Box(modifier = Modifier.padding(end = 10.dp))
                }
            }
        }

        LabeledTextField(
            label = "Block during:",
            value = blockDuring,
            onValueChange = { blockDuring = it },
            topPadding = 28.dp
        )
        LabeledTextField(
            label = "Daily opens:",
            value = dailyOpens,
            onValueChange = { dailyOpens = it },
            topPadding = 20.dp
        )
        LabeledTextField(
            label = "Open duration:",
            value = openDuration,
            onValueChange = { openDuration = it },
            topPadding = 20.dp
        )

        // TODO: to be implemented later - these 3 fields should probably
        // become structured pickers (time range, number stepper, duration
        // picker) rather than raw text once real validation is needed.

        // Pushes the Save pill to the bottom of the screen regardless of
        // how many fields are above it.
        Spacer(modifier = Modifier.weight(1f))

        SavePillButton(onClick = onSaveClick)
    }
}

/**
 * LabeledTextField
 * -------------------
 * One "label above a dark rounded input box" field, matching the wireframe.
 * Uses a plain [BasicTextField] (no Material outline/underline) so the
 * dark pill background from the mockup shows through unmodified.
 */
@Composable
private fun LabeledTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    topPadding: Dp
) {
    Column(modifier = Modifier.padding(top = topPadding)) {
        Text(
            text = label,
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = WireframeColors.OnDark),
            cursorBrush = SolidColor(WireframeColors.OnDark),
            modifier = Modifier
                .fillMaxWidth()
                .background(WireframeColors.CardLight, shape = RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 14.dp)
        )
    }
}

/**
 * SavePillButton
 * -----------------
 * The full-width dark "Save" pill used at the bottom of both the Edit and
 * Add app-group screens. Purely navigational for now - see the doc
 * comment on the screens that use it for what "Save" actually does (or
 * rather, doesn't do) yet.
 */
@Composable
fun SavePillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Text(text = "Save", color = WireframeColors.OnDark)
    }
}
