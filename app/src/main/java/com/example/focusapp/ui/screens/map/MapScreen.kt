package com.example.focusapp.ui.screens.map

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.WireframeColors

/**
 * UiFocusLocation
 * -----------------
 * Purely a UI-layer stand-in for [com.example.focusapp.domain.model.FocusZone]
 * while there is no real data source wired up yet. Once
 * FocusRepository.getFocusZones() does something real, replace this with
 * the actual domain model (or a small mapper between the two).
 */
private data class UiFocusLocation(val id: String, val name: String)

/**
 * MapScreen
 * -----------
 * The "Map" tab (matches the teammate-provided wireframe "Image 4"). Lists
 * the user's saved focus locations, each as a dark pill labelled
 * "Edit Location" with a pencil icon button beside it, plus a big "+"
 * pill at the bottom to add a new one.
 *
 * NONE of this is wired to real data:
 *  - [locations] starts out with one hard-coded entry, just so the screen
 *    isn't empty, matching the mockup.
 *  - Tapping the pencil or "+" does nothing yet - no wireframe was
 *    provided for what those screens should look like (an actual map
 *    picker, presumably), so they're left as clearly-marked TODOs rather
 *    than guessed at.
 */
@Composable
fun MapScreen() {
    // TODO: to be implemented later - replace this with real data from
    // FocusRepository.getFocusZones() (via a ViewModel), instead of a
    // hard-coded local list.
    val locations = remember { mutableStateListOf(UiFocusLocation(id = "l1", name = "Location 1")) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        locations.forEach { location ->
            FocusLocationRow(location = location)
            Box(modifier = Modifier.height(24.dp)) // spacer between cards
        }

        AddLocationPillButton()

        // TODO: to be implemented later - empty-state message when there
        // are no saved locations yet, once `locations` is no longer
        // hard-coded.
    }
}

/**
 * FocusLocationRow
 * -------------------
 * One row in the locations list: the location's name above a dark pill
 * containing a pin icon + "Edit Location" text, plus a separate circular
 * pencil-icon button to its right.
 *
 * TODO: to be implemented later - both the pill and the pencil button
 * should eventually open a map-based edit screen for this location; no
 * wireframe for that screen has been provided yet, so both are left
 * without an onClick handler for now (see MapScreen's doc comment).
 */
@Composable
private fun FocusLocationRow(location: UiFocusLocation) {
    Column {
        Text(
            text = location.name,
            color = WireframeColors.OnLight,
            modifier = Modifier.padding(start = 8.dp, bottom = 6.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .background(WireframeColors.Card, shape = RoundedCornerShape(50))
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Place,
                    contentDescription = null,
                    tint = WireframeColors.OnDark
                )
                Text(
                    text = "Edit Location",
                    color = WireframeColors.OnDark,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }

            // Separate circular "edit" button, outside the pill, matching
            // the wireframe's pencil icon on the right.
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(WireframeColors.Background),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = "Edit ${location.name}",
                    tint = WireframeColors.OnLight
                )
            }
        }
    }
}

/**
 * AddLocationPillButton
 * ------------------------
 * Same big dark "+" pill style as [com.example.focusapp.ui.screens.apps.AddPillButton],
 * duplicated here (rather than shared) since the Apps and Map flows are
 * independent enough that a shared component isn't worth the coupling yet
 * - revisit if a third "+ pill" screen shows up.
 *
 * TODO: to be implemented later - wire this to a real "add location" flow
 * (presumably an embedded map) once that screen is designed.
 */
@Composable
private fun AddLocationPillButton() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(50))
            .clickable { /* TODO: to be implemented later - no destination screen designed yet. */ }
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(WireframeColors.CardLight, shape = RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add location", tint = WireframeColors.OnDark)
        }
    }
}
