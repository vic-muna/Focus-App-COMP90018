package com.example.focusapp.ui.screens.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.example.focusapp.data.usagestats.formatUsageDuration
import com.example.focusapp.data.usagestats.hasUsageAccessPermission
import com.example.focusapp.data.usagestats.queryAppUsageInWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

// Same dark palette as GroupListScreen, so the two Blocked-App-Group pages match.
private val ScreenBgColor = Color(0xFF33386D)
private val CardBgColor = Color(0xFF252853)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFA5ABC7)
private val LimitReachedColor = Color(0xFFFF8A80)

/** How often the numbers refresh while this page is on screen. */
private const val USAGE_REFRESH_INTERVAL_MILLIS = 30_000L

/** One app's row on [GroupUsageScreen]. */
private data class AppUsageToday(
    val app: AppItem,
    val openCount: Int,
    val durationMillis: Long
)

/**
 * GroupUsageScreen
 * -------------------
 * [David Shiau, 2026-09-26] Phase 1 of the "daily open times and duration"
 * limit: reached by tapping Home's schedule banner (which used to start a
 * focus session - see HomeScreenWithSheet). Shows, for every app in
 * [group], how many times it has been opened and how long it has been used
 * inside today's scheduled window, next to the group's limits.
 * [David Shiau, 2026-09-26] Display-only: the actual blocking happens in
 * FocusAccessibilityService (see EvaluateUsageLimitUseCase), from the same
 * numbers.
 *
 * Re-queries whenever the page is resumed (e.g. coming back from granting
 * Usage Access in system Settings) and every [USAGE_REFRESH_INTERVAL_MILLIS]
 * while visible.
 */
@Composable
fun GroupUsageScreen(
    group: BlockedAppGroup,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasUsageAccess by remember { mutableStateOf(hasUsageAccessPermission(context)) }
    // null = first load hasn't finished yet.
    var usage by remember { mutableStateOf<List<AppUsageToday>?>(null) }
    // [David Shiau, 2026-09-26] Numbers only count inside today's scheduled
    // window - the same numbers the Scheduled Limits are enforced against
    // (see EvaluateUsageLimitUseCase). null = not scheduled today.
    val window = remember(group.schedule) { group.schedule.windowOn() }

    LaunchedEffect(group, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                hasUsageAccess = hasUsageAccessPermission(context)
                if (hasUsageAccess) {
                    usage = withContext(Dispatchers.Default) {
                        val inWindow = window?.let {
                            queryAppUsageInWindow(context, it.startMillis, minOf(it.endMillis, System.currentTimeMillis()))
                        }.orEmpty()
                        group.apps.map { app ->
                            AppUsageToday(
                                app = app,
                                openCount = inWindow[app.packageName]?.openCount ?: 0,
                                durationMillis = inWindow[app.packageName]?.foregroundMillis ?: 0L
                            )
                        }
                    }
                }
                delay(USAGE_REFRESH_INTERVAL_MILLIS)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBgColor)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(CardBgColor)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(
                    text = group.name,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (window != null) {
                        "Today's usage during ${formatClockTime(group.schedule.start)} - ${formatClockTime(group.schedule.end)}"
                    } else {
                        "Not scheduled today"
                    },
                    fontSize = 14.sp,
                    color = TextSecondary
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        when {
            !hasUsageAccess -> UsageAccessNeeded(
                onGrantClick = { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
            )
            group.apps.isEmpty() -> Text(
                text = "This group has no apps yet.",
                color = TextSecondary
            )
            usage == null -> Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = TextPrimary)
            }
            else -> LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(usage.orEmpty(), key = { it.app.packageName }) {
                    AppUsageRow(it, maxOpens = group.maxOpensPerApp, maxMinutes = group.maxMinutesPerApp)
                }
            }
        }
    }
}

private fun formatClockTime(time: ClockTime): String = "%02d:%02d".format(time.hour, time.minute)

/** Each number shows "/ limit" when that limit is set, e.g. "Opened 2 / 3 times". */
@Composable
private fun AppUsageRow(usage: AppUsageToday, maxOpens: Int?, maxMinutes: Int?) {
    val isOverLimit = (maxOpens != null && usage.openCount >= maxOpens) ||
        (maxMinutes != null && usage.durationMillis >= maxMinutes * 60_000L)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBgColor)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = usage.app.icon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
            )
        } else {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(TextSecondary)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = usage.app.name,
            color = TextPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = if (maxOpens != null) {
                    "Opened ${usage.openCount} / ${formatOpenTimes(maxOpens)}"
                } else {
                    "Opened ${formatOpenTimes(usage.openCount)}"
                },
                color = TextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = formatUsageDuration(usage.durationMillis) +
                    (maxMinutes?.let { " / ${formatLimitMinutes(it)}" } ?: ""),
                color = TextSecondary,
                fontSize = 14.sp
            )
            if (isOverLimit) {
                Text(text = "Limit reached", color = LimitReachedColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/** Shown instead of the list until Usage Access is granted - numbers can't be read without it. */
@Composable
private fun UsageAccessNeeded(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBgColor)
            .padding(16.dp)
    ) {
        Text(
            text = "To show how often and how long you've used these apps today, " +
                "Focus needs the Usage Access permission. Find Focus in the list " +
                "on the Settings screen that opens next and turn it on.",
            color = TextPrimary,
            fontSize = 14.sp
        )
        Spacer(Modifier.height(12.dp))
        Button(onClick = onGrantClick) { Text("Open Settings") }
    }
}
