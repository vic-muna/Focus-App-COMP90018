package com.example.focusapp.ui.screens.apps

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.data.apps.getLaunchableApps
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.ui.theme.WireframeColors
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * AddAppGroupScreen
 * --------------------
 * Reached by tapping the "+" pill on [AppsScreen] (matches the
 * teammate-provided wireframe "Image 3", plus one addition the wireframe
 * didn't show: a group-name field, added because a group needs SOME name
 * to be selectable later - "Group1 with Facebook+Instagram" needs a name
 * to actually say "Group1").
 *
 * WHAT'S REAL NOW vs. STILL A PLACEHOLDER:
 *  - [installedApps] loads the device's real launchable apps via
 *    [getLaunchableApps] (off the main thread) - no longer a hard-coded list.
 *  - The search field genuinely filters the list now (by label, case-insensitive).
 *  - The toggle circles flip local selection state - still local, but now
 *    that local state is exactly what gets saved (see below).
 *  - Tapping "Save" with a non-blank name and at least one app selected
 *    genuinely builds a new [AppGroup] and calls
 *    `FocusRepository.saveAppGroup(...)`, persisted for real via
 *    [com.example.focusapp.data.local.LocalDataSource]'s SharedPreferences
 *    storage - this survives an app restart.
 *  - The circular icon next to the search bar is still unexplained/inert
 *    (see the earlier TODO - no wireframe ever clarified its purpose).
 *
 * @param onSaveClick called after a successful save, to navigate back.
 */
@Composable
fun AddAppGroupScreen(onSaveClick: () -> Unit) {
    val context = LocalContext.current
    // Tied to this composable's lifecycle - Compose cancels anything
    // launched on it automatically if the screen is navigated away from
    // before the save finishes, so this can't leak or crash on a
    // now-gone screen.
    val coroutineScope = rememberCoroutineScope()

    var groupNameText by remember { mutableStateOf("") }
    var searchQuery by remember { mutableStateOf("") }
    var isLoadingApps by remember { mutableStateOf(true) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    // Which package names are currently checked - a plain Set, since all
    // we need is fast "is this one selected?" membership checks per row.
    var selectedPackageNames by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Loads the real installed-app list once, off the main thread - the
    // same pattern used by AppsScreen's AppPickerDialog.
    LaunchedEffect(Unit) {
        installedApps = withContext(Dispatchers.Default) { getLaunchableApps(context) }
        isLoadingApps = false
    }

    val visibleApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter { it.label.contains(searchQuery, ignoreCase = true) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        GroupNameField(value = groupNameText, onValueChange = { groupNameText = it })

        SearchBarWithToggle(
            query = searchQuery,
            onQueryChange = { searchQuery = it }
        )

        Text(
            text = "Give the group a name and select at least one app before saving.",
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        when {
            isLoadingApps -> Text(text = "Loading installed apps…", color = WireframeColors.OnLight)
            visibleApps.isEmpty() -> Text(text = "No apps match \"$searchQuery\".", color = WireframeColors.OnLight)
            else -> visibleApps.forEach { app ->
                SelectableAppRow(
                    app = app,
                    isSelected = app.packageName in selectedPackageNames,
                    onToggle = {
                        selectedPackageNames = if (app.packageName in selectedPackageNames) {
                            selectedPackageNames - app.packageName
                        } else {
                            selectedPackageNames + app.packageName
                        }
                    }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        SavePillButton(onClick = {
            val name = groupNameText.trim()
            if (name.isEmpty() || selectedPackageNames.isEmpty()) {
                // Same "no real effect" behaviour as everywhere else in
                // this screen family when input isn't ready yet - we just
                // don't save, and stay on the screen (the hint text above
                // already explains what's missing).
                return@SavePillButton
            }

            val newGroup = AppGroup(
                id = UUID.randomUUID().toString(),
                groupName = name,
                packageNames = selectedPackageNames.toList()
            )

            // onClick handlers are plain functions, not suspend functions,
            // so saving (a suspend call) has to happen inside a coroutine
            // we explicitly launch - `coroutineScope`, from
            // rememberCoroutineScope() above, is exactly that.
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    FocusRepositoryProvider.get(context).saveAppGroup(newGroup)
                }
                onSaveClick()
            }
        })
    }
}

/**
 * GroupNameField
 * ----------------
 * The "name this group" text field - the one addition to this screen
 * beyond what the original wireframe showed (see [AddAppGroupScreen]'s
 * doc comment for why it was necessary).
 */
@Composable
private fun GroupNameField(value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = "Group name:",
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
            .padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SearchInputField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.padding(start = 12.dp))
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
 * One row in the real app-picker list: the app's actual icon (falling
 * back to the grey placeholder square if it failed to load) + real name
 * on the left, toggle circle on the right.
 */
@Composable
private fun SelectableAppRow(app: InstalledAppInfo, isSelected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (app.icon != null) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.label,
                modifier = Modifier.size(40.dp)
            )
        } else {
            AppIconPlaceholder()
        }
        Text(
            text = app.label,
            color = WireframeColors.OnLight,
            modifier = Modifier
                .weight(1f)
                .padding(start = 14.dp)
        )
        // Toggle "radio" circle - filled when selected, plain grey
        // otherwise. Selection lives in AddAppGroupScreen's
        // `selectedPackageNames` set, which IS what gets saved.
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(if (isSelected) WireframeColors.Card else WireframeColors.IconPlaceholder)
                .clickable(onClick = onToggle)
        )
    }
}
