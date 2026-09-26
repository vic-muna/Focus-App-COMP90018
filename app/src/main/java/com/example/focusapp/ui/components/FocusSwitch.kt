package com.example.focusapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val TrackWidth = 64.dp
private val TrackHeight = 32.dp
private val ThumbSize = 28.dp
private val ThumbPadding = 2.dp

/** Figma: "Switch Button" - a pill track that turns [FocusColors.accent] when on, with a white shadowed thumb. */
@Composable
fun FocusSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.accent else colors.primaryAction,
        label = "switchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) TrackWidth - ThumbSize - ThumbPadding * 2 else 0.dp,
        label = "switchThumb",
    )

    Box(
        modifier = modifier
            .size(TrackWidth, TrackHeight)
            .clip(CircleShape)
            .background(trackColor)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(ThumbPadding),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(ThumbSize)
                .shadow(elevation = 3.dp, shape = CircleShape)
                .background(colors.pure, CircleShape)
        )
    }
}

@Preview
@Composable
private fun FocusSwitchPreview() {
    FocusAppTheme {
        Column(Modifier.background(FocusTheme.colors.surface).padding(8.dp)) {
            FocusSwitch(checked = false, onCheckedChange = {})
            FocusSwitch(checked = true, onCheckedChange = {}, modifier = Modifier.padding(top = 8.dp))
        }
    }
}
