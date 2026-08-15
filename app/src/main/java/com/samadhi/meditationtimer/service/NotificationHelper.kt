package com.samadhi.meditationtimer.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.samadhi.meditationtimer.MainActivity
import com.samadhi.meditationtimer.R
import java.util.Locale

class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_timer_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.notification_channel_timer_desc)
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildTimerNotification(
        timeText: String,
        subText: String,
        isPaused: Boolean,
        isOverrun: Boolean
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

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(timeText)
            .setSubText(subText)
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
            .build()
    }

    companion object {
        const val CHANNEL_ID = "meditation_timer_channel"
        const val NOTIFICATION_ID = 1001

        fun formatSeconds(totalSeconds: Int): String {
            val mins = totalSeconds / 60
            val secs = totalSeconds % 60
            return String.format(Locale.getDefault(), "%02d:%02d", mins, secs)
        }
    }
}
