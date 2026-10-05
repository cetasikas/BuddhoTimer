package com.samadhi.meditationtimer.service.screentime

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.samadhi.meditationtimer.service.NotificationHelper
import com.samadhi.meditationtimer.service.UsageStatsHelper
import java.util.Calendar

/**
 * BroadcastReceiver that fires hourly Time Anchor notifications.
 * Inspired by TimeAware's "time anchor" concept — gentle hourly nudges
 * that keep you grounded in the present moment.
 */
class TimeAnchorReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("time_anchor_prefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("time_anchor_enabled", false)
        if (!isEnabled) return

        // Check quiet hours
        val quietStart = prefs.getInt("quiet_hours_start", 22) // 10 PM default
        val quietEnd = prefs.getInt("quiet_hours_end", 7)     // 7 AM default
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        val inQuietHours = if (quietStart > quietEnd) {
            // Wraps midnight: e.g. 22 to 7
            currentHour >= quietStart || currentHour < quietEnd
        } else {
            currentHour in quietStart until quietEnd
        }

        if (inQuietHours) {
            // Reschedule for next interval even during quiet hours
            TimeAnchorScheduler.scheduleNext(context)
            return
        }

        // Build context-aware notification message
        val screenTimeMs = UsageStatsHelper.getTodayScreenTimeMs(context)
        val message = buildAnchorMessage(currentHour, screenTimeMs)

        // Fire the notification
        NotificationHelper(context).showTimeAnchorNotification(message)

        // Reschedule for next interval
        TimeAnchorScheduler.scheduleNext(context)
    }

    private fun buildAnchorMessage(hour: Int, screenTimeMs: Long?): String {
        val timeLabel = formatHour(hour)
        val screenTimeStr = screenTimeMs?.let { UsageStatsHelper.formatDuration(it) }

        return if (screenTimeStr != null) {
            val severity = UsageStatsHelper.getUsageSeverity(screenTimeMs)
            when (severity) {
                0 -> "$timeLabel — $screenTimeStr on screen today. You're doing well. 🌿"
                1 -> "$timeLabel — $screenTimeStr on screen today. A breath costs nothing. 🍃"
                2 -> "$timeLabel — $screenTimeStr on screen. Perhaps time to pause? 🌙"
                else -> "$timeLabel — $screenTimeStr on screen. The still moment awaits. 🪷"
            }
        } else {
            when (hour) {
                in 6..11  -> "$timeLabel — Morning light. Begin with intention. 🌅"
                in 12..14 -> "$timeLabel — Midday. Pause and breathe. 🍃"
                in 15..18 -> "$timeLabel — Afternoon still. A moment of quiet? 🌿"
                in 19..21 -> "$timeLabel — Evening arrives. How was your day? 🌙"
                else      -> "$timeLabel — Late night. Rest is also practice. 🌑"
            }
        }
    }

    private fun formatHour(hour: Int): String {
        val h = if (hour == 0 || hour == 12) 12 else hour % 12
        val amPm = if (hour < 12) "AM" else "PM"
        return "$h:00 $amPm"
    }
}

/**
 * Schedules and cancels hourly Time Anchor alarms.
 */
object TimeAnchorScheduler {

    private const val TAG = "TimeAnchorScheduler"
    private const val REQUEST_CODE = 3000

    fun scheduleNext(context: Context) {
        val prefs = context.getSharedPreferences("time_anchor_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("time_anchor_enabled", false)) return

        val intervalMinutes = prefs.getInt("anchor_interval_minutes", 60)
        scheduleInMinutes(context, intervalMinutes)
    }

    fun scheduleInMinutes(context: Context, intervalMinutes: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TimeAnchorReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerMs = System.currentTimeMillis() + (intervalMinutes * 60_000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            }
            Log.d(TAG, "Time anchor scheduled in $intervalMinutes minutes")
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerMs, pendingIntent)
            Log.w(TAG, "Exact alarm restricted, scheduled inexact time anchor", e)
        }
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, TimeAnchorReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Time anchor cancelled")
    }

    fun rescheduleOnBoot(context: Context) {
        val prefs = context.getSharedPreferences("time_anchor_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("time_anchor_enabled", false)) {
            scheduleNext(context)
        }
    }
}
