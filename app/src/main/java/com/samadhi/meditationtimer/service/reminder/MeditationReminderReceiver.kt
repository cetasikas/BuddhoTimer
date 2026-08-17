package com.samadhi.meditationtimer.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samadhi.meditationtimer.data.wisdom.MeditationGuides
import com.samadhi.meditationtimer.service.NotificationHelper

class MeditationReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val notificationHelper = NotificationHelper(context)
        val quote = MeditationGuides.wisdomQuotes.random()

        val reminderTitles = listOf(
            "Time for Stillness 🪷",
            "A Mindful Pause 🌿",
            "Return to the Present ✨",
            "Your Daily Practice 🧘"
        )
        val title = reminderTitles.random()
        val message = "“${quote.quote}” — ${quote.author}"

        notificationHelper.showReminderNotification(title, message)

        // Reschedule for next day / interval if reminder is enabled
        MeditationReminderScheduler.rescheduleNextIfNeeded(context)
    }
}
