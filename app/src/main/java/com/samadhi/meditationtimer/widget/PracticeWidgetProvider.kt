package com.samadhi.meditationtimer.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.samadhi.meditationtimer.MainActivity
import com.samadhi.meditationtimer.MeditationApp
import com.samadhi.meditationtimer.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class PracticeWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        updateWidgets(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PracticeWidgetProvider::class.java))
            updateWidgets(context, manager, ids)
        }

        private fun updateWidgets(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
            if (appWidgetIds.isEmpty()) return

            val repository = (context.applicationContext as MeditationApp).sessionRepository
            val zone = ZoneId.systemDefault()
            val today = LocalDate.now()
            val prefs = context.getSharedPreferences("journal_prefs", Context.MODE_PRIVATE)
            val restDays = prefs.getStringSet("rest_day_dates", emptySet()) ?: emptySet()
            val sessions = runBlocking { repository.getAllSessionsFlow().first() }
            val todayMinutes = sessions
                .filter { Instant.ofEpochMilli(it.completedTimeEpochMs).atZone(zone).toLocalDate() == today }
                .sumOf { it.actualDurationSec } / 60
            val streakDays = (sessions.map {
                Instant.ofEpochMilli(it.completedTimeEpochMs).atZone(zone).toLocalDate()
            } + restDays.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() })
                .distinct()
                .let { dates -> calculateCurrentStreak(dates, today) }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            appWidgetIds.forEach { id ->
                val views = RemoteViews(context.packageName, R.layout.widget_practice).apply {
                    setTextViewText(R.id.widget_today_minutes, "${todayMinutes}m today")
                    setTextViewText(R.id.widget_streak, "${streakDays} day rhythm")
                    setTextViewText(R.id.widget_prompt, if (todayMinutes > 0) "Return when ready" else "Begin gently")
                    setOnClickPendingIntent(R.id.widget_root, pendingIntent)
                }
                appWidgetManager.updateAppWidget(id, views)
            }
        }

        private fun calculateCurrentStreak(dates: List<LocalDate>, today: LocalDate): Int {
            val unique = dates.distinct()
            var checkDate = when {
                unique.contains(today) -> today
                unique.contains(today.minusDays(1)) -> today.minusDays(1)
                else -> return 0
            }
            var streak = 0
            while (unique.contains(checkDate)) {
                streak++
                checkDate = checkDate.minusDays(1)
            }
            return streak
        }
    }
}
