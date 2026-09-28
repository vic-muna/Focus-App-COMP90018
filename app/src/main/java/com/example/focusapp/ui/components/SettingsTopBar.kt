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
 */
@Composable
fun SettingsTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isOpen: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            // FocusIconButton pads its icon by 4 dp, so this puts the gear itself at ScreenTop.
            .padding(top = FocusSpacing.ScreenTop - 4.dp, end = 21.dp),
        contentAlignment = Alignment.TopEnd,
    ) {
        if (isOpen) {
            // Same 50 dp footprint as the gear button, so the X sits exactly where the gear was.
            Box(modifier = Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                GlyphCircleButton(
                    glyph = FocusGlyphs.Close,
                    contentDescription = "Close settings",
                    container = SolidColor(FocusTheme.colors.primaryAction),
                    glyphColor = FocusTheme.colors.onPrimaryAction,
                    onClick = onSettingsClick,
                    size = 42.dp,
                )
            }
        } else {
            FocusIconButton(
                iconRes = R.drawable.ic_setting_fill,
                contentDescription = "Settings",
                onClick = onSettingsClick,
            )
        }
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
private fun SettingsTopBarOpenPreview() {
    FocusAppTheme {
        SettingsTopBar(onSettingsClick = {}, isOpen = true, modifier = Modifier.background(FocusTheme.colors.background))
    }
}