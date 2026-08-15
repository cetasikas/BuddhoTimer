package com.samadhi.meditationtimer.service

import android.content.Context
import android.os.PowerManager
import android.util.Log

class WakeLockHelper(private val context: Context) {
    private var wakeLock: PowerManager.WakeLock? = null

    fun acquireWakeLock(tag: String = "SamadhiTimer::WakeLock") {
        if (wakeLock?.isHeld == true) return
        try {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag)?.apply {
                setReferenceCounted(false)
                acquire(4 * 60 * 60 * 1000L) // Max 4 hours safety limit
            }
            Log.d(TAG, "Partial WakeLock acquired")
        } catch (e: Exception) {
            Log.e(TAG, "Error acquiring wake lock", e)
        }
    }

    fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d(TAG, "Partial WakeLock released")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing wake lock", e)
        } finally {
            wakeLock = null
        }
    }

    companion object {
        private const val TAG = "WakeLockHelper"
    }
}
