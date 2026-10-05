package com.samadhi.meditationtimer.service.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.samadhi.meditationtimer.service.screentime.TimeAnchorScheduler

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED || intent?.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            MeditationReminderScheduler.rescheduleNextIfNeeded(context)
            TimeAnchorScheduler.rescheduleOnBoot(context)
        }
    }
}
