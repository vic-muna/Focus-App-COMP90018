package com.example.focusapp.ui.screens.location

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.card.SwitchListCard
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme
import java.util.Locale

/** One saved location in the list: name, approximate location text, range, coordinates and an on/off switch. */
@Composable
fun LocationGroupCard(
    name: String,
    approxLocation: String?,
    radiusMeters: Float,
    latitude: Double,
    longitude: Double,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {},
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    val placeName = approxLocation?.takeIf { it.isNotBlank() } ?: "location"
    val formattedPlace = if (placeName.startsWith("Approx.", ignoreCase = true)) placeName else "Approx. $placeName"
    val subtitleText = "$formattedPlace (Range: ${radiusMeters.toInt()}m)"

    SwitchListCard(
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        modifier = modifier,
        onClick = onClick,
        onLongClick = onLongClick,
        spacing = 4.dp,
        verticalPadding = 10.dp,
    ) {
        Text(
            text = name,
            style = typography.cardTitle,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 2.dp),
        )
        Text(
            text = subtitleText,
            style = typography.caption,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = String.format(Locale.US, "Latitude: %.4f | Longitude: %.4f", latitude, longitude),
            style = typography.caption,
            color = colors.onSurfaceMuted,
        )
    }
}

@Preview
@Composable
private fun LocationGroupCardPreview() {
    FocusAppTheme {
        LocationGroupCard(
            name = "Group001",
            approxLocation = "FBE Library",
            radiusMeters = 100f,
            latitude = 40.7128,
            longitude = -74.0060,
            enabled = true,
            onEnabledChange = {},
        )
    }
}
