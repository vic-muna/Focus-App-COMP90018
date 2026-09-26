package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.ConfirmButton
import com.example.focusapp.ui.components.FocusSlider
import com.example.focusapp.ui.components.FocusTextField
import com.example.focusapp.ui.components.RejectButton
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import java.util.Locale
import kotlin.math.abs

/** Allowed "Effective Range" values, in meters. */
val LocationRadiusRange = 50f..500f

/**
 * Figma: "Add new location" card - name the zone, set its radius, then
 * confirm (check) or discard (X). Also used to edit a saved zone.
 * [latitude]/[longitude] are null while the position is still unknown, which
 * also disables confirm; [locationLabel] describes where the pin is.
 */
@Composable
fun AddLocationCard(
    locationLabel: String,
    latitude: Double?,
    longitude: Double?,
    name: String,
    onNameChange: (String) -> Unit,
    radiusMeters: Float,
    onRadiusChange: (Float) -> Unit,
    onScheduleClick: () -> Unit,
    scheduleName: String?,
    scheduleAppCount: Int,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val canConfirm = name.isNotBlank() && latitude != null && longitude != null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RejectButton(onClick = onClose)
            ConfirmButton(
                onClick = onConfirm,
                contentDescription = "Save location",
                enabled = canConfirm,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // A long place name gets cut short rather than running into the coordinates.
            Text(
                text = locationLabel,
                style = typography.caption,
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
            )
            if (latitude != null && longitude != null) {
                Text(
                    text = formatCoordinates(latitude, longitude),
                    style = typography.caption,
                    color = colors.onSurface,
                )
            }
        }

        FocusTextField(
            value = name,
            onValueChange = onNameChange,
            placeholder = "Enter Group Name",
        )

        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "Effective Range", style = typography.caption, color = colors.onSurface)
                Text(text = "${radiusMeters.toInt()}m", style = typography.caption, color = colors.onSurface)
            }
            FocusSlider(
                value = radiusMeters,
                onValueChange = { onRadiusChange((it / 10f).toInt() * 10f) },
                valueRange = LocationRadiusRange,
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(12.dp))
                // Once a group is linked, the button turns into an accent summary of it.
                .background(if (scheduleName != null) colors.accent else colors.surfaceSunken)
                .clickable(role = Role.Button, onClick = onScheduleClick),
            contentAlignment = Alignment.Center,
        ) {
            if (scheduleName == null) {
                Text(text = "Schedule", style = typography.inputLarge, color = colors.onSurfaceMuted)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = scheduleName,
                        style = typography.tileTitle,
                        color = colors.onPrimaryAction,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = if (scheduleAppCount == 1) "1 app selected" else "$scheduleAppCount apps selected",
                        style = typography.body,
                        color = colors.onPrimaryAction,
                        maxLines = 1,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }
        }
    }
}

/** e.g. "144.9631E / 37.8136S" - longitude first, matching the Figma card. */
private fun formatCoordinates(latitude: Double, longitude: Double): String {
    val lng = String.format(Locale.US, "%.4f%s", abs(longitude), if (longitude >= 0) "E" else "W")
    val lat = String.format(Locale.US, "%.4f%s", abs(latitude), if (latitude >= 0) "N" else "S")
    return "$lng / $lat"
}

@Preview
@Composable
private fun AddLocationCardPreview() {
    FocusAppTheme {
        AddLocationCard(
            locationLabel = "Approx. current location",
            latitude = -37.8136,
            longitude = 144.9631,
            name = "",
            onNameChange = {},
            radiusMeters = 100f,
            onRadiusChange = {},
            onScheduleClick = {},
            scheduleName = "Group 1",
            scheduleAppCount = 12,
            onClose = {},
            onConfirm = {},
        )
    }
}
