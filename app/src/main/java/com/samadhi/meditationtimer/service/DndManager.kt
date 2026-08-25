package com.samadhi.meditationtimer.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

class DndManager(private val context: Context) {
    private val notificationManager: NotificationManager? =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private var previousInterruptionFilter: Int? = null

    val isDndAccessGranted: Boolean
        get() = notificationManager?.isNotificationPolicyAccessGranted == true

    fun requestDndPermissionIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Enables Do Not Disturb (Priority mode) and saves the previous state for restoration.
     */
    fun enableDnd(): Boolean {
        if (!isDndAccessGranted || notificationManager == null) {
            Log.w(TAG, "Cannot enable DND: Permission not granted or NotificationManager is null")
            return false
        }

        try {
            if (previousInterruptionFilter == null) {
                previousInterruptionFilter = notificationManager.currentInterruptionFilter
            }
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND engaged (INTERRUPTION_FILTER_PRIORITY). Original was: $previousInterruptionFilter")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error setting DND interruption filter", e)
            return false
        }
    }

    /**
     * Restores the original Do Not Disturb filter (or turns DND completely off if previous was unknown).
     */
    fun restoreDnd(): Boolean {
        if (!isDndAccessGranted || notificationManager == null) {
            previousInterruptionFilter = null
            return false
        }

        val targetFilter = previousInterruptionFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        try {
            notificationManager.setInterruptionFilter(targetFilter)
            Log.d(TAG, "Restored DND filter to: $targetFilter (previous was: $previousInterruptionFilter)")
            previousInterruptionFilter = null
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring DND filter", e)
            return false
        }
    }

    /**
     * Explicitly disables Do Not Disturb mode on the system (allows all interruptions).
     */
    fun turnOffDnd(): Boolean {
        if (!isDndAccessGranted || notificationManager == null) return false
        return try {
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            previousInterruptionFilter = null
            Log.d(TAG, "Explicitly turned off DND (INTERRUPTION_FILTER_ALL)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error explicitly turning off DND", e)
            false
        }
    }

    companion object {
        private const val TAG = "DndManager"
    }
}
