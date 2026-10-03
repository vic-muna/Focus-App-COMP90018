package com.example.focusapp.ui.screens.rewards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.focusapp.domain.model.RewardProgress
import com.example.focusapp.domain.model.formatMinutes
import com.example.focusapp.ui.components.bar.CloseTopBar
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme

/**
 * Rewards, opened from the dashboard's trophy:
 *  - the daily streak and how close today is to counting for it
 *  - today's focus-time milestones (back to zero at midnight)
 */
@Composable
fun RewardsScreen(
    onClose: () -> Unit,
    viewModel: RewardsViewModel = viewModel(),
) {
    val progress by viewModel.progress.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // Re-read every time the screen comes back - it may be a new day by now.
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.load() }
    }

    RewardsContent(progress = progress, onClose = onClose)
}

/** Stateless layout of [RewardsScreen]. [progress] is null while loading. */
@Composable
private fun RewardsContent(progress: RewardProgress?, onClose: () -> Unit) {
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

            if (progress == null) {
                Box(modifier = Modifier.fillMaxWidth().padding(top = 24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = colors.accent)
                }
            } else {
                StreakCard(progress)
                MilestonesCard(progress)
            }
        }

        CloseTopBar(contentDescription = "Close rewards", onCloseClick = onClose, alignment = Alignment.TopEnd)
    }
}

@Composable
private fun StreakCard(progress: RewardProgress) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val goal = progress.streakGoalMinutes

    RewardCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.LocalFireDepartment,
                contentDescription = null,
                tint = if (progress.currentStreakDays > 0) colors.accent else colors.onSurfaceMuted,
                modifier = Modifier.size(48.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = "${progress.currentStreakDays} ${if (progress.currentStreakDays == 1) "day" else "days"}",
                    style = typography.statValue,
                    color = colors.onSurface,
                )
                Text(text = "Daily streak", style = typography.caption, color = colors.onSurfaceMuted)
            }
        }

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = "Today", style = typography.listLabel, color = colors.onSurface)
            Text(
                text = "${formatMinutes(progress.todayFocusMinutes)} / ${formatMinutes(goal.toLong())}",
                style = typography.listLabel,
                color = colors.onSurface,
            )
        }
        Spacer(Modifier.height(6.dp))
        ProgressBar(fraction = progress.todayFocusMinutes.toFloat() / goal)
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (progress.todayCountsForStreak) {
                "Goal reached - today counts for your streak!"
            } else {
                "Focus ${formatMinutes(goal - progress.todayFocusMinutes)} more today to " +
                    if (progress.currentStreakDays > 0) "keep your streak." else "start a streak."
            },
            style = typography.caption,
            color = if (progress.todayCountsForStreak) colors.accent else colors.onSurfaceMuted,
        )
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
                    minutesToGo = if (milestone == next) milestone.minutes - progress.todayFocusMinutes else null,
                )
            }
        }
    }
}

/** One milestone: a ticked accent circle once reached; the next one to reach says how far off it is. */
@Composable
private fun MilestoneRow(milestone: DailyMilestone, minutesToGo: Long?) {
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
            color = if (milestone.reached || minutesToGo != null) colors.onSurface else colors.onSurfaceMuted,
            modifier = Modifier.weight(1f),
        )
        when {
            milestone.reached -> Text(text = "Reached", style = typography.caption, color = colors.accent)
            minutesToGo != null -> Text(
                text = "${formatMinutes(minutesToGo)} to go",
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

@Composable
private fun ProgressBar(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(FocusTheme.colors.surfaceSunken),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(5.dp))
                .background(FocusTheme.colors.accent),
        )
    }
}

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
            progress = RewardProgress(
                todayFocusMinutes = 80,
                milestones = listOf(5, 30, 60, 300, 600).map { DailyMilestone(it, reached = 80 >= it) },
                currentStreakDays = 3,
            ),
            onClose = {},
        )
    }
}
