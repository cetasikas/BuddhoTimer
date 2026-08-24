package com.samadhi.meditationtimer.service.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar

object MeditationReminderScheduler {

    private const val TAG = "ReminderScheduler"
    private const val REQUEST_CODE_REMINDER = 2000
    private const val REQUEST_CODE_RANDOM_1 = 2001 // Morning slot
    private const val REQUEST_CODE_RANDOM_2 = 2002 // Afternoon slot
    private const val REQUEST_CODE_RANDOM_3 = 2003 // Evening slot

    fun scheduleDailyReminder(context: Context, hourOfDay: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, MeditationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled daily reminder for: ${calendar.time}")
        } catch (e: SecurityException) {
            // Fallback for Android 12+ if exact alarm permission is restricted
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.w(TAG, "Exact alarm restricted, set inexact reminder for: ${calendar.time}", e)
        }
    }

    fun scheduleIntervalReminder(context: Context, intervalHours: Int) {
        if (intervalHours <= 0) {
            cancelReminder(context)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, MeditationReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val triggerTimeMs = System.currentTimeMillis() + (intervalHours * 3600 * 1000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTimeMs,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled interval reminder for every $intervalHours hours")
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling interval reminder", e)
        }
    }

    fun scheduleRandomDailyReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // 3 Distinct Waking Slots across the day:
        // Slot 1: Morning (8:00 AM - 11:59 AM)
        // Slot 2: Afternoon (12:00 PM - 4:59 PM)
        // Slot 3: Evening (5:00 PM - 9:00 PM)
        val slots = listOf(
            Triple(REQUEST_CODE_RANDOM_1, 8..11, 0..59),
            Triple(REQUEST_CODE_RANDOM_2, 12..16, 0..59),
            Triple(REQUEST_CODE_RANDOM_3, 17..20, 0..59)
        )

        val now = System.currentTimeMillis()

        for ((requestCode, hourRange, minuteRange) in slots) {
            val randHour = hourRange.random()
            val randMinute = minuteRange.random()

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, randHour)
                set(Calendar.MINUTE, randMinute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            val intent = Intent(context, MeditationReminderReceiver::class.java).apply {
                putExtra("extra_slot_code", requestCode)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Scheduled random reminder slot $requestCode for: ${calendar.time}")
            } catch (e: SecurityException) {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
                Log.w(TAG, "Exact alarm restricted, set inexact random reminder for: ${calendar.time}", e)
            }
        }
    }

    fun cancelReminder(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val allCodes = listOf(REQUEST_CODE_REMINDER, REQUEST_CODE_RANDOM_1, REQUEST_CODE_RANDOM_2, REQUEST_CODE_RANDOM_3)
        for (code in allCodes) {
            val intent = Intent(context, MeditationReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                code,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
        Log.d(TAG, "All meditation reminders cancelled")
    }

    fun rescheduleNextIfNeeded(context: Context) {
        val prefs = context.getSharedPreferences("timer_prefs", Context.MODE_PRIVATE)
        val reminderEnabled = prefs.getBoolean("daily_reminder_enabled", false)
        if (!reminderEnabled) return

        val reminderMode = prefs.getString("reminder_mode", "daily") ?: "daily"
        when (reminderMode) {
            "random" -> {
                scheduleRandomDailyReminder(context)
            }
            "interval" -> {
                val intervalHours = prefs.getInt("reminder_interval_hours", 4)
                scheduleIntervalReminder(context, intervalHours)
            }
            else -> {
                val hour = prefs.getInt("reminder_hour", 8)
                val minute = prefs.getInt("reminder_minute", 0)
                scheduleDailyReminder(context, hour, minute)
            }
        }
    }
}
