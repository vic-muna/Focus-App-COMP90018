package com.example.focusapp.data.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.focusapp.MainActivity
import com.example.focusapp.R
import com.example.focusapp.data.apps.getAppIcon
import com.example.focusapp.data.blocking.BlockedAppGroup
import com.example.focusapp.data.blocking.ClockTime
import com.example.focusapp.data.preferences.BackgroundThemeStorage
import com.example.focusapp.data.usagestats.AppWindowUsage
import com.example.focusapp.ui.theme.BackgroundThemes

/**
 * The status-bar card shown while a Time Focus slot is on:
 *  - background: a block in the picked theme's colour
 *  - the slot's name and time, and the icons of the apps it limits
 *  - the last opened limited app and how many opens / minutes it has left
 * Tapping it opens that slot's summary card in the Time Focus tab.
 * Updated by FocusAccessibilityService on every app switch.
 */
object TimeFocusNotification {

    /** MainActivity reads this extra to open a time slot's summary. */
    const val EXTRA_OPEN_TIME_SLOT = "open_time_slot"

    private const val CHANNEL_ID = "time_focus"
    private const val ICON_SIZE_PX = 64

    private val iconViewIds = listOf(
        R.id.app_icon_0, R.id.app_icon_1, R.id.app_icon_2, R.id.app_icon_3, R.id.app_icon_4,
    )

    /**
     * Shows (or updates) the card for [group].
     * [usage] is today's usage inside the slot; [lastOpenedPackage] is the app opened most recently.
     */
    @SuppressLint("MissingPermission") // Checked with areNotificationsEnabled() below.
    fun show(
        context: Context,
        group: BlockedAppGroup,
        usage: Map<String, AppWindowUsage>,
        lastOpenedPackage: String?,
        windowEndMillis: Long,
    ) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        createChannel(context)

        val cardColor = BackgroundThemes.byId(BackgroundThemeStorage(context).getSelectedId()).timeFocusColor.toArgb()
        val title = group.name
        val timeRange = "${clock(group.schedule.start)} - ${clock(group.schedule.end)}"
        val lastOpenedText = lastOpenedText(group, usage, lastOpenedPackage)

        // Collapsed card
        val small = RemoteViews(context.packageName, R.layout.notification_time_focus_small)
        small.setInt(R.id.card, "setBackgroundColor", cardColor)
        small.setTextViewText(R.id.title, "$title  ·  $timeRange")
        small.setTextViewText(R.id.last_opened, lastOpenedText)

        // Expanded card
        val big = RemoteViews(context.packageName, R.layout.notification_time_focus)
        big.setInt(R.id.card, "setBackgroundColor", cardColor)
        big.setTextViewText(R.id.title, title)
        big.setTextViewText(R.id.time_range, timeRange)
        big.setTextViewText(R.id.last_opened, lastOpenedText)
        iconViewIds.forEachIndexed { index, viewId ->
            val app = group.apps.getOrNull(index)
            val icon = app?.let { getAppIcon(context, it.packageName) }
            if (icon != null) {
                big.setImageViewBitmap(viewId, Bitmap.createScaledBitmap(icon, ICON_SIZE_PX, ICON_SIZE_PX, true))
                big.setViewVisibility(viewId, View.VISIBLE)
            } else {
                big.setViewVisibility(viewId, View.GONE)
            }
        }
        val moreCount = group.apps.size - iconViewIds.size
        big.setTextViewText(R.id.more_apps, if (moreCount > 0) "+$moreCount" else "")

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(big)
            .setContentIntent(openSlotIntent(context, group.id))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Goes away by itself when the time slot ends.
            .setTimeoutAfter((windowEndMillis - System.currentTimeMillis()).coerceAtLeast(1_000L))
            .build()
        manager.notify(notificationId(group.id), notification)
    }

    /** Removes the card for the slot with [groupId] (e.g. the slot ended or was switched off). */
    fun cancel(context: Context, groupId: String) {
        NotificationManagerCompat.from(context).cancel(notificationId(groupId))
    }

    /** e.g. "Last opened: Instagram · 2 opens left · 12 min left". */
    private fun lastOpenedText(group: BlockedAppGroup, usage: Map<String, AppWindowUsage>, lastOpenedPackage: String?): String {
        val app = group.apps.find { it.packageName == lastOpenedPackage }
            ?: return "No limited app opened yet"
        val appUsage = usage[app.packageName]
        val parts = mutableListOf("Last opened: ${app.name}")
        group.maxOpensPerApp?.let { maxOpens ->
            val left = (maxOpens - (appUsage?.openCount ?: 0)).coerceAtLeast(0)
            parts += if (left == 1) "1 open left" else "$left opens left"
        }
        group.maxMinutesPerApp?.let { maxMinutes ->
            val usedMinutes = ((appUsage?.foregroundMillis ?: 0L) / 60_000L).toInt()
            parts += "${(maxMinutes - usedMinutes).coerceAtLeast(0)} min left"
        }
        return parts.joinToString("  ·  ")
    }

    private fun clock(time: ClockTime) = "%02d:%02d".format(time.hour, time.minute)

    /** One notification per time slot. */
    private fun notificationId(groupId: String) = 2_000 + (groupId.hashCode() and 0xFFFF)

    /** Opens the app on the Time Focus tab, showing this slot's summary card. */
    private fun openSlotIntent(context: Context, groupId: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_TIME_SLOT, groupId)
        }
        return PendingIntent.getActivity(
            context,
            notificationId(groupId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Time Focus", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the time slot that is on and the apps it limits"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
