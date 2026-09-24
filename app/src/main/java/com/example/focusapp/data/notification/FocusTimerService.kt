package com.example.focusapp.data.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.example.focusapp.MainActivity

private const val CHANNEL_ID = "focus_session_timer"
private const val NOTIFICATION_ID = 1001
private const val ACTION_START = "com.example.focusapp.action.START_FOCUS_TIMER"
private const val ACTION_STOP = "com.example.focusapp.action.STOP_FOCUS_TIMER"
private const val EXTRA_START_TIME_MILLIS = "extra_start_time_millis"

/**
 * FocusTimerService
 * ------------------
 * [Claude, 2026-09-21] Foreground Service whose only job is to keep one
 * persistent, non-dismissable notification-shade entry visible for as
 * long as a focus session is active, showing a live "how long has this
 * session been running" timer - the "notification shade" requested
 * alongside the existing full-screen FocusSessionScreen timer.
 *
 * Deliberately dumb and self-contained: it does NOT compute or own the
 * session's elapsed time (FocusSessionScreen's own tick loop is
 * untouched) - it is only ever handed the session's [EXTRA_START_TIME_MILLIS]
 * and passes that same timestamp to NotificationCompat's built-in
 * setUsesChronometer(true)/setWhen(...), which the OS then ticks on its
 * own every second at zero extra cost to this app. Both timers are just
 * independently reading the same wall-clock start time, so they can't
 * drift apart.
 *
 * Started/stopped from NavGraph.kt's startFocusSession()/onEndSessionClick,
 * alongside (never instead of) the existing AccessibilityBridge blocking
 * calls - this class has no dependency on, and no effect on, the actual
 * app-blocking logic in FocusAccessibilityService/AccessibilityBridge.
 */
class FocusTimerService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null // started, not bound - nothing to bind to

    override fun onCreate() {
        super.onCreate()
        createNotificationChannelIfNeeded()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val startTimeMillis = intent.getLongExtra(EXTRA_START_TIME_MILLIS, System.currentTimeMillis())
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(startTimeMillis),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    private fun buildNotification(startTimeMillis: Long): android.app.Notification {
        val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        val contentPendingIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(com.example.focusapp.R.drawable.ic_notification_timer)
            .setContentTitle("Focus session active")
            .setContentText("Restricted apps are blocked while this session runs")
            .setWhen(startTimeMillis)
            .setUsesChronometer(true)
            .setOngoing(true) // non-dismissable by swipe, for as long as the service is foregrounded
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW) // no sound/heads-up - a persistent status entry, not an alert
            .setContentIntent(contentPendingIntent)
            .build()
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = ContextCompat.getSystemService(this, NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus session timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Persistent timer shown while a focus session is blocking apps"
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        /** Call when a focus session starts - shows the persistent notification. */
        fun start(context: Context, startTimeMillis: Long) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_START_TIME_MILLIS, startTimeMillis)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Call when a focus session ends or is cancelled - removes the persistent notification. */
        fun stop(context: Context) {
            val intent = Intent(context, FocusTimerService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
