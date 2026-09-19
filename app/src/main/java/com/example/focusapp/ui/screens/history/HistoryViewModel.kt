package com.example.focusapp.ui.screens.history

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.focusapp.data.repository.FocusRepositoryProvider
import com.example.focusapp.domain.model.FocusSession
import com.example.focusapp.domain.repository.FocusRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * HistoryViewModel
 * ------------------
 * Loads past focus sessions via FocusRepository.getSessionHistory() (see
 * domain/repository/FocusRepository.kt - backed by RoomLocalDataSource in
 * this branch) and exposes them as a StateFlow for [HistoryScreen] to
 * observe and render in a scrollable list.
 *
 * Also exposes [weekBuckets] - "this week" and "last week" as two separate
 * [WeekBucket]s (each carrying that week's actual sessions, not just a
 * count), using FocusRepository.getSessionsBetween() (the
 * Innovation-criterion time-interval query). This is a rolling 7-day
 * window ending "now", not a calendar week (Mon-Sun etc.) - simpler, and
 * avoids locale-dependent "first day of week" questions. See
 * [loadWeekBuckets] for the exact boundaries.
 *
 * A [WeekBucket]'s sessions can be further split into [DayBucket]s via
 * [groupedByDay] - that's what HistoryScreen shows when a week card is
 * tapped, so "this week" isn't just one aggregate number but drills down
 * to real per-day history.
 *
 * Extends AndroidViewModel rather than plain ViewModel purely to get hold
 * of a Context for FocusRepositoryProvider.get(context) - this class has
 * no other Android/lifecycle dependency. The default ViewModelProvider
 * factory Compose's viewModel() already uses (see HistoryScreen.kt) knows
 * how to construct an AndroidViewModel automatically, so no extra
 * ViewModelProvider.Factory wiring is needed here.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val _sessions = MutableStateFlow<List<FocusSession>>(emptyList())

    /** Every past focus session, as last loaded from FocusRepository - unfiltered by week. */
    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()

    private val _isLoading = MutableStateFlow(true)

    /** True while a load is in flight - lets the UI show a spinner instead of an empty state. */
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _weekBuckets = MutableStateFlow<List<WeekBucket>>(emptyList())

    /** Always exactly two entries, in order: "This week", "Last week" - see [loadWeekBuckets]. */
    val weekBuckets: StateFlow<List<WeekBucket>> = _weekBuckets.asStateFlow()

    init {
        loadSessions()
    }

    /** (Re)loads session history + the weekly buckets from FocusRepository. Safe to call again, e.g. on pull-to-refresh. */
    fun loadSessions() {
        viewModelScope.launch {
            _isLoading.value = true
            val repository = FocusRepositoryProvider.get(getApplication())
            _sessions.value = repository.getSessionHistory()
            _weekBuckets.value = loadWeekBuckets(repository)
            _isLoading.value = false
        }
    }

    private suspend fun loadWeekBuckets(repository: FocusRepository): List<WeekBucket> {
        val now = System.currentTimeMillis()
        val oneWeekMillis = TimeUnit.DAYS.toMillis(7)

        // Non-overlapping by construction: lastWeek's upper bound is one millisecond
        // below thisWeek's lower bound, even though getSessionsBetween() is inclusive
        // on both ends - otherwise a session starting at exactly `now - oneWeekMillis`
        // would (in theory) get counted in both windows.
        val thisWeek = repository.getSessionsBetween(fromMillis = now - oneWeekMillis, toMillis = now)
        val lastWeek = repository.getSessionsBetween(
            fromMillis = now - 2 * oneWeekMillis,
            toMillis = now - oneWeekMillis - 1
        )

        return listOf(
            WeekBucket(label = "This week", sessions = thisWeek),
            WeekBucket(label = "Last week", sessions = lastWeek)
        )
    }
}

/** endTimeMillis is null for a session still in progress - treated as 0 minutes here rather than
 *  skipped, since an in-progress session shouldn't normally reach History anyway (see HistoryScreen.kt). */
fun FocusSession.durationMinutes(): Long {
    val end = endTimeMillis ?: return 0L
    return TimeUnit.MILLISECONDS.toMinutes(end - startTimeMillis)
}

/** One rolling week ("This week" / "Last week") and the sessions that fall inside it. */
data class WeekBucket(
    val label: String,
    val sessions: List<FocusSession>
) {
    val sessionCount: Int get() = sessions.size
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes() }
}

/** One calendar day (device's default timezone) within a [WeekBucket] - see [groupedByDay]. */
data class DayBucket(
    val dayStartMillis: Long,
    val sessions: List<FocusSession>
) {
    val totalMinutes: Long get() = sessions.sumOf { it.durationMinutes() }
}

/**
 * Groups a week's sessions by *calendar* day (device's default timezone, truncated to
 * midnight) - not just a rolling 24h bucket, so "Monday" always means the same thing a user
 * would expect regardless of what time of day they open History. Newest day first; sessions
 * within a day newest first too, matching getSessionHistory()'s own ordering convention.
 * Only days that actually have a session appear - no empty placeholder rows for quiet days.
 */
fun List<FocusSession>.groupedByDay(): List<DayBucket> {
    val calendar = Calendar.getInstance()
    fun startOfDay(millis: Long): Long {
        calendar.timeInMillis = millis
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    return this
        .sortedByDescending { it.startTimeMillis }
        .groupBy { startOfDay(it.startTimeMillis) }
        .toSortedMap(compareByDescending<Long> { it })
        .map { (dayStartMillis, daySessions) -> DayBucket(dayStartMillis, daySessions) }
}
