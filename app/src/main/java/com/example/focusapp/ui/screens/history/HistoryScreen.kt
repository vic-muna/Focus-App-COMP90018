package com.example.focusapp.ui.screens.history

// NOTE: not currently reachable from the real navigation graph - see
// Destinations.kt. Temporarily reachable via a "View Focus History (Debug)"
// row on SettingsScreen for demo purposes only (see SettingsScreen.kt) until
// the team decides where/whether this fits into the real nav.

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.ui.theme.WireframeColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * HistoryScreen
 * ---------------
 * Shows past focus sessions (duration, distracting-app open count, whether it
 * was completed) pulled from the Local Data Source via [HistoryViewModel].
 *
 * "This week" / "Last week" are two separate tappable [WeekCard]s (see
 * [HistoryViewModel.weekBuckets]) rather than one shared summary row -
 * tapping either one opens a [WeekDetailDialogContent] that drills down
 * into that week's individual days (see [groupedByDay]), each showing its
 * own session rows. Below the two week cards is still the full, unfiltered
 * session list, unchanged.
 *
 * Deliberately the "most spartan LazyColumn that works" - plain rows, no
 * icons, no swipe actions, no empty-state illustration. This exists so
 * there's something real on screen to demo (the data flow through
 * FocusRepository -> Room already worked; this screen just wasn't showing
 * it - see HistoryViewModel.kt's doc comment). Kai-Jiun/whoever ends up
 * owning visual polish for this screen should feel free to replace the
 * row Composables below wholesale; [HistoryViewModel]'s public surface
 * (sessions/weekBuckets/isLoading/loadSessions()) is the stable part.
 *
 * @param viewModel loads/exposes session history; see [HistoryViewModel].
 */
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = viewModel()) {
    val sessions by viewModel.sessions.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val weekBuckets by viewModel.weekBuckets.collectAsState()

    // Which week card (if any) is currently open in a detail dialog - null means none.
    var selectedWeek by remember { mutableStateOf<WeekBucket?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .padding(24.dp)
    ) {
        Text(text = "Focus History", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if (!isLoading) {
            WeekCardsRow(weekBuckets = weekBuckets, onWeekClick = { selectedWeek = it })
            Spacer(modifier = Modifier.height(16.dp))
        }

        when {
            isLoading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            sessions.isEmpty() -> Text(
                text = "No session history yet.",
                modifier = Modifier.padding(top = 8.dp)
            )

            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    SessionRow(session)
                }
            }
        }
    }

    val weekToShow = selectedWeek
    if (weekToShow != null) {
        Dialog(onDismissRequest = { selectedWeek = null }) {
            WeekDetailDialogContent(bucket = weekToShow, onDismiss = { selectedWeek = null })
        }
    }
}

/**
 * Two separate tappable cards, one per [WeekBucket] (normally "This week" and "Last week" -
 * see [HistoryViewModel.weekBuckets]) - replaces the old single side-by-side summary row so
 * each week can be tapped independently to drill into its own days.
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(WireframeColors.Card)
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Text(text = bucket.label, color = WireframeColors.OnDark, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "${bucket.sessionCount} sessions · ${bucket.totalMinutes}m",
            color = WireframeColors.OnDark
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap for daily breakdown",
            color = WireframeColors.OnDark,
            style = MaterialTheme.typography.labelSmall
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(WireframeColors.Background)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = bucket.label, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Close",
                color = WireframeColors.OnLight,
                modifier = Modifier.clickable(onClick = onDismiss)
            )
        }

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${bucket.sessionCount} sessions · ${bucket.totalMinutes}m total",
            color = WireframeColors.OnLight,
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.height(16.dp))

        val dayBuckets = bucket.sessions.groupedByDay()
        if (dayBuckets.isEmpty()) {
            Text(text = "No sessions this week.", modifier = Modifier.padding(top = 8.dp))
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
            color = WireframeColors.OnLight,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(modifier = Modifier.height(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            day.sessions.forEach { session -> SessionRow(session) }
        }
    }
}

@Composable
private fun SessionRow(session: FocusSession) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(WireframeColors.Card)
            .padding(16.dp)
    ) {
        Text(
            text = formatStartTime(session.startTimeMillis),
            color = WireframeColors.OnDark,
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Duration: ${formatDuration(session)}",
            color = WireframeColors.OnDark
        )
        Text(
            text = "Distracting app opens: ${session.distractingAppOpenCount}",
            color = WireframeColors.OnDark
        )
        Text(
            text = if (session.wasCompletedSuccessfully) "Completed" else "Not completed",
            color = WireframeColors.OnDark
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
