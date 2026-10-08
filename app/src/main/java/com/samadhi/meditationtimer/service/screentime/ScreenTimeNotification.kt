package com.samadhi.meditationtimer.service.screentime

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.PowerManager
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.samadhi.meditationtimer.MainActivity
import com.samadhi.meditationtimer.R
import com.samadhi.meditationtimer.data.ScreenTimePreferences
import com.samadhi.meditationtimer.service.HourlyUsage
import com.samadhi.meditationtimer.service.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Persistent lock screen / notification shade status for daily screen time.
 *
 * Completely separate from the meditation, breathing and reminder channels:
 * it has its own channel, ongoing, and only exists while the user opts in
 * from Settings. Content mirrors the Screen Time screen - big
 * duration, goal delta, a cumulative line chart with the goal as a dashed
 * marker, and a top apps snippet. Refreshed on unlock, every minute the screen
 * is on, and after goal or settings changes.
 */
object ScreenTimeNotification {

    const val CHANNEL_ID = "screen_time_status_channel_v2"
    /** The old silent channel is deleted once, then never used again. */
    private const val OLD_CHANNEL_ID = "screen_time_status_channel"
    const val NOTIFICATION_ID = 1007
    const val TICK_ACTION = "com.samadhi.meditationtimer.action.SCREEN_TIME_TICK"

    private const val TICK_REQUEST_CODE = 3002

    /**
     * True while a one-shot refresh tick is armed. `setInexactRepeating` lets
     * the alarm manager batch and drift the ticks, and a missed screen-off can
     * leave a repeating chain running idle, so the tick is re-armed one at a
     * time from the moment it actually fires. The flag keeps overlapping
     * triggers (screen-on plus the previous tick) from doubling up.
     */
    private val tickerArmed = java.util.concurrent.atomic.AtomicBoolean(false)

    private const val ACCENT = 0xFF26A69A.toInt()
    private const val OVER = 0xFFE5A93B.toInt()

    val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun showOrUpdate(context: Context) {
        scope.launch { updateNow(context.applicationContext) }
    }

    fun cancel(context: Context) {
        manager(context).cancel(NOTIFICATION_ID)
    }

    /** One-shot refresh about a minute from now; re-armed by each tick. */
    fun scheduleTicker(context: Context) {
        val app = context.applicationContext
        if (!tickerArmed.compareAndSet(false, true)) return
        val alarm = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = System.currentTimeMillis() + 60_000L
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            alarm.canScheduleExactAlarms()
        if (exact) {
            // Exact is fine here: the ticker only exists while the screen is on,
            // so the device is already awake and the alarm costs no extra wakeups.
            alarm.setExact(AlarmManager.RTC, triggerAt, tickIntent(app))
        } else {
            alarm.setWindow(AlarmManager.RTC, triggerAt, 60_000L, tickIntent(app))
        }
    }

    fun cancelTicker(context: Context) {
        tickerArmed.set(false)
        val app = context.applicationContext
        val alarm = app.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarm.cancel(tickIntent(app))
    }

    /** Entry point for [ScreenTimeNotificationReceiver]; handles any action. */
    suspend fun handleAction(context: Context, action: String?) {
        val app = context.applicationContext
        when (action) {
            Intent.ACTION_SCREEN_ON -> {
                if (!isEnabled(app)) {
                    cancelTicker(app)
                    cancel(app)
                    return
                }
                updateNow(app)
                scheduleTicker(app)
            }

            Intent.ACTION_SCREEN_OFF -> cancelTicker(app)

            TICK_ACTION -> {
                // This tick consumed the armed one-shot; re-arm for the next
                // minute only if the screen is still on.
                tickerArmed.set(false)
                if (!isEnabled(app)) {
                    cancelTicker(app)
                    cancel(app)
                    return
                }
                updateNow(app)
                val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isInteractive) scheduleTicker(app)
            }

            Intent.ACTION_BOOT_COMPLETED, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                if (!isEnabled(app)) {
                    cancel(app)
                    return
                }
                updateNow(app)
                val pm = app.getSystemService(Context.POWER_SERVICE) as PowerManager
                if (pm.isInteractive) scheduleTicker(app)
            }
        }
    }

    private suspend fun isEnabled(context: Context): Boolean =
        ScreenTimePreferences(context).screenTimeNotificationEnabled.first()

    private suspend fun updateNow(context: Context) {
        val app = context.applicationContext
        val prefs = ScreenTimePreferences(app)
        if (!prefs.screenTimeNotificationEnabled.first()) {
            cancel(app)
            return
        }
        if (!UsageStatsHelper.hasUsageAccessPermission(app)) {
            cancel(app)
            return
        }

        val goalMinutes = prefs.dailyGoalMinutes.first()
        val snapshot = UsageStatsHelper.getScreenTimeSnapshot(app, 7)
        val notification = buildNotification(
            context = app,
            todayMs = snapshot.todayTotalMs,
            goalMinutes = goalMinutes,
            hourly = snapshot.hourlyToday,
            apps = snapshot.todayApps.take(3)
        )
        ensureChannel(app)
        manager(app).notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(
        context: Context,
        todayMs: Long,
        goalMinutes: Int,
        hourly: List<HourlyUsage>,
        apps: List<com.samadhi.meditationtimer.service.AppUsage>
    ): Notification {
        val goalMs = if (goalMinutes > 0) goalMinutes * 60_000L else 0L
        val over = goalMs > 0 && todayMs > goalMs
        val deltaShort = when {
            goalMs <= 0 -> null
            over -> "${UsageStatsHelper.formatDuration(todayMs - goalMs)} over"
            else -> "${UsageStatsHelper.formatDuration(goalMs - todayMs)} left"
        }
        val deltaLong = when {
            goalMs <= 0 -> null
            over -> "${UsageStatsHelper.formatDuration(todayMs - goalMs)} over your " +
                "${ScreenTimePreferences.presetLabel(goalMinutes)} intention"
            else -> "${UsageStatsHelper.formatDuration(goalMs - todayMs)} left of your " +
                "${ScreenTimePreferences.presetLabel(goalMinutes)} intention"
        }

        val openApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val collapsed = RemoteViews(context.packageName, R.layout.notification_screen_time_collapsed).apply {
            setTextViewText(R.id.st_time, UsageStatsHelper.formatDuration(todayMs))
            if (deltaShort != null) {
                setViewVisibility(R.id.st_delta, View.VISIBLE)
                setTextViewText(R.id.st_delta, deltaShort)
                setTextColor(R.id.st_delta, if (over) OVER else ACCENT)
                val progress = buildProgressBitmap(todayMs, goalMs)
                if (progress != null) {
                    setViewVisibility(R.id.st_progress, View.VISIBLE)
                    setImageViewBitmap(R.id.st_progress, progress)
                }
            }
        }

        val expanded = RemoteViews(context.packageName, R.layout.notification_screen_time_expanded).apply {
            setTextViewText(R.id.st_time_big, UsageStatsHelper.formatDuration(todayMs))
            if (deltaLong != null) {
                setViewVisibility(R.id.st_delta_big, View.VISIBLE)
                setTextViewText(R.id.st_delta_big, deltaLong)
                setTextColor(R.id.st_delta_big, if (over) OVER else ACCENT)
            }
            val chart = buildChartBitmap(hourly, goalMs)
            if (chart != null) {
                setViewVisibility(R.id.st_chart, View.VISIBLE)
                setImageViewBitmap(R.id.st_chart, chart)
            }
            if (apps.isNotEmpty()) {
                setViewVisibility(R.id.st_apps, View.VISIBLE)
                setTextViewText(
                    R.id.st_apps,
                    apps.mapIndexed { index, app ->
                        "${index + 1}. ${app.appName} \u00B7 ${UsageStatsHelper.formatDuration(app.durationMs)}"
                    }.joinToString("\n")
                )
            }
        }

        val contentText = when {
            deltaShort != null -> "${UsageStatsHelper.formatDuration(todayMs)} on screen \u00B7 $deltaShort"
            else -> "${UsageStatsHelper.formatDuration(todayMs)} on screen today"
        }

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Screen Time")
            .setContentText(contentText)
            .setCustomContentView(collapsed)
            .setCustomBigContentView(expanded)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setColor(ACCENT)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    /**
     * Cumulative line across today's hours. The line is accent-colored while
     * under the goal and switches to the over color above it, with the goal
     * itself drawn as a dashed marker. Returns null when there is no data.
     */
    private fun buildChartBitmap(hourly: List<HourlyUsage>, goalMs: Long): Bitmap? {
        if (hourly.isEmpty()) return null

        val w = 640
        val h = 192
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val padL = 8f
        val padR = 8f
        val padT = 18f
        val padB = 34f
        val innerW = w - padL - padR
        val innerH = h - padT - padB

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val currentHour = cal.get(Calendar.HOUR_OF_DAY)
        val nowFrac = currentHour + cal.get(Calendar.MINUTE) / 60f + cal.get(Calendar.SECOND) / 3600f

        val total = hourly.fold(0L) { sum, item -> sum + item.durationMs }
        val maxVal = maxOf(goalMs, total, 3_600_000L) * 1.12f

        fun xAt(hourF: Float): Float = padL + (hourF / 24f) * innerW
        fun yAt(ms: Float): Float = (h - padB) - (ms / maxVal) * innerH

        val xs = ArrayList<Float>()
        val ys = ArrayList<Float>()
        val cums = ArrayList<Long>()
        xs.add(xAt(0f))
        ys.add(yAt(0f))
        cums.add(0L)

        var cum = 0L
        for (hour in 0..currentHour.coerceAtMost(hourly.size - 1)) {
            cum += hourly[hour].durationMs
            xs.add(xAt(minOf(hour + 1f, nowFrac)))
            ys.add(yAt(cum.toFloat()))
            cums.add(cum)
        }

        val baseline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x30FFFFFF
            strokeWidth = 2f
        }
        canvas.drawLine(padL, (h - padB).toFloat(), (w - padR).toFloat(), (h - padB).toFloat(), baseline)

        // Hour-of-day grid: light verticals at 4-hour steps and a labelled
        // tick underneath, so the day's rhythm can be read from the chart.
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x14FFFFFF
            strokeWidth = 2f
        }
        val tickPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x45FFFFFF
            strokeWidth = 2f
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0x59FFFFFF
            textSize = 20f
            typeface = Typeface.DEFAULT
        }
        val baseY = (h - padB).toFloat()
        for (hour in 0..24 step 4) {
            val tickX = xAt(hour.toFloat())
            if (hour > 0 && hour < 24) {
                canvas.drawLine(tickX, padT, tickX, baseY, gridPaint)
            }
            canvas.drawLine(tickX, baseY, tickX, baseY + 7f, tickPaint)
            labelPaint.textAlign = when (hour) {
                0 -> Paint.Align.LEFT
                24 -> Paint.Align.RIGHT
                else -> Paint.Align.CENTER
            }
            val labelX = when (hour) {
                0 -> padL
                24 -> (w - padR).toFloat()
                else -> tickX
            }
            canvas.drawText(hour.toString(), labelX, (h - 8f).toFloat(), labelPaint)
        }

        if (goalMs > 0) {
            val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = 0x99E5A93B.toInt()
                strokeWidth = 2f
                pathEffect = DashPathEffect(floatArrayOf(14f, 10f), 0f)
            }
            val goalY = yAt(goalMs.toFloat())
            canvas.drawLine(padL, goalY, (w - padR).toFloat(), goalY, goalPaint)
        }

        val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 4f
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            style = Paint.Style.STROKE
        }
        for (i in 1 until xs.size) {
            val x0 = xs[i - 1]
            val y0 = ys[i - 1]
            val x1 = xs[i]
            val y1 = ys[i]
            val c0 = cums[i - 1]
            val c1 = cums[i]
            when {
                goalMs <= 0 -> {
                    line.color = ACCENT
                    canvas.drawLine(x0, y0, x1, y1, line)
                }

                c0 <= goalMs && c1 <= goalMs -> {
                    line.color = ACCENT
                    canvas.drawLine(x0, y0, x1, y1, line)
                }

                c0 >= goalMs && c1 >= goalMs -> {
                    line.color = OVER
                    canvas.drawLine(x0, y0, x1, y1, line)
                }

                else -> {
                    val fraction = (goalMs - c0).toFloat() / (c1 - c0).toFloat()
                    val crossX = x0 + (x1 - x0) * fraction
                    val crossY = yAt(goalMs.toFloat())
                    line.color = ACCENT
                    canvas.drawLine(x0, y0, crossX, crossY, line)
                    line.color = OVER
                    canvas.drawLine(crossX, crossY, x1, y1, line)
                }
            }
        }

        val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = if (goalMs > 0 && cums.last() > goalMs) OVER else ACCENT
            style = Paint.Style.FILL
        }
        canvas.drawCircle(xs.last(), ys.last(), 6f, dot)

        return bitmap
    }

    /**
     * Thin two-segment progress line: accent while under the goal, over color
     * past it, over a dim track scaled to whichever is larger. Null without a
     * goal, since there is nothing to measure against.
     */
    private fun buildProgressBitmap(usageMs: Long, goalMs: Long): Bitmap? {
        if (goalMs <= 0) return null

        val w = 640
        val h = 14
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val scale = maxOf(goalMs, usageMs)
        if (scale <= 0L) return null

        val radius = h / 2f
        val track = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x33FFFFFF }
        canvas.drawRoundRect(RectF(0f, 0f, w.toFloat(), h.toFloat()), radius, radius, track)

        val underW = (minOf(usageMs, goalMs).toFloat() / scale) * w
        if (usageMs > goalMs) {
            val overW = ((usageMs - goalMs).toFloat() / scale) * w
            val overPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = OVER }
            val left = underW
            val right = (left + overW).coerceAtMost(w.toFloat())
            if (right > left) {
                canvas.drawRoundRect(
                    RectF(left, 0f, right, h.toFloat()),
                    minOf(radius, (right - left) / 2f),
                    minOf(radius, (right - left) / 2f),
                    overPaint
                )
            }
        }
        if (underW > 1f) {
            // Drawn last and slightly past the junction so the two segments
            // meet cleanly without the track showing through the rounded ends.
            val underPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ACCENT }
            val right = (underW + 3f).coerceAtMost(w.toFloat())
            canvas.drawRoundRect(
                RectF(0f, 0f, right, h.toFloat()),
                minOf(radius, right / 2f),
                minOf(radius, right / 2f),
                underPaint
            )
        }

        return bitmap
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = manager(context)
            // A channel's importance is immutable once created, so the silent
            // one is retired and the status moves to a new, audible channel.
            // The system dismisses any notification still on the retired one.
            if (channelMigrated.compareAndSet(false, true)) {
                nm.deleteNotificationChannel(OLD_CHANNEL_ID)
            }
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Time",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Persistent daily screen time status"
                setShowBadge(true)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(channel)
        }
    }

    private val channelMigrated = java.util.concurrent.atomic.AtomicBoolean(false)

    private fun manager(context: Context): NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    private fun tickIntent(context: Context): PendingIntent {
        val intent = Intent(context, ScreenTimeNotificationReceiver::class.java).apply {
            action = TICK_ACTION
        }
        return PendingIntent.getBroadcast(
            context,
            TICK_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
