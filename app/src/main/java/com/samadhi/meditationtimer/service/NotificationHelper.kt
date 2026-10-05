package com.samadhi.meditationtimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.core.app.NotificationCompat
import com.samadhi.meditationtimer.MainActivity
import com.samadhi.meditationtimer.R
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val timerChannel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_timer_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = context.getString(R.string.notification_channel_timer_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(timerChannel)

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDERS_ID,
                "Meditation Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily and periodic gentle reminders to pause and meditate"
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val timeAnchorChannel = NotificationChannel(
                CHANNEL_TIME_ANCHOR_ID,
                "Time Anchors",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Gentle hourly nudges to keep you grounded in the present moment"
                setShowBadge(false)
                enableLights(false)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(timeAnchorChannel)

            val completionChannel = NotificationChannel(
                CHANNEL_COMPLETION_ID,
                "Session Completion",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when a meditation or breathing session completes"
                setShowBadge(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(completionChannel)
        }
    }

    fun buildReminderNotification(title: String, message: String): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1002,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_REMINDERS_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF26A69A.toInt())
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    fun showReminderNotification(title: String, message: String) {
        val notification = buildReminderNotification(title, message)
        notificationManager.notify(NOTIFICATION_REMINDER_ID, notification)
    }

    fun buildTimerNotification(
        contentText: String,
        isPaused: Boolean,
        isOverrun: Boolean,
        chronoBaseMs: Long = System.currentTimeMillis(),
        isCountdown: Boolean = false,
        staticTimeText: String = "",
    ): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause / Resume Action
        val toggleActionIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = if (isPaused) MeditationTimerService.ACTION_RESUME else MeditationTimerService.ACTION_PAUSE
        }
        val toggleActionPendingIntent = PendingIntent.getService(
            context,
            1,
            toggleActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val toggleActionTitle = if (isPaused) {
            context.getString(R.string.notification_action_resume)
        } else {
            context.getString(R.string.notification_action_pause)
        }

        // Finish Action
        val finishActionIntent = Intent(context, MeditationTimerService::class.java).apply {
            action = MeditationTimerService.ACTION_FINISH
        }
        val finishActionPendingIntent = PendingIntent.getService(
            context,
            2,
            finishActionIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when {
            isPaused -> context.getString(R.string.notification_title_paused)
            isOverrun -> context.getString(R.string.notification_title_overrun)
            else -> context.getString(R.string.notification_title_meditating)
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (isPaused && staticTimeText.isNotEmpty()) "Paused • $staticTimeText" else contentText)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setColor(if (isOverrun) 0xFF58A6FF.toInt() else 0xFFE5A93B.toInt())
            .addAction(android.R.drawable.ic_media_pause, toggleActionTitle, toggleActionPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.notification_action_finish), finishActionPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (isPaused) {
            // Paused: show static time in body; chronometer off
            builder
                .setUsesChronometer(false)
                .setShowWhen(false)
        } else {
            // Active: chronometer self-updates; setWhen anchors reference time
            builder
                .setWhen(chronoBaseMs)
                .setUsesChronometer(true)
                .setChronometerCountDown(isCountdown)
                .setShowWhen(true)
        }

        // Android 16+ (API 36+) Promoted Ongoing Notification (Live Activity / Status Bar Chip)
        if (Build.VERSION.SDK_INT >= 36) {
            builder.addExtras(Bundle().apply {
                putBoolean("android.requestPromotedOngoing", true)
            })
        }

        return builder.build()
    }

    fun showCompletionNotification(title: String, message: String) {
        // If the user is actively viewing the app (e.g. on the overview/summary screen),
        // do not post a redundant status-bar notification.
        if (com.samadhi.meditationtimer.MeditationApp.isAppInForeground) {
            return
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            1004,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_COMPLETION_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setColor(0xFF26A69A.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(NOTIFICATION_COMPLETION_ID, notification)
    }

    fun showBreathingPausedNotification(
        patternName: String,
        elapsedSec: Int
    ) {
        val resumeIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val resumePendingIntent = PendingIntent.getActivity(
            context,
            2001,
            resumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("🪷 Box Breathing Paused")
            .setContentText("$patternName • ${formatSeconds(elapsedSec)} completed")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(resumePendingIntent)
            .setAutoCancel(true)
            .setOngoing(false)
            .setColor(0xFFD4AF37.toInt())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                R.drawable.ic_notification,
                "▶ Resume Session",
                resumePendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_BREATHING_ID, notification)
    }

    fun clearBreathingNotification() {
        notificationManager.cancel(NOTIFICATION_BREATHING_ID)
    }

    fun clearTimerNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun clearCompletionNotification() {
        notificationManager.cancel(NOTIFICATION_COMPLETION_ID)
    }

    fun clearAllTimerNotifications() {
        notificationManager.cancel(NOTIFICATION_ID)
        notificationManager.cancel(NOTIFICATION_COMPLETION_ID)
        notificationManager.cancel(NOTIFICATION_BREATHING_ID)
    }

    fun showTimeAnchorNotification(message: String) {
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            3001,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_TIME_ANCHOR_ID)
            .setContentTitle("🌿 Time Anchor")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .setColor(0xFFE5A93B.toInt())
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        notificationManager.notify(NOTIFICATION_TIME_ANCHOR_ID, notification)
    }

    companion object {
        const val CHANNEL_ID = "meditation_timer_channel_v2"
        const val CHANNEL_REMINDERS_ID = "meditation_reminders_channel"
        const val CHANNEL_COMPLETION_ID = "meditation_completion_channel"
        const val CHANNEL_TIME_ANCHOR_ID = "time_anchor_channel"
        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_REMINDER_ID = 1002
        const val NOTIFICATION_COMPLETION_ID = 1003
        const val NOTIFICATION_BREATHING_ID = 1005
        const val NOTIFICATION_TIME_ANCHOR_ID = 1006

        fun formatSeconds(totalSeconds: Int): String {
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }
}
