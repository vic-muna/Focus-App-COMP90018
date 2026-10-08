package com.example.focusapp.data.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.focusapp.MainActivity
import com.example.focusapp.R

/**
 * Tells a party member the host is waiting for them: the host tried to start the group's focus
 * session, but this phone doesn't have App Blocking (Accessibility) turned on yet. Tapping it
 * opens the app. Shown by PartyModeViewModel from its Firebase listener, so it needs the app
 * to be running (a push that reaches a closed app would need a server to send it).
 */
object PartyNotification {

    private const val CHANNEL_ID = "party_mode"
    private const val NOTIFICATION_ID = 3_001

    @SuppressLint("MissingPermission") // Checked with areNotificationsEnabled() below.
    fun showPermissionNeeded(context: Context) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        createChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openApp = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_timer)
            .setContentTitle("Your group is waiting for you")
            .setContentText("Turn on App Blocking so the host can start focusing together.")
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
    }

    /** Removes it, e.g. once the permission is on or the host stops waiting. */
    fun cancelPermissionNeeded(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(CHANNEL_ID, "Party Mode", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tells you when your group is waiting for you"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }
}
