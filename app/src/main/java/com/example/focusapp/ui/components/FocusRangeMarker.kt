package com.example.focusapp.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Figma: "Focuse Range" - a pin inside a ring showing a zone's effective
 * range. Pass [FocusColors.mapZone] for saved zones and
 * [FocusColors.mapZoneNew] for one being created.
 */
@Composable
fun FocusRangeMarker(
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 116.dp,
) {
    Box(
        modifier = modifier
            // requiredSize: a large zone may be wider than the screen - let it overflow, not shrink.
            .requiredSize(diameter)
            .border(width = 2.dp, color = color, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.focuse_range),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(width = 31.dp, height = 36.dp),
        )
    }
}

@Preview
@Composable
private fun FocusRangeMarkerPreview() {
    FocusAppTheme {
        FocusRangeMarker(color = FocusTheme.colors.mapZoneNew)
    }
}
