package com.example.focusapp.ui.screens.home

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusZone
import com.example.focusapp.ui.common.rememberLocationPermissionState
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_RADIUS_METERS = 50f
private const val MAX_RADIUS_METERS = 1000f
private const val DEFAULT_RADIUS_METERS = 150f

/**
 * Form-based editor for the single Location Zone. No map view - just a
 * name field, a radius slider, and a "Use current location" button that
 * silently captures GPS coordinates. Matches the fidelity of the rest of
 * the Home flow's wireframe screens; a real map is explicitly out of
 * scope here (no Maps SDK/API key in this project yet).
 *
 * Self-contained like EditAppGroupScreen: loads/saves via
 * FocusRepositoryProvider directly rather than NavGraph-hoisted state,
 * since there's no natural hoisting point for a single zone there.
 */
@Composable
fun EditLocationZoneScreen(
    onSaveComplete: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val permissionState = rememberLocationPermissionState()

    var isLoading by remember { mutableStateOf(true) }
    var existingZoneId by remember { mutableStateOf<String?>(null) }
    var zoneName by remember { mutableStateOf("") }
    var radiusMeters by remember { mutableStateOf(DEFAULT_RADIUS_METERS) }
    var latitude by remember { mutableStateOf<Double?>(null) }
    var longitude by remember { mutableStateOf<Double?>(null) }
    var isFetchingLocation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val existing = withContext(Dispatchers.IO) {
            FocusRepositoryProvider.get(context).getFocusZone()
        }
        if (existing != null) {
            existingZoneId = existing.id
            zoneName = existing.name
            radiusMeters = existing.radiusMeters
            latitude = existing.latitude
            longitude = existing.longitude
        }
        isLoading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(horizontal = 24.dp)
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Edit Location Zone",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Button(
                enabled = !isLoading && latitude != null && longitude != null,
                onClick = {
                    val lat = latitude ?: return@Button
                    val lng = longitude ?: return@Button
                    scope.launch {
                        val zone = FocusZone(
                            id = existingZoneId ?: "zone_${System.currentTimeMillis()}",
                            name = zoneName.ifBlank { "My Location" },
                            latitude = lat,
                            longitude = lng,
                            radiusMeters = radiusMeters
                        )
                        withContext(Dispatchers.IO) {
                            FocusRepositoryProvider.get(context).saveFocusZone(zone)
                        }
                        onSaveComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = zoneName,
            onValueChange = { zoneName = it },
            label = { Text("Zone name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        Text(
            text = "Radius: ${radiusMeters.toInt()} m",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
        Slider(
            value = radiusMeters,
            onValueChange = { radiusMeters = it },
            valueRange = MIN_RADIUS_METERS..MAX_RADIUS_METERS
        )

        Spacer(Modifier.height(24.dp))

        OutlinedButton(
            enabled = !isFetchingLocation,
            onClick = {
                if (!permissionState.hasPermission) {
                    permissionState.request()
                } else {
                    isFetchingLocation = true
                    fetchCurrentLocation(context) { lat, lng ->
                        latitude = lat
                        longitude = lng
                        isFetchingLocation = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                when {
                    !permissionState.hasPermission -> "Allow location access"
                    isFetchingLocation -> "Locating…"
                    else -> "Use current location"
                }
            )
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = when {
                !permissionState.hasPermission ->
                    "Location permission not granted yet - tap the button above to allow it."
                latitude != null && longitude != null -> "Location set."
                else -> "No location captured yet - tap \"Use current location\" to set one."
            },
            fontSize = 13.sp,
            color = Color(0xFF6B6B6B)
        )
    }
}

@SuppressLint("MissingPermission")
private fun fetchCurrentLocation(
    context: Context,
    onResult: (latitude: Double, longitude: Double) -> Unit
) {
    LocationServices.getFusedLocationProviderClient(context)
        .lastLocation
        .addOnSuccessListener { location ->
            if (location != null) {
                onResult(location.latitude, location.longitude)
            }
        }
}

@Preview(showBackground = true)
@Composable
private fun EditLocationZoneScreenPreview() {
    EditLocationZoneScreen(onSaveComplete = {})
}
