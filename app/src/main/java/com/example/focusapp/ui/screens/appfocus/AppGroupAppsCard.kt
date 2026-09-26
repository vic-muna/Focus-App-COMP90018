package com.example.focusapp.ui.screens.appfocus

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.apps.InstalledAppInfo
import com.example.focusapp.ui.components.FocusCheckbox
import com.example.focusapp.ui.components.NextButton
import com.example.focusapp.ui.components.RejectButton
import com.example.focusapp.ui.components.verticalScrollbar
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

private val AppListHeight = 225.dp
private val AppIconSize = 26.dp

/**
 * Figma: "App Focuse" add-group step 1 - pick the apps a new group blocks.
 * [apps] is null while the installed-app list is still loading. X discards,
 * the arrow moves on (disabled until at least one app is selected).
 */
@Composable
fun AppGroupAppsCard(
    apps: List<InstalledAppInfo>?,
    selectedPackages: Set<String>,
    onToggleApp: (packageName: String) -> Unit,
    onClose: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            RejectButton(onClick = onClose)
            NextButton(onClick = onNext, enabled = selectedPackages.isNotEmpty())
        }

        Text(
            text = "Apps Group",
            style = typography.tileTitle,
            color = colors.onSurface,
            modifier = Modifier.padding(start = 12.dp, top = 16.dp, bottom = 8.dp),
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(AppListHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceSunken),
            contentAlignment = Alignment.Center,
        ) {
            if (apps == null) {
                Text(text = "Loading apps…", style = typography.caption, color = colors.onSurfaceMuted)
            } else {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScrollbar(
                            state = listState,
                            thumbColor = colors.onSurfaceMuted,
                            trackColor = colors.surface,
                        ),
                ) {
                    items(apps, key = { it.packageName }) { app ->
                        AppSelectRow(
                            app = app,
                            selected = app.packageName in selectedPackages,
                            onToggle = { onToggleApp(app.packageName) },
                        )
                    }
                }
            }
        }
    }
}

/** One row of [AppGroupAppsCard]: checkbox, app icon and name. The whole row toggles. */
@Composable
private fun AppSelectRow(
    app: InstalledAppInfo,
    selected: Boolean,
    onToggle: () -> Unit,
) {
    val colors = FocusTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(start = 20.dp, end = 32.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FocusCheckbox(checked = selected, onCheckedChange = { onToggle() })
        Spacer(Modifier.width(20.dp))
        val icon = app.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(AppIconSize)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(AppIconSize)
                    .clip(RoundedCornerShape(6.dp))
                    .background(colors.surface),
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = app.label,
            style = FocusTheme.typography.listLabel,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun AppGroupAppsCardPreview() {
    FocusAppTheme {
        AppGroupAppsCard(
            apps = List(12) { InstalledAppInfo("com.example.app$it", "App $it", icon = null) },
            selectedPackages = setOf("com.example.app1"),
            onToggleApp = {},
            onClose = {},
            onNext = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
