package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

/**
 * The settings gear pinned to the top-right corner. On the Settings screen
 * itself ([isOpen]) the same spot shows an X instead, which closes it -
 * tapping the same place opens and closes Settings.
 * When [onPartyClick] is set (Home only), the Party Mode icon sits in the
 * top-left corner, on the other side of the gear.
 */
@Composable
fun SettingsTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOpen: Boolean = false,
    onPartyClick: (() -> Unit)? = null,
) {
    Box(modifier = modifier.topCorners()) {
        if (onPartyClick != null) {
            FocusIconButton(
                iconRes = R.drawable.partymod,
                contentDescription = "Party Mode",
                onClick = onPartyClick,
                modifier = Modifier.align(Alignment.TopStart),
            )
        }
        if (isOpen) {
            CornerCloseButton(
                contentDescription = "Close settings",
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        } else {
            FocusIconButton(
                iconRes = R.drawable.ic_setting_fill,
                contentDescription = "Settings",
                onClick = onSettingsClick,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

/**
 * A top bar with just an X, in the top-left ([alignment] = TopStart) or
 * top-right (TopEnd) corner - same spot and size as Home's corner icons.
 * Party Mode puts it on the left, where Home's Party Mode icon was; the
 * dashboard puts it on the right.
 */
@Composable
fun CloseTopBar(
    contentDescription: String,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.TopStart,
) {
    Box(modifier = modifier.topCorners()) {
        CornerCloseButton(
            contentDescription = contentDescription,
            onClick = onCloseClick,
            modifier = Modifier.align(alignment),
        )
    }
}

/** FocusIconButton pads its icon by 4 dp, so this puts the corner icons themselves at ScreenTop. */
private fun Modifier.topCorners(): Modifier = this
    .fillMaxWidth()
    .padding(top = FocusSpacing.ScreenTop - 4.dp, start = 21.dp, end = 21.dp)

/** An X with the same 50 dp footprint as a corner icon button, so it can take that icon's place. */
@Composable
private fun CornerCloseButton(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.size(50.dp), contentAlignment = Alignment.Center) {
        GlyphCircleButton(
            glyph = FocusGlyphs.Close,
            contentDescription = contentDescription,
            container = SolidColor(FocusTheme.colors.primaryAction),
            glyphColor = FocusTheme.colors.onPrimaryAction,
            onClick = onClick,
            size = 42.dp,
        )
    }
}

@Preview(widthDp = 393)
@Composable
private fun SettingsTopBarPreview() {
    FocusAppTheme {
        SettingsTopBar(onSettingsClick = {}, modifier = Modifier.background(FocusTheme.colors.background))
    }
}

@Preview(widthDp = 393)
@Composable
private fun SettingsTopBarWithPartyPreview() {
    FocusAppTheme {
        SettingsTopBar(onSettingsClick = {}, onPartyClick = {}, modifier = Modifier.background(FocusTheme.colors.background))
    }
}

@Preview(widthDp = 393)
@Composable
private fun SettingsTopBarOpenPreview() {
    FocusAppTheme {
        SettingsTopBar(onSettingsClick = {}, isOpen = true, modifier = Modifier.background(FocusTheme.colors.background))
    }
}

@Preview(widthDp = 393)
@Composable
private fun CloseTopBarPreview() {
    FocusAppTheme {
        CloseTopBar(
            contentDescription = "Close",
            onCloseClick = {},
            modifier = Modifier.background(FocusTheme.colors.background),
        )
    }
}

@Preview(widthDp = 393)
@Composable
private fun CloseTopBarEndPreview() {
    FocusAppTheme {
        CloseTopBar(
            contentDescription = "Close",
            onCloseClick = {},
            alignment = Alignment.TopEnd,
            modifier = Modifier.background(FocusTheme.colors.background),
        )
    }
}