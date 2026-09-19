package com.example.focusapp.ui.screens.map

import android.Manifest
import android.widget.Toast
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.example.focusapp.ui.theme.WireframeColors

import com.example.focusapp.data.sensor.SensorDataSource


/**
 * UiFocusLocation
 * -----------------
 * Purely a UI-layer stand-in for [com.example.focusapp.domain.model.FocusZone]
 * while there is no real data source wired up yet. Once
 * FocusRepository.getFocusZone() does something real, replace this with
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
    val context = LocalContext.current
    val sensorDataSource = remember { SensorDataSource() }

    var showBackgroundRationale by remember { mutableStateOf(false) }

    // --- NEW: State to hold the ID typed by the user for testing ---
    var zoneIdToRemove by remember { mutableStateOf("") }
    var isFastTracking by remember { mutableStateOf(false) }

    val backgroundPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "Background tracking enabled!", Toast.LENGTH_SHORT).show()
            // TODO: Replace 0.0 with actual map coordinates later
            sensorDataSource.startTracking(context)
            sensorDataSource.setTrackingPriority(context, isHigh = isFastTracking)
            sensorDataSource.addFocusZone(context, lat = 0.0, lng = 0.0, rad = 100.0f)
        } else {
            Toast.makeText(context, "Background denied. Geofences won't work.", Toast.LENGTH_LONG).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(context, "GPS Permission Granted!", Toast.LENGTH_SHORT).show()
            sensorDataSource.startTracking(context)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val hasBackground = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasBackground) {
                    showBackgroundRationale = true
                } else {
                    sensorDataSource.addFocusZone(context, lat = 0.0, lng = 0.0, rad = 100f)
                }
            } else {
                sensorDataSource.addFocusZone(context, lat = 0.0, lng = 0.0, rad = 100f)
            }
        } else {
            Toast.makeText(context, "Permission denied. GPS won't work.", Toast.LENGTH_SHORT).show()
        }
    }


    val locations = remember { mutableStateListOf(UiFocusLocation(id = "l1", name = "Location 1")) }

    if (showBackgroundRationale) {
        AlertDialog(
            onDismissRequest = { showBackgroundRationale = false },
            title = { Text("Background Tracking Required") },
            text = { Text("To automatically start focus sessions when your phone is in your pocket, this app needs background location access. On the next screen, please select 'Allow all the time'.") },
            confirmButton = {
                Button(onClick = {
                    showBackgroundRationale = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        backgroundPermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    }
                }) {
                    Text("Continue")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBackgroundRationale = false }) {
                    Text("No Thanks")
                }
            }
        )
    }

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

        AddLocationPillButton(
            onClick = {
                val hasFine = ContextCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasFine) {
                    sensorDataSource.startTracking(context)
                    sensorDataSource.setTrackingPriority(context, isHigh = isFastTracking)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val hasBg = ContextCompat.checkSelfPermission(
                            context, Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED

                        if (!hasBg) {
                            showBackgroundRationale = true
                        } else {
                            sensorDataSource.addFocusZone(context, lat = 0.0, lng = 0.0, rad = 100f)
                        }
                    } else {
                        sensorDataSource.addFocusZone(context, lat = 0.0, lng = 0.0, rad = 100f)
                    }
                } else {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isFastTracking,
                onCheckedChange = { isChecked ->
                    isFastTracking = isChecked
                    sensorDataSource.setTrackingPriority(context, isHigh = isChecked)
                }
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = if (isFastTracking) "High Accuracy (5s)" else "Power Saving (30s)",
                color = WireframeColors.OnLight
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = {
            sensorDataSource.stopTracking(context)
        }) {
            Text("Turn off Tracking")
        }

        // --- NEW: Test UI for removing geofences ---
        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Geofence Testing",
            color = WireframeColors.OnLight
        )

        OutlinedTextField(
            value = zoneIdToRemove,
            onValueChange = { zoneIdToRemove = it },
            label = { Text("Enter UUID to remove") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (zoneIdToRemove.isNotBlank()) {
                    sensorDataSource.removeFocusZone(context, zoneIdToRemove)
                    Toast.makeText(context, "Attempting to remove: $zoneIdToRemove", Toast.LENGTH_SHORT).show()
                    zoneIdToRemove = "" // Clear the field after clicking
                } else {
                    Toast.makeText(context, "Please enter an ID first", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Remove Focus Zone")
        }
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
private fun AddLocationPillButton(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(50))
            .clickable { /* TODO: to be implemented later - no destination screen designed yet. */
                onClick() //temporary test
             }
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
