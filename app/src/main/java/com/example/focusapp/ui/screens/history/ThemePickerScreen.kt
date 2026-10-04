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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.focusapp.domain.model.RewardRules
import com.example.focusapp.ui.theme.BackgroundTheme
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import com.example.focusapp.ui.components.button.RejectButton
import com.example.focusapp.ui.components.card.FlyCardOverlay
import com.example.focusapp.ui.components.card.consumeTaps

private val TileShape = RoundedCornerShape(20.dp)

/**
 * Picks the background theme: a 3-column grid of portrait tiles, the
 * current one outlined. Tapping a theme picks it right away ([onSelect]);
 * a reward theme stays locked until [bestStreakDays] reaches its goal.
 * For testing: tapping a locked tile three times asks for the unlock code,
 * and the right one unlocks it ([onUnlockWithCode]) - see [RewardRules.TEST_UNLOCK_CODE].
 * X or back leaves without changing anything ([onClose]). Slots without a
 * theme yet are "Coming soon" tiles.
 */
@Composable
fun ThemePickerScreen(
    selectedId: String,
    bestStreakDays: Int,
    onSelect: (BackgroundTheme) -> Unit,
    onClose: () -> Unit,
    codeUnlockedIds: Set<String> = emptySet(),
    onUnlockWithCode: (BackgroundTheme) -> Unit = {},
) {
    // The locked theme being tapped and how many times, then the one the code card is open for.
    var tappedLockedId by remember { mutableStateOf<String?>(null) }
    var lockedTapCount by remember { mutableIntStateOf(0) }
    var themeToUnlock by remember { mutableStateOf<BackgroundTheme?>(null) }
    var code by remember { mutableStateOf("") }
    var showCodeError by remember { mutableStateOf(false) }

    fun closeCodeCard() {
        themeToUnlock = null
        code = ""
        showCodeError = false
    }

    fun onLockedTileTap(theme: BackgroundTheme) {
        lockedTapCount = if (tappedLockedId == theme.id) lockedTapCount + 1 else 1
        tappedLockedId = theme.id
        if (lockedTapCount >= RewardRules.TEST_UNLOCK_TAPS) {
            lockedTapCount = 0
            themeToUnlock = theme
        }
    }

    BackHandler(onBack = { if (themeToUnlock != null) closeCodeCard() else onClose() })

    val colors = FocusTheme.colors
    // The real themes, then empty "Coming soon" slots (null) to fill the grid.
    val emptySlotCount = (BackgroundThemes.PICKER_SLOTS - BackgroundThemes.all.size).coerceAtLeast(0)
    val slots: List<BackgroundTheme?> = BackgroundThemes.all + List(emptySlotCount) { null }

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
                    val locked = theme != null &&
                        !theme.isUnlocked(bestStreakDays) && theme.id !in codeUnlockedIds
                    ThemeTile(
                        theme = theme,
                        selected = theme != null && theme.id == selectedId,
                        locked = locked,
                        onClick = { theme?.let { if (locked) onLockedTileTap(it) else onSelect(it) } },
                    )
                }
            }
        }

        themeToUnlock?.let { theme ->
            FlyCardOverlay(onOutsideClick = ::closeCodeCard) {
                ThemeUnlockCodeCard(
                    themeName = theme.name,
                    code = code,
                    onCodeChange = {
                        code = it
                        showCodeError = false
                    },
                    showError = showCodeError,
                    onConfirm = {
                        if (code.trim() == RewardRules.TEST_UNLOCK_CODE) {
                            onUnlockWithCode(theme)
                            closeCodeCard()
                        } else {
                            showCodeError = true
                        }
                    },
                    onClose = ::closeCodeCard,
                    modifier = Modifier.consumeTaps(),
                )
            }
        }
    }
}

/**
 * One picker tile - the theme's art and name, or a "Coming soon" tile when [theme] is null.
 * A [locked] tile is dimmed and says how long a streak unlocks it.
 */
@Composable
private fun ThemeTile(
    theme: BackgroundTheme?,
    selected: Boolean,
    locked: Boolean,
    onClick: () -> Unit,
) {
    val colors = FocusTheme.colors

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.72f)
                .clip(TileShape)
                .background(colors.onPrimaryAction)
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
                if (locked) LockedOverlay(unlockStreakDays = theme.unlockStreakDays)
            } else {
                Text(
                    text = "Coming\nsoon",
                    style = FocusTheme.typography.caption,
                    color = colors.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
        Text(
            text = theme?.name.orEmpty(),
            style = FocusTheme.typography.listLabel,
            color = when {
                selected -> colors.accent
                locked -> colors.onSurfaceMuted
                else -> colors.onSurface
            },
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .height(16.dp),
        )
    }
}

/** Dims a locked theme's art and says how long a daily streak unlocks it. */
@Composable
private fun LockedOverlay(unlockStreakDays: Int) {
    val colors = FocusTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background.copy(alpha = 0.75f)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(imageVector = Icons.Filled.Lock, contentDescription = "Locked", tint = colors.onSurface)
        Text(
            text = "$unlockStreakDays-day streak\nto unlock",
            style = FocusTheme.typography.caption,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun ThemePickerScreenPreview() {
    FocusAppTheme {
        // No streak yet: Forest is free, Valley is still locked.
        ThemePickerScreen(selectedId = BackgroundThemes.Scene.id, bestStreakDays = 0, onSelect = {}, onClose = {})
    }
}