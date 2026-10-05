package com.samadhi.meditationtimer.service

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Helper for reading daily screen time via Android UsageStatsManager.
 * Inspired by TimeAware's ambient screen time awareness feature.
 *
 * IMPORTANT: Requires PACKAGE_USAGE_STATS permission, which must be
 * manually granted by the user in Settings → Apps → Special App Access → Usage Access.
 */
object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"

    /**
     * Returns true if the user has granted Usage Access permission.
     */
    fun hasUsageAccessPermission(context: Context): Boolean {
        return try {
            val appOpsManager = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                appOpsManager.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            } else {
                @Suppress("DEPRECATION")
                appOpsManager.checkOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName
                )
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: Exception) {
            Log.e(TAG, "Error checking usage access permission", e)
            false
        }
    }

    /**
     * Opens the system Usage Access settings screen so the user can grant permission.
     */
    fun openUsageAccessSettings(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            })
        } catch (e: Exception) {
            Log.e(TAG, "Could not open Usage Access settings", e)
        }
    }

    /**
     * Gets today's total screen time in milliseconds.
     * Returns null if permission is not granted.
     */
    fun getTodayScreenTimeMs(context: Context): Long? {
        if (!hasUsageAccessPermission(context)) return null

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
            )

            // Sum foreground time across all apps, excluding the launcher itself
            val totalMs = stats
                .filter { it.packageName != context.packageName }
                .sumOf { it.totalTimeInForeground }

            totalMs
        } catch (e: Exception) {
            Log.e(TAG, "Error reading usage stats", e)
            null
        }
    }

    /**
     * Gets today's top apps by screen time usage.
     * Returns a list of (appName, durationMs) pairs, sorted descending.
     * Returns empty list if permission is not granted.
     */
    fun getTopAppsToday(context: Context, limit: Int = 5): List<AppUsage> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager

            val calendar = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val startOfDay = calendar.timeInMillis
            val now = System.currentTimeMillis()

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                startOfDay,
                now
            )

            stats
                .filter { it.packageName != context.packageName && it.totalTimeInForeground > 0 }
                .sortedByDescending { it.totalTimeInForeground }
                .take(limit)
                .mapNotNull { stat ->
                    try {
                        val appInfo = pm.getApplicationInfo(stat.packageName, 0)
                        val appName = pm.getApplicationLabel(appInfo).toString()
                        AppUsage(
                            packageName = stat.packageName,
                            appName = appName,
                            durationMs = stat.totalTimeInForeground
                        )
                    } catch (e: Exception) {
                        null // Skip apps that can't be resolved (e.g. uninstalled)
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading top apps", e)
            emptyList()
        }
    }

    /**
     * Gets daily screen time totals for the last 7 days (today + 6 previous days).
     * Returns a list of DayUsage, from oldest to newest (index 0 = 6 days ago, index 6 = today).
     */
    fun getWeeklyScreenTime(context: Context): List<DayUsage> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        return try {
            val usageStatsManager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val now = System.currentTimeMillis()
            val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                sevenDaysAgo,
                now
            )

            // Build a map of day-of-year -> total ms
            val dayMap = mutableMapOf<Int, Long>()
            val cal = Calendar.getInstance()

            for (stat in stats) {
                if (stat.packageName == context.packageName) continue
                if (stat.totalTimeInForeground <= 0) continue

                cal.timeInMillis = stat.lastTimeUsed.takeIf { it > 0 } ?: (sevenDaysAgo + stat.totalTimeInForeground)
                val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                dayMap[dayKey] = (dayMap[dayKey] ?: 0L) + stat.totalTimeInForeground
            }

            // Build 7-entry list from 6 days ago to today
            val result = mutableListOf<DayUsage>()
            val today = Calendar.getInstance()
            for (daysBack in 6 downTo 0) {
                val dayCal = Calendar.getInstance().apply {
                    timeInMillis = today.timeInMillis
                    add(Calendar.DAY_OF_YEAR, -daysBack)
                }
                val dayKey = dayCal.get(Calendar.YEAR) * 1000 + dayCal.get(Calendar.DAY_OF_YEAR)
                val dayName = when (daysBack) {
                    0 -> "Today"
                    1 -> "Yesterday"
                    else -> android.text.format.DateFormat.format("EEE", dayCal.time).toString()
                }
                result.add(DayUsage(dayName = dayName, durationMs = dayMap[dayKey] ?: 0L))
            }

            result
        } catch (e: Exception) {
            Log.e(TAG, "Error reading weekly screen time", e)
            emptyList()
        }
    }

    /**
     * Formats a duration in milliseconds to a human-readable string.
     * Examples: "2h 34m", "47m", "0m"
     */
    fun formatDuration(durationMs: Long): String {
        val totalMinutes = durationMs / 60_000
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            else -> "${minutes}m"
        }
    }

    /**
     * Returns a zen-flavored label describing the usage level.
     */
    fun getZenUsageLabel(durationMs: Long): String {
        val hours = durationMs / 3_600_000.0
        return when {
            hours < 1.0 -> "Grounded & Present"
            hours < 2.0 -> "Mindfully Connected"
            hours < 3.5 -> "Drifting Toward Distraction"
            hours < 5.0 -> "Deep in the Stream"
            hours < 7.0 -> "Time for Stillness"
            else -> "Return to the Present"
        }
    }

    /**
     * Returns a usage severity level (0–3) for color-coding.
     * 0 = calm/healthy, 1 = moderate, 2 = high, 3 = very high
     */
    fun getUsageSeverity(durationMs: Long): Int {
        val hours = durationMs / 3_600_000.0
        return when {
            hours < 2.0 -> 0
            hours < 4.0 -> 1
            hours < 6.0 -> 2
            else -> 3
        }
    }
}

data class AppUsage(
    val packageName: String,
    val appName: String,
    val durationMs: Long
)

data class DayUsage(
    val dayName: String,
    val durationMs: Long
)
