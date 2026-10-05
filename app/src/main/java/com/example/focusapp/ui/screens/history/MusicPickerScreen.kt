package com.example.focusapp.ui.screens.history

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.R
import com.example.focusapp.domain.model.FocusMusic
import com.example.focusapp.domain.model.FocusMusics
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.UnlockCard
import com.example.focusapp.ui.components.card.consumeTaps
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

private val RowShape = RoundedCornerShape(16.dp)

/**
 * Picks the Focus Mode music, opened from the dashboard's ID card: a list of tracks,
 * the current one outlined. Works like [ThemePickerScreen] - tapping an unlocked track
 * picks it ([onSelect]); tapping a locked one opens a card showing the user's [points]
 * and its price, and buys it on confirm ([onUnlock]). [points] is null while loading.
 */
@Composable
fun MusicPickerScreen(
    selectedId: String,
    ownedIds: Set<String>,
    points: Long?,
    onSelect: (FocusMusic) -> Unit,
    onUnlock: (FocusMusic) -> Unit,
    onClose: () -> Unit,
) {
    // The locked track the unlock card is open for.
    var musicToUnlock by remember { mutableStateOf<FocusMusic?>(null) }

    BackHandler(onBack = { if (musicToUnlock != null) musicToUnlock = null else onClose() })

    val colors = FocusTheme.colors

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background)
                .padding(top = FocusSpacing.ScreenTop, start = 32.dp, end = 32.dp),
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                RejectButton(onClick = onClose, modifier = Modifier.align(Alignment.CenterStart))
                Text(
                    text = "Music",
                    style = FocusTheme.typography.primaryActionLabel,
                    color = colors.onSurface,
                )
                if (points != null) {
                    Text(
                        text = "$points pts",
                        style = FocusTheme.typography.listLabel,
                        color = colors.accent,
                        modifier = Modifier.align(Alignment.CenterEnd),
                    )
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(top = 32.dp, bottom = FocusSpacing.ScreenBottom),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(FocusMusics.all, key = { it.id }) { music ->
                    val locked = !music.isUnlocked(ownedIds)
                    MusicRow(
                        music = music,
                        selected = music.id == selectedId,
                        locked = locked,
                        onClick = { if (locked) musicToUnlock = music else onSelect(music) },
                    )
                }
            }
        }

        musicToUnlock?.let { music ->
            FlyCardOverlay(onOutsideClick = { musicToUnlock = null }) {
                UnlockCard(
                    itemName = music.name,
                    points = points ?: 0,
                    pricePoints = music.pricePoints,
                    onConfirm = {
                        onUnlock(music)
                        musicToUnlock = null
                    },
                    onClose = { musicToUnlock = null },
                    modifier = Modifier.consumeTaps(),
                    // Try before you buy: a 15-second clip, stopped when the card closes.
                    preview = music.audio?.let { audio -> { MusicPreview(audio = audio) } },
                )
            }
        }
    }
}

/** One track: a note (or a lock), its name and intro, and its price or "In use"; outlined when [selected]. */
@Composable
private fun MusicRow(music: FocusMusic, selected: Boolean, locked: Boolean, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RowShape)
            .background(colors.surface)
            .then(if (selected) Modifier.border(3.dp, colors.accent, RowShape) else Modifier)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (locked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Locked",
                tint = colors.onSurfaceMuted,
                modifier = Modifier.size(28.dp),
            )
        } else {
            Icon(
                painter = painterResource(R.drawable.ic_music),
                contentDescription = null,
                tint = if (selected) colors.accent else colors.onSurface,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = music.name,
                style = typography.body,
                color = when {
                    selected -> colors.accent
                    locked -> colors.onSurfaceMuted
                    else -> colors.onSurface
                },
            )
            Text(text = music.intro, style = typography.caption, color = colors.onSurfaceMuted)
        }
        when {
            locked -> Text(text = "${music.pricePoints} pts", style = typography.caption, color = colors.onSurfaceMuted)
            selected -> Text(text = "In use", style = typography.caption, color = colors.accent)
        }
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun MusicPickerScreenPreview() {
    FocusAppTheme {
        MusicPickerScreen(
            selectedId = FocusMusics.default.id,
            ownedIds = emptySet(),
            points = 42,
            onSelect = {},
            onUnlock = {},
            onClose = {},
        )
    }
}
