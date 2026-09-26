package com.example.focusapp.ui.screens.appfocus

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusTheme

/** Figma "App Focuse" grid tile size. */
val AppGroupTileWidth = 154.dp
val AppGroupTileHeight = 170.dp

private val TileShape = RoundedCornerShape(16.dp)
private val TileIconSize = 54.dp

/**
 * Figma: one app-group tile - the group's first app icon, its name and how
 * many apps it blocks. [icon] is null when the group has no apps yet (or
 * the icon couldn't be resolved), which shows a blank placeholder instead.
 * Tap = [onClick] (open the group), hold = [onLongClick] (delete).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGroupTile(
    name: String,
    appCount: Int,
    icon: Bitmap?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: () -> Unit = {},
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = modifier
            .size(AppGroupTileWidth, AppGroupTileHeight)
            .clip(TileShape)
            .background(colors.surface)
            .combinedClickable(role = Role.Button, onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(30.dp))
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(TileIconSize)
                    .clip(RoundedCornerShape(12.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(TileIconSize)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceSunken),
            )
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = name,
            style = typography.tileTitle,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = if (appCount == 1) "1 app selected" else "$appCount apps selected",
            style = typography.caption,
            color = colors.onSurface,
        )
    }
}

/** Figma: the last grid tile - a muted circle with a plus, for creating a new group. */
@Composable
fun AddAppGroupTile(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = modifier
            .size(AppGroupTileWidth, AppGroupTileHeight)
            .clip(TileShape)
            .background(colors.surface)
            .clickable(role = Role.Button, onClickLabel = "Add group", onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(62.dp)
                .background(colors.onSurfaceMuted, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.size(30.dp)) {
                val stroke = 5.dp.toPx()
                val mid = size.width / 2
                drawLine(colors.surface, Offset(mid, 0f), Offset(mid, size.height), stroke, StrokeCap.Round)
                drawLine(colors.surface, Offset(0f, mid), Offset(size.width, mid), stroke, StrokeCap.Round)
            }
        }
    }
}

@Preview
@Composable
private fun AppGroupTilePreview() {
    FocusAppTheme {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier
                .background(FocusTheme.colors.background)
                .padding(16.dp),
        ) {
            AppGroupTile(name = "Group 1", appCount = 12, icon = null, onClick = {})
            AddAppGroupTile(onClick = {})
        }
    }
}
