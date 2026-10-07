package com.samadhi.meditationtimer.service.screentime

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.launch

/**
 * Keeps the persistent screen time notification fresh: refreshes and starts
 * the minute ticker on unlock, stops the ticker on screen off, and re-posts
 * after reboot or an app update when the user has opted in.
 */
class ScreenTimeNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        ScreenTimeNotification.scope.launch {
            try {
                ScreenTimeNotification.handleAction(context, intent.action)
            } finally {
                pending.finish()
            }
        }
    }
}
