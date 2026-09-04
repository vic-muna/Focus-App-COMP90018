package com.example.focusapp.ui.screens.apps

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.WireframeColors

/**
 * UiAppGroup
 * ------------
 * Purely a UI-layer stand-in for [com.example.focusapp.domain.model.AppGroup]
 * while there is no real data source wired up yet. Once
 * FocusRepository.getAppGroups() does something real, replace this with
 * the actual domain model (or a small mapper between the two).
 */
private data class UiAppGroup(val id: String, val name: String, val appIconCount: Int = 3)

/**
 * AppsScreen
 * ------------
 * Main / landing page of the app (matches the teammate-provided wireframe
 * "Image 1"). Lists the user's app groups, each as a dark pill showing a
 * row of app-icon placeholders and an "Edit" link, plus a big "+" pill at
 * the bottom to add a new group.
 *
 * No real app-group data is loaded or saved here - [groups] starts out
 * with one hard-coded entry just so the screen isn't empty, matching the
 * mockup. Tapping "Edit" or "+" only navigates; it does not read or write
 * any real AppGroup yet.
 *
 * @param onEditGroupClick called with the tapped group's display name,
 *        so the Edit screen can show "Apps in Group <name>".
 * @param onAddGroupClick called when the "+" pill is tapped.
 */
@Composable
fun AppsScreen(
    onEditGroupClick: (String) -> Unit,
    onAddGroupClick: () -> Unit
) {
    // TODO: to be implemented later - replace this with real data from
    // FocusRepository.getAppGroups() (via a ViewModel), instead of a
    // hard-coded local list.
    val groups = remember { mutableStateListOf(UiAppGroup(id = "g1", name = "App Group 1")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        groups.forEach { group ->
            AppGroupRow(group = group, onEditClick = { onEditGroupClick(group.name) })
            Box(modifier = Modifier.height(24.dp)) // simple vertical spacer between cards
        }

        AddPillButton(onClick = onAddGroupClick)

        // TODO: to be implemented later - empty-state message when there
        // are no app groups yet, once `groups` is no longer hard-coded.
    }
}

/**
 * AppGroupRow
 * -------------
 * One row in the group list: the group's name above a dark pill
 * containing up to 3 app-icon placeholders and an "Edit" text button.
 *
 * The 3 grey squares are NOT real app icons - there is no logic yet for
 * picking which apps belong to a group or fetching their real icons
 * (see domain/model/AppGroup.kt).
 */
@Composable
private fun AppGroupRow(group: UiAppGroup, onEditClick: () -> Unit) {
    Column {
        Text(
            text = group.name,
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(group.appIconCount) { index ->
                AppIconPlaceholder()
                if (index != group.appIconCount - 1) {
                    Box(modifier = Modifier.padding(end = 10.dp))
                }
            }
            // weight(1f) (a RowScope-only modifier) makes this Box consume
            // exactly the remaining horizontal space after the icons, so
            // "Edit" lands at the far right without overflowing the pill.
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "Edit",
                    color = WireframeColors.OnDark,
                    modifier = Modifier.clickable(onClick = onEditClick)
                )
            }
        }
    }
}

/**
 * AppIconPlaceholder
 * ---------------------
 * A plain light-grey rounded square standing in for a real app icon.
 * TODO: to be implemented later - replace with the app's real launcher
 * icon (via PackageManager) once app selection is implemented.
 */
// Not `private`: reused as-is from EditAppGroupScreen.kt and
// AddAppGroupScreen.kt (both in this same `ui.screens.apps` package) so
// every screen in the Apps flow shows an identical icon placeholder.
@Composable
fun AppIconPlaceholder() {
    Box(
        modifier = Modifier
            .size(44.dp)
            .background(WireframeColors.IconPlaceholder, shape = RoundedCornerShape(10.dp))
    )
}

/**
 * AddPillButton
 * ---------------
 * The big dark "+" pill used at the bottom of the Apps screen. Purely
 * navigational - creating a real new AppGroup happens (or rather,
 * doesn't happen yet) on [AddAppGroupScreen], which this navigates to.
 *
 * The Map screen uses its own near-identical
 * `ui.screens.map.AddLocationPillButton` rather than this one, since the
 * two "+" flows aren't coupled to each other - see that file's doc
 * comment for why it wasn't shared instead.
 */
@Composable
fun AddPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WireframeColors.CardLight, shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add", tint = WireframeColors.OnDark)
        }
    }
}
