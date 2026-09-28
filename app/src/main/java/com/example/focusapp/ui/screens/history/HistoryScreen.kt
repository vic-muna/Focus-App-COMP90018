package com.example.focusapp.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.components.RejectButton
import com.example.focusapp.ui.theme.BackgroundTheme
import com.example.focusapp.ui.theme.BackgroundThemes
import com.example.focusapp.ui.theme.FocusAppTheme
import com.example.focusapp.ui.theme.FocusSpacing
import com.example.focusapp.ui.theme.FocusTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private val BAR_CHART_MAX_BAR_HEIGHT = 100.dp

/**
 * HistoryScreen
 * ---------------
 * The dashboard, reached from Home's illustration. On top, an "ID card" for
 * the current background theme ([ThemeIdCard] - tapping it opens the theme
 * picker via [onChangeThemeClick]); below it, the Focus History pulled from
 * the Local Data Source via [HistoryViewModel]:
 *  - "This week" bar chart (minutes per day)
 *  - "This week" / "Last week" cards - tapping one opens a
 *    [WeekDetailDialogContent] that drills down into that week's days
 *    (see [groupedByDay]), each with its own session rows
 *  - the full, unfiltered session list
 *
 * [HistoryViewModel]'s public surface (sessions/weekBuckets/isLoading/
 * loadSessions()) is unchanged - only the look was redone to the app theme.
 *
 * @param viewModel loads/exposes session history; see [HistoryViewModel].
 */
@Composable
fun HistoryScreen(
    theme: BackgroundTheme,
    onChangeThemeClick: () -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val weekBuckets by viewModel.weekBuckets.collectAsState()

    // Which week card (if any) is currently open in a detail dialog - null means none.
    var selectedWeek by remember { mutableStateOf<WeekBucket?>(null) }

    HistoryContent(
        theme = theme,
        onChangeThemeClick = onChangeThemeClick,
        sessions = sessions,
        isLoading = isLoading,
        weekBuckets = weekBuckets,
        onWeekClick = { selectedWeek = it },
    )

    val weekToShow = selectedWeek
    if (weekToShow != null) {
        Dialog(onDismissRequest = { selectedWeek = null }) {
            WeekDetailDialogContent(bucket = weekToShow, onDismiss = { selectedWeek = null })
        }
    }
}

/** Stateless layout of [HistoryScreen]. */
@Composable
private fun HistoryContent(
    theme: BackgroundTheme,
    onChangeThemeClick: () -> Unit,
    sessions: List<FocusSession>,
    isLoading: Boolean,
    weekBuckets: List<WeekBucket>,
    onWeekClick: (WeekBucket) -> Unit,
) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = 32.dp,
            end = 32.dp,
            top = FocusSpacing.ScreenTop,
            bottom = FocusSpacing.ScreenBottom,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            ThemeIdCard(theme = theme, onChangeClick = onChangeThemeClick)
        }

        item {
            Text(
                text = "Focus History",
                style = typography.primaryActionLabel,
                color = colors.onSurface,
                modifier = Modifier.padding(top = 20.dp),
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.accent)
                }
            }
        } else {
            item {
                Text(text = "This week", style = typography.tileTitle, color = colors.onSurface)
            }
            item {
                WeeklyBarChart(
                    points = sessions.dailyChartPoints(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .padding(16.dp),
                )
            }
            item {
                WeekCardsRow(weekBuckets = weekBuckets, onWeekClick = onWeekClick)
            }
            if (sessions.isEmpty()) {
                item {
                    Text(
                        text = "No session history yet.",
                        style = typography.body,
                        color = colors.onSurfaceMuted,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session)
                }
            }
        }
    }
}

/**
 * Simple bar chart - one bar per [DayChartPoint] (normally exactly 7, oldest to newest -
 * see [dailyChartPoints]), height scaled relative to whichever day in the set has the most
 * minutes. Plain Column/Row/Box, no charting library and no Canvas drawing - matches how
 * everything else on this screen (and this app) is built, and keeps this addition at zero
 * new Gradle dependencies.
 */
@Composable
private fun WeeklyBarChart(points: List<DayChartPoint>, modifier: Modifier = Modifier) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography
    val maxMinutes = (points.maxOfOrNull { it.totalMinutes } ?: 0L).coerceAtLeast(1L)
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val fraction = (point.totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
            val barHeight = (BAR_CHART_MAX_BAR_HEIGHT * fraction).coerceAtLeast(4.dp)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (point.totalMinutes > 0) "${point.totalMinutes}m" else "",
                    color = colors.onSurface,
                    style = typography.microLabel
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(barHeight)
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(if (point.totalMinutes > 0) colors.accent else colors.onSurfaceMuted)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = dayFormat.format(Date(point.dayStartMillis)),
                    color = colors.onSurface,
                    style = typography.microLabel
                )
            }
        }
    }
}

/**
 * Two separate tappable cards, one per [WeekBucket] (normally "This week" and "Last week" -
 * see [HistoryViewModel.weekBuckets]), each tapped independently to drill into its own days.
 */
@Composable
private fun WeekCardsRow(weekBuckets: List<WeekBucket>, onWeekClick: (WeekBucket) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        weekBuckets.forEach { bucket ->
            WeekCard(
                bucket = bucket,
                modifier = Modifier.weight(1f),
                onClick = { onWeekClick(bucket) }
            )
        }
    }
}

@Composable
private fun WeekCard(bucket: WeekBucket, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = bucket.label, color = colors.onSurface, style = typography.tileTitle)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${bucket.sessionCount} sessions · ${bucket.totalMinutes}m",
            color = colors.onSurface,
            style = typography.body
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap for daily breakdown",
            color = colors.onSurfaceMuted,
            style = typography.caption
        )
    }
}

/**
 * Opened when a [WeekCard] is tapped - [bucket]'s sessions split into [DayBucket]s via
 * [groupedByDay], each rendered as a small header (date + that day's count/minutes) followed
 * by its own [SessionRow]s. Scrollable and height-capped so a busy week doesn't push the
 * dialog off the top/bottom of the screen.
 */
@Composable
private fun WeekDetailDialogContent(bucket: WeekBucket, onDismiss: () -> Unit) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = bucket.label, style = typography.cardTitle, color = colors.onSurface)
            RejectButton(onClick = onDismiss)
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${bucket.sessionCount} sessions · ${bucket.totalMinutes}m total",
            color = colors.onSurfaceMuted,
            style = typography.caption
        )
        Spacer(modifier = Modifier.height(16.dp))

        val dayBuckets = bucket.sessions.groupedByDay()
        if (dayBuckets.isEmpty()) {
            Text(
                text = "No sessions this week.",
                style = typography.body,
                color = colors.onSurfaceMuted,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            LazyColumn(
                modifier = Modifier.heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                items(dayBuckets, key = { it.dayStartMillis }) { day ->
                    DaySection(day)
                }
            }
        }
    }
}

@Composable
private fun DaySection(day: DayBucket) {
    Column {
        Text(
            text = "${formatDayHeader(day.dayStartMillis)} · ${day.sessions.size} sessions · ${day.totalMinutes}m",
            color = FocusTheme.colors.onSurface,
            style = FocusTheme.typography.listLabel
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            day.sessions.forEach { session -> SessionRow(session) }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    val colors = FocusTheme.colors
    val typography = FocusTheme.typography

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = formatStartTime(session.startTimeMillis), color = colors.onSurface, style = typography.tileTitle)
            Text(
                text = if (session.wasCompletedSuccessfully) "Completed" else "Not completed",
                color = if (session.wasCompletedSuccessfully) colors.accent else colors.onSurfaceMuted,
                style = typography.caption
            )
        }
        Text(text = "Duration: ${formatDuration(session)}", color = colors.onSurface, style = typography.body)
        Text(
            text = "Distracting app opens: ${session.distractingAppOpenCount}",
            color = colors.onSurface,
            style = typography.body
        )
    }
}

private fun formatStartTime(startTimeMillis: Long): String =
    SimpleDateFormat("EEE d MMM, HH:mm", Locale.getDefault()).format(Date(startTimeMillis))

private fun formatDayHeader(dayStartMillis: Long): String =
    SimpleDateFormat("EEE d MMM", Locale.getDefault()).format(Date(dayStartMillis))

private fun formatDuration(session: FocusSession): String {
    val end = session.endTimeMillis ?: return "in progress"
    val totalMinutes = TimeUnit.MILLISECONDS.toMinutes(end - session.startTimeMillis)
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

private val previewSessions: List<FocusSession> = run {
    val now = System.currentTimeMillis()
    val hour = TimeUnit.HOURS.toMillis(1)
    listOf(
        FocusSession("1", now - 2 * hour, now - hour, distractingAppOpenCount = 2, wasCompletedSuccessfully = true),
        FocusSession("2", now - 26 * hour, now - 25 * hour + 20 * 60_000, wasCompletedSuccessfully = false),
    )
}

@Preview(widthDp = 393, heightDp = 852)
@Composable
private fun HistoryContentPreview() {
    FocusAppTheme {
        HistoryContent(
            theme = BackgroundThemes.Scene,
            onChangeThemeClick = {},
            sessions = previewSessions,
            isLoading = false,
            weekBuckets = listOf(
                WeekBucket(label = "This week", sessions = previewSessions),
                WeekBucket(label = "Last week", sessions = emptyList()),
            ),
            onWeekClick = {},
        )
    }
}