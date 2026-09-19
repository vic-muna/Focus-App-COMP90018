package com.example.focusapp.ui.screens.apps

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.apps.getAppIcon
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.AppGroup
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * EditAppGroupScreen
 * ---------------------
 * Reached by tapping "Edit" on an [AppsScreen] group row (matches the
 * teammate-provided wireframe "Image 2"). Shows the group's REAL app
 * icons (loaded by [groupId] from [com.example.focusapp.data.local.LocalDataSource]'s
 * persisted storage), then the 3 free-text fields for the restriction
 * schedule ("Block during", "Daily opens", "Open duration"), and a "Save" pill.
 *
 * WHAT'S REAL NOW vs. STILL A PLACEHOLDER:
 *  - The group itself (name + which apps belong to it) is real, persisted
 *    data, loaded fresh by [groupId] every time this screen opens.
 *  - The 3 schedule fields are STILL local, UI-only state - typing works,
 *    but nothing is read from or written to the group. "Save" just
 *    navigates back without persisting them. Adding/removing apps from an
 *    existing group also isn't wired up yet (only creating a brand-new
 *    group, on [AddAppGroupScreen], persists its app list right now).
 *
 * @param groupId the [AppGroup.id] to load, passed in via navigation.
 * @param onSaveClick called when "Save" is tapped (currently just
 *        navigates back - see NavGraph.kt).
 */
@Composable
fun EditAppGroupScreen(
    groupId: String,
    onSaveClick: () -> Unit
) {
    val context = LocalContext.current
    var group by remember { mutableStateOf<AppGroup?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(groupId) {
        group = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getAppGroups().find { it.id == groupId }
        }
        isLoading = false
    }

    // TODO: to be implemented later - persist real values into/out of
    // `group`'s schedule instead of these 3 free-standing fields; there is
    // currently no `blockDuring`/`dailyOpens`/`openDuration` on the
    // AppGroup domain model at all yet.
    var blockDuring by remember { mutableStateOf("") }
    var dailyOpens by remember { mutableStateOf("") }
    var openDuration by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        val displayName = group?.groupName ?: if (isLoading) "…" else "(not found)"
        Text(
            text = "Apps in Group $displayName",
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )

        // Real icons for this specific group's saved packages, instead of
        // 3 generic placeholder squares.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            val packageNames = group?.packageNames.orEmpty()
            if (packageNames.isEmpty()) {
                Text(
                    text = if (isLoading) "Loading…" else "(no apps in this group)",
                    color = WireframeColors.OnDark
                )
            } else {
                // Show at most 3 icons, matching the wireframe's row width,
                // plus a "+N" badge for anything beyond that.
                val shown = packageNames.take(3)
                shown.forEachIndexed { index, packageName ->
                    GroupMemberIcon(packageName = packageName)
                    if (index != shown.lastIndex) {
                        Box(modifier = Modifier.padding(end = 10.dp))
                    }
                }
                val overflowCount = packageNames.size - shown.size
                if (overflowCount > 0) {
                    Text(
                        text = "+$overflowCount",
                        color = WireframeColors.OnDark,
                        modifier = Modifier.padding(start = 10.dp)
                    )
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
        // picker) rather than raw text once real validation is needed, and
        // should read/write onto the AppGroup itself once it has fields
        // for them.

        // Pushes the Save pill to the bottom of the screen regardless of
        // how many fields are above it.
        Spacer(modifier = Modifier.weight(1f))

        SavePillButton(onClick = onSaveClick)
    }
}

/**
 * GroupMemberIcon
 * ------------------
 * One real app icon inside the group-preview pill, resolved by package
 * name via [getAppIcon]. Falls back to the same grey placeholder square
 * used elsewhere if the icon can't be loaded (e.g. the app was
 * uninstalled after being added to this group).
 */
@Composable
private fun GroupMemberIcon(packageName: String) {
    val context = LocalContext.current
    val icon = remember(packageName) { getAppIcon(context, packageName) }
    if (icon != null) {
        Image(
            bitmap = icon.asImageBitmap(),
            contentDescription = packageName,
            modifier = Modifier.size(44.dp)
        )
    } else {
        AppIconPlaceholder()
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
