package com.example.focusapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Figma Settings: one titled group of rows - an icon and title, a divider,
 * then [content] (usually [SettingsRow]s, each ending in its own divider).
 */
@Composable
fun SettingsSection(
    title: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = FocusTheme.colors

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.onSurface,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(text = title, style = FocusTheme.typography.cardTitle, color = colors.onSurface)
        }
        SettingsDivider()
        content()
    }
}

/**
 * Figma Settings: one row - [label] on the left, [trailing] (a switch, a
 * value or an arrow) on the right, and a divider underneath. The whole row
 * calls [onClick] when it's set.
 */
@Composable
fun SettingsRow(
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = FocusTheme.typography.rowLabel,
                color = FocusTheme.colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(12.dp))
            trailing()
        }
        SettingsDivider()
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(thickness = 1.dp, color = FocusTheme.colors.onSurfaceMuted)
}

@Preview(widthDp = 360)
@Composable
private fun SettingsSectionPreview() {
    FocusAppTheme {
        SettingsSection(
            title = "Music",
            icon = Icons.Filled.MusicNote,
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        ) {
            SettingsRow(label = "Focus Music") { FocusSwitch(checked = false, onCheckedChange = {}) }
            SettingsRow(label = "Home Music") { FocusSwitch(checked = true, onCheckedChange = {}) }
        }
    }
}