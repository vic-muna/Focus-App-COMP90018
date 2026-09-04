package com.example.focusapp.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.WireframeColors

/**
 * SelectableApp
 * ---------------
 * A UI-only stand-in for "an app the user could add to a group", used
 * purely to render the search/select list from the wireframe. Once real
 * app data exists (via PackageManager), replace this with a proper
 * domain model instead of a hard-coded local list.
 */
private data class SelectableApp(
    val id: String,
    val name: String,
    val isSelected: Boolean = false
)

/**
 * AddAppGroupScreen
 * --------------------
 * Reached by tapping the "+" pill on [AppsScreen] (matches the
 * teammate-provided wireframe "Image 3"). Lets the user search a list of
 * apps and toggle which ones to include, then tap "Save".
 *
 * NONE of this is wired to real data:
 *  - [apps] is a hard-coded local list ("App Name 1", "App Name 2"), not
 *    the device's actually-installed apps.
 *  - The search field accepts typing but does not filter the list yet.
 *  - The toggle circles flip local UI state only.
 *  - "Save" just navigates back - it does not create a new AppGroup or
 *    call FocusRepository.saveAppGroup(...).
 *
 * @param onSaveClick called when "Save" is tapped.
 */
@Composable
fun AddAppGroupScreen(onSaveClick: () -> Unit) {
    // TODO: to be implemented later - replace with the real installed-app
    // list (PackageManager.getInstalledApplications(...)) instead of this
    // hard-coded placeholder list.
    val apps = remember {
        mutableStateListOf(
            SelectableApp(id = "a1", name = "App Name 1"),
            SelectableApp(id = "a2", name = "App Name 2")
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        SearchBarWithToggle(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        apps.forEachIndexed { index, app ->
            SelectableAppRow(
                app = app,
                onToggle = {
                    // Local, UI-only toggle - not persisted anywhere.
                    apps[index] = app.copy(isSelected = !app.isSelected)
                }
            )
        }

        // TODO: to be implemented later - filter `apps` by `searchQuery`
        // once search is real, and show an empty state when nothing matches.

        Spacer(modifier = Modifier.weight(1f))

        SavePillButton(onClick = onSaveClick)
    }
}

/**
 * SearchBarWithToggle
 * ----------------------
 * The search field + circular toggle shown at the top of the wireframe.
 * The circle's exact purpose wasn't specified by the design (select-all?
 * a filter?) so it is rendered but does nothing on tap yet.
 *
 * TODO: to be implemented later - confirm the toggle circle's intended
 * behaviour with the design owner, then wire it up.
 */
@Composable
private fun SearchBarWithToggle(
    query: String,
    onQueryChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchInputField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(WireframeColors.IconPlaceholder)
        )
    }
}

/**
 * SearchInputField
 * -------------------
 * The rounded search input itself, using [BasicTextField] (kept unstyled
 * beyond the pill background) so it matches the flat, greyscale mockup
 * rather than a default Material text field's underline/outline.
 */
@Composable
private fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        textStyle = TextStyle(color = WireframeColors.OnLight),
        cursorBrush = SolidColor(WireframeColors.OnLight),
        modifier = modifier
            .background(WireframeColors.IconPlaceholder, shape = RoundedCornerShape(50))
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

/**
 * SelectableAppRow
 * -------------------
 * One row in the app-picker list: icon placeholder + name on the left,
 * toggle circle on the right. Tapping the circle flips [app]'s selected
 * state in the caller's local list only - no persistence.
 */
@Composable
private fun SelectableAppRow(app: SelectableApp, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconPlaceholder()
        Text(
            text = app.name,
            color = WireframeColors.OnLight,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        // Toggle "radio" circle - filled when selected, plain grey
        // otherwise. Purely local UI state, see class doc above.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (app.isSelected) WireframeColors.Card else WireframeColors.IconPlaceholder
                )
                .clickable(onClick = onToggle)
        )
    }
}
