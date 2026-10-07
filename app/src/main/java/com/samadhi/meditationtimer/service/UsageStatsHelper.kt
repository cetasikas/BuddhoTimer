package com.samadhi.meditationtimer.service

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import android.util.Log
import java.util.Calendar

/**
 * Helper for reading screen time metrics via Android UsageStatsManager.
 *
 * IMPORTANT: Requires PACKAGE_USAGE_STATS permission, which must be
 * manually granted by the user in Settings → Apps → Special App Access → Usage Access.
 *
 * Two query strategies are used deliberately:
 *
 *  - Time-windowed figures (today's total, top apps, hourly bars, week days,
 *    month days) are reconstructed from raw events with
 *    [UsageStatsManager.queryEvents], so each interval lands on the day and
 *    hour it actually happened. The pre-aggregated INTERVAL_DAILY buckets are
 *    deliberately not used for them: on real devices their windows are not
 *    midnight-aligned and their totals are not clipped to the query window,
 *    which misfiled today's usage under yesterday and hid the hourly chart.
 *
 *  - [UsageStatsManager.queryUsageStats] survives only in the pre-API-29
 *    fallback snapshot and as the month fallback when the event stream no
 *    longer reaches back to the start of the month.
 *
 * The dashboard walks events once per refresh; the Timer banner uses the
 * smaller today-only walk.
 */
object UsageStatsHelper {

    private const val TAG = "UsageStatsHelper"

/** Length of a calendar day, used to bound per-day usage queries. */
private const val DAY_MS = 24L * 60L * 60L * 1000L


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
     * Name fragments that identify a launcher, recents or system UI shell.
     *
     * Matched against the package id itself rather than a resolved package list,
     * because Android 11+ hides installed packages from unqueried apps and the
     * recents host in particular does not always answer the home or launcher
     * intents. These shells emit ACTIVITY_RESUMED constantly, so without this a
     * single recents activity can report hundreds of opens and dominate the list.
     */
    private val SHELL_NAME_TOKENS = listOf(
        "launcher",
        "systemui",
        "nexuslauncher",
        "recents"
    )

    /**
     * Decides which packages count as tracked apps.
     *
     * Usage stats report far more than the apps a person actually uses. Home
     * screens, recents, the notification shade and the OS screens that appear
     * while granting a permission all accumulate foreground time, and several of
     * them fire dozens of opens during a single permission flow.
     *
     * The test is whether a package exposes a launcher entry point. Anything that
     * cannot be tapped into from the home screen is system bookkeeping rather than
     * something the user chose to spend time in. The manifest <queries> block
     * grants the visibility this needs.
     */
    private class PackageFilter(context: Context) {

        private val appContext = context.applicationContext

        /** Packages the user can launch from the home screen. */
        private val launchable: Set<String> = queryPackages(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        )

        /** Packages hosting a home screen, which are technically always foreground. */
        private val home: Set<String> = queryPackages(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        )

        private fun queryPackages(intent: Intent): Set<String> {
            return try {
                appContext.packageManager
                    .queryIntentActivities(intent, 0)
                    .mapNotNull { it.activityInfo?.packageName }
                    .toSet()
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving packages for intent $intent", e)
                emptySet()
            }
        }

        /**
         * True when a package must not contribute to screen time totals.
         */
        fun isExcluded(packageName: String?): Boolean {
            if (packageName.isNullOrEmpty()) return true
            if (SHELL_NAME_TOKENS.any { packageName.contains(it, ignoreCase = true) }) return true
            if (packageName in home) return true
            return packageName !in launchable
        }
    }

    /**
     * Gets today's total screen time in milliseconds, excluding this app and
     * any home screen package.
     *
     * Reconstructed from the event stream, so the figure stops at midnight
     * exactly instead of trusting an aggregated bucket that may span it.
     * Returns null if permission is not granted.
     */
    fun getTodayScreenTimeMs(context: Context): Long? {
        if (!hasUsageAccessPermission(context)) return null

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val filter = PackageFilter(context)

            val from = startOfToday()
            val to = System.currentTimeMillis()

            var total = 0L
            accumulateForeground(
                usageStatsManager, from, to, filter::isExcluded,
                onSegment = { _, segStart, segEnd -> total += segEnd - segStart }
            )
            total
        } catch (e: Exception) {
            Log.e(TAG, "Error reading usage stats", e)
            null
        }
    }

    /**
     * Gets today's top apps by foreground time, sorted descending.
     *
     * Returns empty list if permission is not granted.
     */
    fun getTopAppsToday(context: Context, limit: Int = 8): List<AppUsage> {
        if (!hasUsageAccessPermission(context)) return emptyList()

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager
            val filter = PackageFilter(context)
            val from = startOfToday()
            val to = System.currentTimeMillis()

            val perApp = mutableMapOf<String, Long>()
            accumulateForeground(
                usageStatsManager, from, to, filter::isExcluded,
                onSegment = { pkg, segStart, segEnd ->
                    perApp[pkg] = (perApp[pkg] ?: 0L) + (segEnd - segStart)
                }
            )

            perApp.entries
                .sortedByDescending { it.value }
                .take(limit)
                .mapNotNull { entry ->
                    try {
                        val appInfo = pm.getApplicationInfo(entry.key, 0)
                        AppUsage(
                            packageName = entry.key,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            durationMs = entry.value
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
     * Midnight-aligned daily totals for the last [days] days, oldest first.
     *
     * Retained for callers that only need the coarse bars; the dashboard uses
     * [getScreenTimeSnapshot] so that totals and pickups stay consistent.
     */
    fun getWeeklyScreenTime(context: Context): List<DayUsage> {
        return getScreenTimeSnapshot(context, days = 7).days
    }

    /**
     * Accurate screen time snapshot built from raw usage events.
     *
     * Foreground intervals are reconstructed from ACTIVITY_RESUMED →
     * ACTIVITY_PAUSED/STOPPED pairs and split at midnight, so an app used late
     * one night and early the next is credited to both days. This is the fix
     * for the previous behaviour, which assigned an app's entire daily total to
     * whichever single day contained its last use.
     *
     * @param days number of calendar days to report, today included
     *
     * Known limitation: an app that was already in the foreground when the
     * window opened has no ACTIVITY_RESUMED inside it, so its time until the
     * next pause goes uncounted. In practice this only affects apps still open
     * at midnight six days ago, so the error is small and bounded.
     */
    fun getScreenTimeSnapshot(context: Context, days: Int = 7): ScreenTimeSnapshot {
        val empty = ScreenTimeSnapshot(
            days = emptyList(),
            todayTotalMs = 0L,
            todayApps = emptyList(),
            todayPickups = 0,
            pickupsAvailable = false
        )
        if (!hasUsageAccessPermission(context)) return empty

        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val filter = PackageFilter(context)
        val excluded: (String?) -> Boolean = { pkg -> filter.isExcluded(pkg) }
        val now = System.currentTimeMillis()

        // Hourly and monthly breakdowns are their own walks: the hourly one only
        // needs today, the monthly one falls back to aggregated buckets when the
        // event stream no longer reaches back to the start of the month.
        val hourly = hourlyToday(usageStatsManager, excluded, now)
        val month = currentMonth(context, usageStatsManager, excluded, now)

        // ACTIVITY_RESUMED/PAUSED/STOPPED were introduced in API 29. On older
        // releases the event stream carries different type codes, so fall back
        // to aggregated buckets and report that pickups are unavailable.
        val base = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            legacySnapshot(context, days)
        } else {
            buildEventSnapshot(context, days)
        }

        return base.copy(
            hourlyToday = hourly,
            monthDays = month
        )
    }

    /**
     * Replays the activity event stream over [from]..[to] and reports each
     * reconstructed foreground interval via [onSegment].
     *
     * Exactly one activity is treated as topmost at a time: ACTIVITY_RESUMED
     * opens a segment and a matching ACTIVITY_PAUSED/STOPPED closes it, so the
     * overlapping resume/stop pairs that multi-activity apps emit cannot
     * double-count (the failure mode of naive per-event pairing). The activity
     * class narrows the match, so pausing one activity of an app that has two
     * in flight does not end the other's segment. The platform pauses the top
     * activity when the screen turns off, which keeps screen-off time out of
     * the total; SCREEN_NON_INTERACTIVE force-closes any segment as a safety
     * net on releases that skip that pause.
     *
     * Events arrive in chronological order, so segments are already sorted.
     * [onEvent], when provided, observes every raw event of the same pass,
     * letting callers count pickups and app opens without a second query.
     */
    private fun accumulateForeground(
        usageStatsManager: UsageStatsManager,
        from: Long,
        to: Long,
        excluded: (String?) -> Boolean,
        onSegment: (packageName: String, segStart: Long, segEnd: Long) -> Unit,
        onEvent: ((UsageEvents.Event) -> Unit)? = null
    ) {
        var currentPkg: String? = null
        var currentClass: String? = null
        var segStart = 0L

        fun closeSegment(at: Long) {
            val pkg = currentPkg ?: return
            val start = segStart.coerceAtLeast(from)
            val end = at.coerceAtMost(to)
            currentPkg = null
            currentClass = null
            if (end > start) onSegment(pkg, start, end)
        }

        val events = usageStatsManager.queryEvents(from, to)
        val event = UsageEvents.Event()
        while (events.getNextEvent(event)) {
            if (event.timeStamp > to) break
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    closeSegment(event.timeStamp)
                    val pkg = event.packageName
                    if (pkg != null && !excluded(pkg)) {
                        currentPkg = pkg
                        currentClass = event.className
                        segStart = event.timeStamp
                    }
                }

                // STOPPED closes only when PAUSED never arrived (the app was
                // killed while foreground); after a PAUSED it is a no-op.
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    if (currentPkg == event.packageName &&
                        currentClass == event.className
                    ) {
                        closeSegment(event.timeStamp)
                    }
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                event.eventType == UsageEvents.Event.SCREEN_NON_INTERACTIVE
            ) {
                closeSegment(event.timeStamp)
            }
            onEvent?.invoke(event)
        }
        closeSegment(to)
    }

    /**
     * Splits [start]..[end] at each entry of [slotStarts] (ascending) and
     * reports the pieces with the slot each falls into. [slotStarts] must begin
     * at or before [start]; the slot after the last boundary runs to [end].
     */
    private fun splitInto(
        start: Long,
        end: Long,
        slotStarts: LongArray,
        onPiece: (slot: Int, pieceStart: Long, pieceEnd: Long) -> Unit
    ) {
        var s = start
        while (s < end) {
            var slot = 0
            for (i in slotStarts.indices.reversed()) {
                if (slotStarts[i] <= s) {
                    slot = i
                    break
                }
            }
            val boundary =
                if (slot + 1 < slotStarts.size) slotStarts[slot + 1] else Long.MAX_VALUE
            val e = minOf(end, boundary)
            if (e > s) onPiece(slot, s, e)
            s = e
        }
    }

    /**
     * Start timestamp of each hour of the day containing [todayStart], built
     * with Calendar arithmetic so a DST transition cannot skew the boundaries.
     */
    private fun hourStartsOf(todayStart: Long): LongArray {
        val result = LongArray(24)
        var t = todayStart
        for (h in 0 until 24) {
            result[h] = t
            t = Calendar.getInstance().apply {
                timeInMillis = t
                add(Calendar.HOUR_OF_DAY, 1)
            }.timeInMillis
        }
        return result
    }

    /**
     * Foreground milliseconds for each hour of today that has elapsed.
     */
    private fun hourlyToday(
        usageStatsManager: UsageStatsManager,
        excluded: (String?) -> Boolean,
        now: Long
    ): List<HourlyUsage> {
        val start = startOfToday(now)
        val totals = LongArray(24)
        return try {
            val hours = hourStartsOf(start)
            accumulateForeground(
                usageStatsManager, start, now, excluded,
                onSegment = { _, segStart, segEnd ->
                    splitInto(segStart, segEnd, hours) { hour, pieceStart, pieceEnd ->
                        totals[hour] += pieceEnd - pieceStart
                    }
                }
            )
            totals.mapIndexed { hour, ms -> HourlyUsage(hour, ms) }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading hourly usage", e)
            emptyList()
        }
    }

    /**
     * Per-day totals for the calendar month containing [now].
     *
     * The platform only retains a limited history, so early days can come back
     * empty rather than zero; [MonthDayUsage.hasData] distinguishes the two.
     */
    private fun currentMonth(
        context: Context,
        usageStatsManager: UsageStatsManager,
        excluded: (String?) -> Boolean,
        now: Long
    ): List<MonthDayUsage> {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayDom = cal.get(Calendar.DAY_OF_MONTH)

        val monthStart = (Calendar.getInstance()).apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val totals = LongArray(daysInMonth + 1)

        // Events older than the platform's retention window are silently gone,
        // so a cheap probe decides which source can answer for the whole month.
        // System events fire even on quiet days, so an event landing within the
        // first day of the month means the stream still covers it end to end.
        val probe = usageStatsManager.queryEvents(monthStart, now)
        val probeEvent = UsageEvents.Event()
        val earliestEvent =
            if (probe.getNextEvent(probeEvent)) probeEvent.timeStamp else Long.MAX_VALUE
        val eventsCoverMonth = earliestEvent <= monthStart + DAY_MS

        if (eventsCoverMonth) {
            val dayStarts = LongArray(todayDom) { dom ->
                (Calendar.getInstance()).apply {
                    timeInMillis = monthStart
                    set(Calendar.DAY_OF_MONTH, dom + 1)
                }.timeInMillis
            }
            accumulateForeground(
                usageStatsManager, monthStart, now, excluded,
                onSegment = { _, segStart, segEnd ->
                    splitInto(segStart, segEnd, dayStarts) { slot, pieceStart, pieceEnd ->
                        totals[slot + 1] += pieceEnd - pieceStart
                    }
                }
            )
        } else {
            // Fallback: aggregated buckets queried one day at a time. A single
            // wide INTERVAL_DAILY query returns buckets aligned to the query
            // window rather than to local midnight, so asking for the whole
            // month in one call collapsed it into one bucket stamped with the
            // first of the month. Per-day queries keep each total tied to the
            // day it belongs to.
            for (dom in 1..todayDom) {
                val dayStart = (Calendar.getInstance()).apply {
                    timeInMillis = monthStart
                    add(Calendar.DAY_OF_MONTH, dom - 1)
                }.timeInMillis
                val dayEnd = minOf(dayStart + DAY_MS - 1, now)
                if (dayEnd < dayStart) continue
                try {
                    totals[dom] = queryUsageStats(usageStatsManager, dayStart, dayEnd, excluded)
                        .sumOf { it.totalTimeInForeground }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading usage for day $dom", e)
                }
            }
        }

        return (1..daysInMonth).map { dom ->
            MonthDayUsage(
                dayOfMonth = dom,
                durationMs = totals[dom],
                isToday = dom == todayDom,
                isFuture = dom > todayDom
            )
        }
    }

@androidx.annotation.RequiresApi(Build.VERSION_CODES.Q)
    private fun buildEventSnapshot(context: Context, days: Int): ScreenTimeSnapshot {
        val empty = ScreenTimeSnapshot(
            days = emptyList(),
            todayTotalMs = 0L,
            todayApps = emptyList(),
            todayPickups = 0,
            pickupsAvailable = true
        )
        if (!hasUsageAccessPermission(context)) return empty

        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val filter = PackageFilter(context)
            val now = System.currentTimeMillis()
            val dayStarts = midnightStarts(days, now)
            val earliest = dayStarts.first()

            fun dayIndexFor(ts: Long): Int {
                for (i in dayStarts.indices.reversed()) {
                    if (ts >= dayStarts[i]) return i
                }
                return -1
            }

            val excluded: (String?) -> Boolean = { pkg -> filter.isExcluded(pkg) }

            val dayTotals = LongArray(days)
            val todayAppTotals = mutableMapOf<String, Long>()
            val dayPickups = IntArray(days)
            val todayAppOpens = mutableMapOf<String, Int>()
            // Per-package count of currently resumed activities. Apps commonly run
            // several activities, so an open is only a 0 -> 1 transition, otherwise
            // in-app navigation would be counted as a fresh open.
            val resumedActivities = mutableMapOf<String, Int>()
            val todayIdx = days - 1

            // One event-stream walk yields durations, pickups and opens together.
            // The accumulator keeps a single topmost activity, so overlapping
            // resume/stop pairs cannot double-count, and screen-off time stays
            // out because the platform pauses the top activity. The aggregated
            // INTERVAL_DAILY buckets are avoided for durations on purpose: their
            // windows are not midnight-aligned and their totals are not clipped
            // to the query, which filed today's usage under yesterday.
            accumulateForeground(
                usageStatsManager,
                earliest,
                now,
                excluded,
                onSegment = { pkg, segStart, segEnd ->
                    splitInto(segStart, segEnd, dayStarts) { slot, pieceStart, pieceEnd ->
                        val piece = pieceEnd - pieceStart
                        dayTotals[slot] += piece
                        if (slot == todayIdx) {
                            todayAppTotals[pkg] = (todayAppTotals[pkg] ?: 0L) + piece
                        }
                    }
                },
                onEvent = { event ->
                    when {
                        // A pickup is the screen being turned on. These are system
                        // events with no package name, so they must be handled before
                        // the package filter, which would otherwise drop them.
                        event.eventType == UsageEvents.Event.SCREEN_INTERACTIVE -> {
                            val idx = dayIndexFor(event.timeStamp)
                            if (idx >= 0) dayPickups[idx]++
                        }

                        else -> {
                            val pkg = event.packageName
                            if (pkg != null && !excluded(pkg)) {
                                when (event.eventType) {
                                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                                        val count = (resumedActivities[pkg] ?: 0) + 1
                                        resumedActivities[pkg] = count
                                        if (count == 1) {
                                            val idx = dayIndexFor(event.timeStamp)
                                            if (idx == todayIdx) {
                                                todayAppOpens[pkg] =
                                                    (todayAppOpens[pkg] ?: 0) + 1
                                            }
                                        }
                                    }

                                    // Only PAUSED decrements; STOPPED follows PAUSED for
                                    // the same activity and would double-decrement.
                                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                                        val count =
                                            ((resumedActivities[pkg] ?: 0) - 1).coerceAtLeast(0)
                                        resumedActivities[pkg] = count
                                    }

                                    else -> Unit
                                }
                            }
                        }
                    }
                }
            )

            val pm = context.packageManager
            val todayApps = todayAppTotals.entries
                .sortedByDescending { it.value }
                .take(12)
                .map { entry ->
                    AppUsage(
                        packageName = entry.key,
                        appName = resolveAppLabel(pm, entry.key),
                        durationMs = entry.value,
                        openCount = todayAppOpens[entry.key] ?: 0
                    )
                }

            val dayList = dayStarts.indices.map { i ->
                DayUsage(
                    dayName = dayLabel(i, days, now),
                    durationMs = dayTotals[i],
                    pickups = dayPickups[i]
                )
            }

            ScreenTimeSnapshot(
                days = dayList,
                todayTotalMs = todayAppTotals.values.sum(),
                todayApps = todayApps,
                todayPickups = dayPickups.last(),
                pickupsAvailable = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building screen time snapshot", e)
            empty
        }
    }

    /**
     * Aggregated buckets for [from]..[to], excluding unwanted packages.
     */
    private fun queryUsageStats(
        usageStatsManager: UsageStatsManager,
        from: Long,
        to: Long,
        excluded: (String?) -> Boolean
    ): List<UsageStats> {
        return usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY,
            from,
            to
        ).filterNot { excluded(it.packageName) }
    }

    /**
     * Human label for a package, falling back to the package name.
     *
     * Packages can stay invisible to [android.content.pm.PackageManager] even
     * with a `<queries>` declaration, for example system apps with no launcher
     * entry. Their screen time is still real, so the row is kept rather than
     * discarded.
     */
    private fun resolveAppLabel(pm: android.content.pm.PackageManager, packageName: String): String {
        return try {
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        } catch (e: Exception) {
            packageName.substringAfterLast('.')
        }
    }

    /**
     * Approximate snapshot for API 26–28, which lack the foreground event types.
     *
     * Buckets an app's whole daily total into the day containing its last use,
     * which is wrong when usage spans midnight. That is the only signal the
     * aggregated query exposes, so it is used here rather than reporting zeros.
     */
    private fun legacySnapshot(context: Context, days: Int): ScreenTimeSnapshot {
        return try {
            val usageStatsManager =
                context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
            val pm = context.packageManager
            val filter = PackageFilter(context)
            val now = System.currentTimeMillis()
            val dayStarts = midnightStarts(days, now)

            val stats = usageStatsManager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                dayStarts.first(),
                now
            )

            val dayMap = mutableMapOf<Int, Long>()
            val cal = Calendar.getInstance()
            val todayApps = mutableMapOf<String, Long>()

            for (stat in stats) {
                if (filter.isExcluded(stat.packageName)) continue
                if (stat.totalTimeInForeground <= 0) continue

                cal.timeInMillis = stat.lastTimeUsed.takeIf { it > 0 } ?: dayStarts.first()
                val dayKey = cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
                dayMap[dayKey] = (dayMap[dayKey] ?: 0L) + stat.totalTimeInForeground

                // Today-only per-app figures are unaffected by midnight splitting.
                if (cal.get(Calendar.YEAR) == Calendar.getInstance().get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) ==
                    Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                ) {
                    todayApps[stat.packageName] = stat.totalTimeInForeground
                }
            }

            val dayList = dayStarts.indices.map { i ->
                val dayCal = Calendar.getInstance().apply { timeInMillis = dayStarts[i] }
                val dayKey = dayCal.get(Calendar.YEAR) * 1000 + dayCal.get(Calendar.DAY_OF_YEAR)
                DayUsage(
                    dayName = dayLabel(i, days, now),
                    durationMs = dayMap[dayKey] ?: 0L,
                    pickups = 0
                )
            }

            val appList = todayApps.entries
                .sortedByDescending { it.value }
                .take(12)
                .mapNotNull { entry ->
                    try {
                        val appInfo = pm.getApplicationInfo(entry.key, 0)
                        AppUsage(
                            packageName = entry.key,
                            appName = pm.getApplicationLabel(appInfo).toString(),
                            durationMs = entry.value
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

            ScreenTimeSnapshot(
                days = dayList,
                todayTotalMs = dayList.last().durationMs,
                todayApps = appList,
                todayPickups = 0,
                pickupsAvailable = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error building legacy snapshot", e)
            ScreenTimeSnapshot(emptyList(), 0L, emptyList(), 0, pickupsAvailable = false)
        }
    }

    /**
     * Today's number of screen-ons, i.e. how many times the phone was picked up
     * and the screen turned on. Deliberately distinct from per-app open counts,
     * which track returning to a specific app. Returns 0 without permission.
     */
    fun getPickupsToday(context: Context): Int {
        if (!hasUsageAccessPermission(context)) return 0
        return getScreenTimeSnapshot(context, days = 1).todayPickups
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

    /**
     * Severity relative to a user-configured daily goal, used for goal colouring.
     * Returns a value in 0f..1f where 1f means the goal has been reached.
     */
    fun getGoalProgress(durationMs: Long, goalMinutes: Int): Float {
        if (goalMinutes <= 0) return 0f
        val goalMs = goalMinutes * 60_000L
        if (goalMs <= 0L) return 0f
        return (durationMs.toFloat() / goalMs.toFloat()).coerceIn(0f, 1f)
    }

    private fun startOfToday(now: Long = System.currentTimeMillis()): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    /**
     * Midnight timestamp for each of the last [days] days, oldest first.
     * Uses Calendar arithmetic rather than fixed 24h offsets so DST
     * transitions do not shift bucket boundaries.
     */
    private fun midnightStarts(days: Int, now: Long): LongArray {
        val result = LongArray(days)
        for (i in 0 until days) {
            val cal = Calendar.getInstance()
            cal.timeInMillis = now
            cal.add(Calendar.DAY_OF_YEAR, -(days - 1 - i))
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            result[i] = cal.timeInMillis
        }
        return result
    }

    private fun dayLabel(index: Int, days: Int, now: Long): String {
        val daysBack = days - 1 - index
        return when (daysBack) {
            0 -> "Today"
            1 -> "Yesterday"
            else -> {
                val cal = Calendar.getInstance()
                cal.timeInMillis = now
                cal.add(Calendar.DAY_OF_YEAR, -daysBack)
                android.text.format.DateFormat.format("EEE", cal.time).toString()
            }
        }
    }
}

data class AppUsage(
    val packageName: String,
    val appName: String,
    val durationMs: Long,
    /** Times the app was brought to the foreground today. */
    val openCount: Int = 0
)

data class DayUsage(
    val dayName: String,
    val durationMs: Long,
    /** Screen-on events that day, i.e. phone pickups. */
    val pickups: Int = 0
)

/**
 * Authoritative screen time figures for a single query pass.
 *
 * [todayPickups] counts screen-ons (phone pickups). Per-app open counts live on
 * [todayApps] as [AppUsage.openCount] and are tracked separately.
 */
data class ScreenTimeSnapshot(
    val days: List<DayUsage>,
    val todayTotalMs: Long,
    val todayApps: List<AppUsage>,
    val todayPickups: Int,
    /** False on API < 29, where the event stream cannot report pickups. */
    val pickupsAvailable: Boolean,
    /** Foreground time per hour of today, indexed by hour of day. */
    val hourlyToday: List<HourlyUsage> = emptyList(),
    /** Per-day totals for the current calendar month. */
    val monthDays: List<MonthDayUsage> = emptyList()
)

/**
 * Foreground time in a single hour of the day.
 */
data class HourlyUsage(
    val hour: Int,
    val durationMs: Long
)

/**
 * One day of the current calendar month.
 */
data class MonthDayUsage(
    val dayOfMonth: Int,
    val durationMs: Long,
    val isToday: Boolean,
    val isFuture: Boolean
) {
    /** False when the platform has no record, as opposed to a genuine zero. */
    val hasData: Boolean get() = durationMs > 0
}
