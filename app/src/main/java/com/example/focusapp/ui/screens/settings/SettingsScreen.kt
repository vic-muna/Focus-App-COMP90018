package com.example.focusapp.ui.screens.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.focusapp.data.apps.getAppLabel
import com.example.focusapp.data.usagestats.hasUsageAccessPermission
import com.example.focusapp.data.usagestats.queryUsageDurationsToday
import com.example.focusapp.ui.theme.WireframeColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * SettingsScreen
 * ----------------
 * The "Settings" tab. Still mostly a placeholder (see the TODO below), but
 * now also hosts a "Check App Usage Duration" button - a real,
 * on-demand call into the OS's [android.app.usage.UsageStatsManager] via
 * [queryUsageDurationsToday], showing today's real per-app usage totals.
 * Display-only for now: tapping it doesn't feed into blocking or anything
 * else, it just answers "how long have I used each app today" the same
 * way a phone's own Digital Wellbeing / Screen Time screen would.
 *
 * [David Shiau, 2026-09-20] Added the "Check App Usage Duration" button and
 * everything below it in this file - previously this screen had no
 * functional content at all.
 *
 * TODO: to be implemented later - actual settings content (notifications,
 * account, language, etc.) once that's designed.
 */
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // null = not checked yet this screen visit; empty = checked, nothing used today.
    var usageResults by remember { mutableStateOf<List<Pair<String, Long>>?>(null) }
    var isLoadingUsage by remember { mutableStateOf(false) }
    var showUsagePermissionDialog by remember { mutableStateOf(false) }

    // [David Shiau, 2026-09-20] Triggers UsageStatsManager (via
    // queryUsageDurationsToday) to fetch and display today's real per-app
    // usage totals - gated on the Usage Access permission first.
    fun checkAppUsageDuration() {
        // Usage access has no runtime permission dialog - the only way to
        // find out if it's granted is to ask AppOpsManager directly (see
        // hasUsageAccessPermission's doc comment), so check first rather
        // than querying and silently getting an empty map back.
        if (!hasUsageAccessPermission(context)) {
            showUsagePermissionDialog = true
            return
        }
        isLoadingUsage = true
        scope.launch {
            val durationsByPackage = withContext(Dispatchers.Default) {
                queryUsageDurationsToday(context)
            }
            usageResults = durationsByPackage.entries
                .sortedByDescending { it.value }
                .map { (packageName, millis) -> getAppLabel(context, packageName) to millis }
            isLoadingUsage = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(WireframeColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(text = "Settings")
        Text(
            text = "No settings options yet - this screen is a placeholder.",
            modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(Modifier.height(24.dp))

        Button(onClick = { checkAppUsageDuration() }) {
            Text("Check App Usage Duration")
        }

        Spacer(Modifier.height(16.dp))

        when {
            isLoadingUsage -> CircularProgressIndicator()
            usageResults != null -> UsageDurationResults(usageResults.orEmpty())
        }
    }

    if (showUsagePermissionDialog) {
        UsageAccessPermissionDialog(
            onConfirm = {
                showUsagePermissionDialog = false
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            },
            onDismiss = { showUsagePermissionDialog = false }
        )
    }
}

/** Today's per-app totals, most-used first - a plain list, since this is display-only. */
@Composable
private fun UsageDurationResults(results: List<Pair<String, Long>>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(WireframeColors.Card, shape = RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(text = "Today's App Usage", color = WireframeColors.OnDark)
        Spacer(Modifier.height(8.dp))

        if (results.isEmpty()) {
            Text(text = "No app usage recorded yet today.", color = WireframeColors.OnDark)
        } else {
            results.forEach { (label, millis) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = label, color = WireframeColors.OnDark, modifier = Modifier.weight(1f))
                    Text(text = formatUsageDuration(millis), color = WireframeColors.OnDark)
                }
            }
        }
    }
}

/** Explains why the Usage Access permission is needed before sending the user to grant it. */
@Composable
private fun UsageAccessPermissionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usage access needed") },
        text = {
            Text(
                "To show today's app usage, Focus needs the Usage Access " +
                    "permission. Find Focus in the list on the Settings " +
                    "screen that opens next and turn it on."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Open Settings") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Turns raw milliseconds into a short "1h 23m" / "45m" / "12s" style string. */
private fun formatUsageDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return when {
        hours > 0 -> "${hours}h ${minutes}m"
        minutes > 0 -> "${minutes}m ${seconds}s"
        else -> "${seconds}s"
    }
}
