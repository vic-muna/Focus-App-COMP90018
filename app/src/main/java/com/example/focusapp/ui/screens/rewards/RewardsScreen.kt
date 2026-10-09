package com.example.focusapp.ui.screens.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.DailyMilestone
import com.example.focusapp.domain.model.FocusMusics
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.model.formatMinutes
import com.example.focusapp.ui.components.bar.CloseTopBar
import com.example.focusapp.ui.components.button.FocusPillButton
import com.example.focusapp.ui.components.card.FocusConfirmDialog
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

/** Something the shop sells: a background or a focus music track. */
private data class ShopItem(val id: String, val name: String, val intro: String, val pricePoints: Int)

/**
 * Rewards, opened from the dashboard's trophy:
 *  - points: 1 for every 10 minutes of focus, spent in the shop below
 *  - today's focus-time milestones (back to zero at midnight)
 *  - the shop: backgrounds and focus music bought with points
 */
@Composable
fun RewardsScreen(
    onClose: () -> Unit,
    viewModel: RewardsViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    var itemToBuy by remember { mutableStateOf<ShopItem?>(null) }

    // Re-read every time the screen comes back - it may be a new day by now.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.load() }
    }

    itemToBuy?.let { item ->
        FocusConfirmDialog(
            title = "Buy \"${item.name}\"?",
            message = "This uses ${item.pricePoints} of your ${state?.progress?.points ?: 0} points.",
            confirmLabel = "Buy",
            onConfirm = {
                viewModel.buy(item.id, item.pricePoints)
                itemToBuy = null
            },
            onDismiss = { itemToBuy = null },
        )
    }

    RewardsContent(
        state = state,
        onBuyClick = { itemToBuy = it },
        onUseMusicClick = { id -> viewModel.selectMusic(FocusMusics.byId(id)) },
        onClose = onClose,
    )
}

/** Stateless layout of [RewardsScreen]. [state] is null while loading. */
@Composable
private fun RewardsContent(
    state: RewardsState?,
    onBuyClick: (ShopItem) -> Unit,
    onUseMusicClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val colors = FocusTheme.colors

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(start = 32.dp, end = 32.dp, top = FocusSpacing.ScreenTop, bottom = FocusSpacing.ScreenBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Same height as the X in the top-right corner, so the title lines up with it.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = "Rewards", style = FocusTheme.typography.primaryActionLabel, color = colors.onSurface)
            }

            if (state == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else {
                PointsCard(state.progress)
                MilestonesCard(state.progress)
                ShopCard(
                    title = "Backgrounds",
                    items = BackgroundThemes.all.filter { it.pricePoints > 0 }
                        .map { ShopItem(it.id, it.name, it.intro, it.pricePoints) },
                    state = state,
                    onBuyClick = onBuyClick,
                )
                ShopCard(
                    title = "Focus music",
                    items = FocusMusics.all.map { ShopItem(it.id, it.name, it.intro, it.pricePoints) },
                    state = state,
                    onBuyClick = onBuyClick,
                    selectedId = state.selectedMusicId ?: FocusMusics.default.id,
                    onUseClick = onUseMusicClick,
                )
            }
        }

        CloseTopBar(contentDescription = "Close rewards", onCloseClick = onClose, alignment = Alignment.TopEnd)
    }
}

@Composable
private fun PointsCard(progress: RewardProgress) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    RewardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Stars,
                contentDescription = null,
                tint = if (progress.points > 0) colors.accent else colors.onSurfaceMuted,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(text = formatPoints(progress.points), style = typography.statValue, color = colors.onSurface)
                Text(text = "1 point for every 10 minutes of focus", style = typography.caption, color = colors.onSurfaceMuted)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Total focus time", style = typography.listLabel, color = colors.onSurface)
            Text(text = formatMinutes(progress.totalFocusMinutes), style = typography.listLabel, color = colors.onSurface)
        }
    }
}

@Composable
private fun MilestonesCard(progress: RewardProgress) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val next = progress.nextMilestone

    RewardCard {
        Text(text = "Today's milestones", style = typography.tileTitle, color = colors.onSurface)
        Text(text = "Resets at midnight", style = typography.caption, color = colors.onSurfaceMuted)
        Spacer(Modifier.height(12.dp))
        Text(text = formatMinutes(progress.todayFocusMinutes), style = typography.statValue, color = colors.onSurface)
        Text(text = "focused today", style = typography.caption, color = colors.onSurfaceMuted)
        Spacer(Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            progress.milestones.forEach { milestone ->
                MilestoneRow(
                    milestone = milestone,
                    toGoLabel = if (milestone == next) {
                        "${formatMinutes(milestone.minutes - progress.todayFocusMinutes)} to go"
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/**
 * A shop section: each item shows a Buy button with its price until it's owned.
 * With [onUseClick], owned items can be picked too ([selectedId] is the one in use).
 */
@Composable
private fun ShopCard(
    title: String,
    items: List<ShopItem>,
    state: RewardsState,
    onBuyClick: (ShopItem) -> Unit,
    selectedId: String? = null,
    onUseClick: ((String) -> Unit)? = null,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    RewardCard {
        Text(text = title, style = typography.tileTitle, color = colors.onSurface)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items.forEach { item ->
                val owned = item.pricePoints == 0 || item.id in state.ownedIds
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = item.name, style = typography.body, color = colors.onSurface)
                        Text(text = item.intro, style = typography.caption, color = colors.onSurfaceMuted)
                    }
                    Spacer(Modifier.width(8.dp))
                    when {
                        !owned -> FocusPillButton(
                            label = formatPoints(item.pricePoints.toLong()),
                            containerColor = colors.accent,
                            enabled = state.progress.points >= item.pricePoints,
                            onClick = { onBuyClick(item) },
                        )
                        onUseClick == null -> Text(text = "Owned", style = typography.caption, color = colors.accent)
                        item.id == selectedId -> Text(text = "In use", style = typography.caption, color = colors.accent)
                        else -> FocusPillButton(
                            label = "Use",
                            containerColor = colors.surfaceSunken,
                            contentColor = colors.onSurface,
                            onClick = { onUseClick(item.id) },
                        )
                    }
                }
            }
        }
    }
}

/** One milestone: a ticked accent circle once reached; [toGoLabel] says how far off the next one is. */
@Composable
private fun MilestoneRow(milestone: DailyMilestone, toGoLabel: String?) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .then(
                    if (milestone.reached) Modifier.background(colors.accent)
                    else Modifier.border(2.dp, colors.onSurfaceMuted, CircleShape)
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (milestone.reached) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.onPrimaryAction,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = formatMilestone(milestone.minutes),
            style = typography.body,
            color = if (milestone.reached || toGoLabel != null) colors.onSurface else colors.onSurfaceMuted,
            modifier = Modifier.weight(1f),
        )
        when {
            milestone.reached -> Text(text = "Reached", style = typography.caption, color = colors.accent)
            toGoLabel != null -> Text(
                text = toGoLabel,
                style = typography.caption,
                color = colors.onSurface,
            )
        }
    }
}

/** A rounded surface card, like the dashboard's. */
@Composable
private fun RewardCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(FocusTheme.colors.surface)
            .padding(20.dp),
    ) {
        content()
    }
}

/** "1 pt", "120 pts". */
private fun formatPoints(points: Long): String = if (points == 1L) "1 pt" else "$points pts"

/** "5 minutes", "1 hour", "10 hours". */
private fun formatMilestone(minutes: Int): String = when {
    minutes < 60 -> "$minutes minutes"
    minutes == 60 -> "1 hour"
    else -> "${minutes / 60} hours"
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun RewardsContentPreview() {
    FocusAppTheme {
        RewardsContent(
            state = RewardsState(
                progress = RewardProgress(
                    todayFocusMinutes = 80,
                    milestones = listOf(5, 30, 60, 300, 600).map { DailyMilestone(it, reached = 80 >= it) },
                    totalFocusMinutes = 420,
                    points = 180,
                ),
                ownedIds = setOf(FocusMusics.default.id),
                selectedMusicId = FocusMusics.default.id,
            ),
            onBuyClick = {},
            onUseMusicClick = {},
            onClose = {},
        )
    }
}
