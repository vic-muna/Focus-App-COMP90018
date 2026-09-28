package com.example.focusapp.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.ui.components.RejectButton
import com.example.focusapp.ui.theme.BackgroundTheme
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

private val TileShape = RoundedCornerShape(20.dp)

/**
 * Picks the background theme: a 3-column grid of portrait tiles, the
 * current one outlined. Tapping a theme picks it right away ([onSelect]);
 * X or back leaves without changing anything ([onClose]). Slots without a
 * theme yet are grey "Coming soon" tiles.
 */
@Composable
fun ThemePickerScreen(
    selectedId: String,
    onSelect: (BackgroundTheme) -> Unit,
    onClose: () -> Unit,
) {
    BackHandler(onBack = onClose)

    val colors = FocusTheme.colors
    val slots: List<BackgroundTheme?> =
        BackgroundThemes.all + List((BackgroundThemes.PICKER_SLOTS - BackgroundThemes.all.size).coerceAtLeast(0)) { null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = FocusSpacing.ScreenTop, start = 32.dp, end = 32.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            RejectButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart))
            Text(
                text = "Background",
                style = FocusTheme.typography.primaryActionLabel,
                color = colors.onSurface,
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = PaddingValues(top = 32.dp, bottom = FocusSpacing.ScreenBottom),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(slots) { theme ->
                ThemeTile(
                    theme = theme,
                    selected = theme != null && theme.id == selectedId,
                    onClick = { theme?.let(onSelect) },
                )
            }
        }
    }
}

/** One picker tile - the theme's art and name, or a grey "Coming soon" tile when [theme] is null. */
@Composable
private fun ThemeTile(
    theme: BackgroundTheme?,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = FocusTheme.colors

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(TileShape)
                .background(colors.primaryAction)
                .then(if (selected) Modifier.border(4.dp, colors.accent, TileShape) else Modifier)
                .clickable(enabled = theme != null, role = Role.RadioButton, onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            if (theme != null) {
                Image(
                    painter = painterResource(theme.homeArt),
                    contentDescription = theme.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Text(
                    text = "Coming\nsoon",
                    style = FocusTheme.typography.caption,
                    color = colors.onPrimaryAction,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = theme?.name.orEmpty(),
            style = FocusTheme.typography.listLabel,
            color = if (selected) colors.accent else colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(16.dp),
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun ThemePickerScreenPreview() {
    FocusAppTheme {
        ThemePickerScreen(selectedId = BackgroundThemes.Scene.id, onSelect = {}, onClose = {})
    }
}