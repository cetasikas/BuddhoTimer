package com.samadhi.meditationtimer.service

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.provider.Settings
import android.util.Log

class DndManager(private val context: Context) {
    private val notificationManager: NotificationManager? =
        context.applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("buddho_dnd_prefs", Context.MODE_PRIVATE)

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
            val current = notificationManager.currentInterruptionFilter
            // Only save the previous filter if we haven't already saved one
            if (!prefs.contains(KEY_PREVIOUS_FILTER)) {
                prefs.edit().putInt(KEY_PREVIOUS_FILTER, current).apply()
                sPreviousFilter = current
            }
            notificationManager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            Log.d(TAG, "DND engaged (INTERRUPTION_FILTER_PRIORITY). Original was: ${sPreviousFilter ?: current}")
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
            clearSavedFilter()
            return false
        }

        val savedFilter = if (prefs.contains(KEY_PREVIOUS_FILTER)) {
            prefs.getInt(KEY_PREVIOUS_FILTER, NotificationManager.INTERRUPTION_FILTER_ALL)
        } else {
            sPreviousFilter ?: NotificationManager.INTERRUPTION_FILTER_ALL
        }

        try {
            notificationManager.setInterruptionFilter(savedFilter)
            Log.d(TAG, "Restored DND filter to: $savedFilter")
            clearSavedFilter()
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring DND filter", e)
            clearSavedFilter()
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
            clearSavedFilter()
            Log.d(TAG, "Explicitly turned off DND (INTERRUPTION_FILTER_ALL)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error explicitly turning off DND", e)
            clearSavedFilter()
            false
        }
    }

    private fun clearSavedFilter() {
        prefs.edit().remove(KEY_PREVIOUS_FILTER).apply()
        sPreviousFilter = null
    }

    companion object {
        private const val TAG = "DndManager"
        private const val KEY_PREVIOUS_FILTER = "saved_previous_interruption_filter"
        private var sPreviousFilter: Int? = null
    }
}
