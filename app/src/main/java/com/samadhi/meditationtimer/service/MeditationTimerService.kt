package com.samadhi.meditationtimer.service

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.samadhi.meditationtimer.domain.model.TimerStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MeditationTimerService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null

    private lateinit var notificationHelper: NotificationHelper
    private lateinit var wakeLockHelper: WakeLockHelper
    private lateinit var soundHelper: SoundHelper
    private lateinit var dndManager: DndManager

    private var startTimeEpochMs: Long = 0L
    private var targetDurationSec: Int = 0
    private var prepTimeSec: Int = 0
    private var elapsedSeconds: Int = 0
    private var overrunSeconds: Int = 0
    private var isOpenEnded: Boolean = false
    private var isDndEnabledForSession: Boolean = false
    private var hasChimedForOverrun: Boolean = false

    private var soundBellResName: String = SoundHelper.SOUND_TIBETAN_BOWL

    inner class LocalBinder : Binder() {
        fun getService(): MeditationTimerService = this@MeditationTimerService
    }

    override fun onCreate() {
        super.onCreate()
        notificationHelper = NotificationHelper(this)
        wakeLockHelper = WakeLockHelper(this)
        soundHelper = SoundHelper(this)
        dndManager = DndManager(this)
        Log.d(TAG, "MeditationTimerService created")
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val targetSec = intent.getIntExtra(EXTRA_TARGET_SEC, 15 * 60)
                val prepSec = intent.getIntExtra(EXTRA_PREP_SEC, 5)
                val autoDnd = intent.getBooleanExtra(EXTRA_AUTO_DND, true)
                val openEnded = intent.getBooleanExtra(EXTRA_OPEN_ENDED, false)
                val soundBell = intent.getStringExtra(EXTRA_SOUND_BELL) ?: SoundHelper.SOUND_TIBETAN_BOWL
                val mindfulnessIntervalMin = intent.getIntExtra(EXTRA_MINDFULNESS_INTERVAL_MIN, 0)
                startTimer(targetSec, prepSec, autoDnd, openEnded, soundBell, mindfulnessIntervalMin)
            }
            ACTION_PAUSE -> pauseTimer()
            ACTION_RESUME -> resumeTimer()
            ACTION_FINISH -> finishTimer()
            ACTION_CANCEL -> cancelTimer()
        }
        return START_NOT_STICKY
    }

    private fun startTimer(
        targetSec: Int,
        prepSec: Int,
        autoDnd: Boolean,
        openEnded: Boolean,
        soundBell: String,
        mindfulnessIntervalMin: Int
    ) {
        targetDurationSec = targetSec
        prepTimeSec = prepSec
        isDndEnabledForSession = autoDnd
        isOpenEnded = openEnded
        soundBellResName = soundBell
        elapsedSeconds = 0
        overrunSeconds = 0
        hasChimedForOverrun = false
        startTimeEpochMs = System.currentTimeMillis()

        wakeLockHelper.acquireWakeLock()

        if (isDndEnabledForSession) {
            dndManager.enableDnd()
        }

        // Start Foreground immediately with initial notification
        startForegroundServiceWithNotification(
            timeText = if (prepSec > 0) "Preparing in $prepSec s" else NotificationHelper.formatSeconds(targetSec),
            subText = if (openEnded) "Open-Ended" else "Target: ${targetSec / 60}m",
            isPaused = false,
            isOverrun = false
        )

        timerJob?.cancel()
        timerJob = serviceScope.launch {
            // 1. Preparation Phase (if any)
            var currentPrep = prepSec
            while (currentPrep > 0 && isActive) {
                _timerStatus.value = TimerStatus.Preparing(currentPrep)
                updateNotification(
                    timeText = "Starting in $currentPrep s",
                    subText = "Take a comfortable posture",
                    isPaused = false,
                    isOverrun = false
                )
                soundHelper.triggerGentleVibration()
                delay(1000L)
                currentPrep--
            }

            // Beginning chime
            soundHelper.playChime(soundBellResName, serviceScope)

            // 2. Active Session Loop
            val intervalSec = mindfulnessIntervalMin * 60
            while (isActive) {
                elapsedSeconds++

                // Mindfulness Interval Bell (Chimes periodically if enabled)
                if (intervalSec > 0 && elapsedSeconds > 0 && elapsedSeconds % intervalSec == 0) {
                    // Only chime if not right at the end of a timed session (which plays the completion chime)
                    if (isOpenEnded || elapsedSeconds != targetDurationSec) {
                        soundHelper.triggerHarmonicIntervalHaptic()
                        soundHelper.playChime(soundBellResName, serviceScope)
                    }
                }

                if (isOpenEnded) {
                    // Open-ended Stopwatch mode
                    _timerStatus.value = TimerStatus.Running(
                        remainingSeconds = 0,
                        elapsedSeconds = elapsedSeconds,
                        isOverrun = false
                    )
                    updateNotification(
                        timeText = NotificationHelper.formatSeconds(elapsedSeconds),
                        subText = "Open-Ended Practice",
                        isPaused = false,
                        isOverrun = false
                    )
                } else {
                    // Target Countdown & Overrun mode
                    val remaining = targetDurationSec - elapsedSeconds
                    if (remaining > 0) {
                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = remaining,
                            elapsedSeconds = elapsedSeconds,
                            isOverrun = false
                        )
                        updateNotification(
                            timeText = NotificationHelper.formatSeconds(remaining),
                            subText = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                            isPaused = false,
                            isOverrun = false
                        )
                    } else {
                        // In Overrun / Samadhi Extended Mode
                        overrunSeconds = elapsedSeconds - targetDurationSec
                        
                        if (!hasChimedForOverrun) {
                            hasChimedForOverrun = true
                            soundHelper.triggerHarmonicCompletionHaptic()
                            soundHelper.playChime(soundBellResName, serviceScope)
                        }

                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = 0,
                            elapsedSeconds = elapsedSeconds,
                            isOverrun = true
                        )
                        updateNotification(
                            timeText = "+${NotificationHelper.formatSeconds(overrunSeconds)}",
                            subText = "Extended Time • Total: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                            isPaused = false,
                            isOverrun = true
                        )
                    }
                }

                delay(1000L)
            }
        }
    }

    private fun pauseTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Running) {
            timerJob?.cancel()
            _timerStatus.value = TimerStatus.Paused(
                remainingSeconds = current.remainingSeconds,
                elapsedSeconds = current.elapsedSeconds,
                isOverrun = current.isOverrun
            )
            updateNotification(
                timeText = if (current.isOverrun) "+${NotificationHelper.formatSeconds(overrunSeconds)}" else NotificationHelper.formatSeconds(current.remainingSeconds),
                subText = "Paused",
                isPaused = true,
                isOverrun = current.isOverrun
            )
            wakeLockHelper.releaseWakeLock()
        }
    }

    private fun resumeTimer() {
        val current = _timerStatus.value
        if (current is TimerStatus.Paused) {
            wakeLockHelper.acquireWakeLock()
            timerJob = serviceScope.launch {
                while (isActive) {
                    elapsedSeconds++

                    if (isOpenEnded) {
                        _timerStatus.value = TimerStatus.Running(
                            remainingSeconds = 0,
                            elapsedSeconds = elapsedSeconds,
                            isOverrun = false
                        )
                        updateNotification(
                            timeText = NotificationHelper.formatSeconds(elapsedSeconds),
                            subText = "Open-Ended Practice",
                            isPaused = false,
                            isOverrun = false
                        )
                    } else {
                        val remaining = targetDurationSec - elapsedSeconds
                        if (remaining > 0) {
                            _timerStatus.value = TimerStatus.Running(
                                remainingSeconds = remaining,
                                elapsedSeconds = elapsedSeconds,
                                isOverrun = false
                            )
                            updateNotification(
                                timeText = NotificationHelper.formatSeconds(remaining),
                                subText = "Elapsed: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                                isPaused = false,
                                isOverrun = false
                            )
                        } else {
                            overrunSeconds = elapsedSeconds - targetDurationSec
                            if (!hasChimedForOverrun) {
                                hasChimedForOverrun = true
                                soundHelper.playChime(soundBellResName, serviceScope)
                            }
                            _timerStatus.value = TimerStatus.Running(
                                remainingSeconds = 0,
                                elapsedSeconds = elapsedSeconds,
                                isOverrun = true
                            )
                            updateNotification(
                                timeText = "+${NotificationHelper.formatSeconds(overrunSeconds)}",
                                subText = "Extended Time • Total: ${NotificationHelper.formatSeconds(elapsedSeconds)}",
                                isPaused = false,
                                isOverrun = true
                            )
                        }
                    }

                    delay(1000L)
                }
            }
        }
    }

    private fun finishTimer() {
        timerJob?.cancel()
        val completedMs = System.currentTimeMillis()

        _timerStatus.value = TimerStatus.Completed(
            targetDurationSec = targetDurationSec,
            actualDurationSec = elapsedSeconds,
            overrunSec = overrunSeconds,
            startTimeEpochMs = startTimeEpochMs,
            completedTimeEpochMs = completedMs
        )

        cleanupAndStop()
    }

    private fun cancelTimer() {
        timerJob?.cancel()
        _timerStatus.value = TimerStatus.Idle
        cleanupAndStop()
    }

    private fun cleanupAndStop() {
        if (isDndEnabledForSession) {
            dndManager.restoreDnd()
        }
        wakeLockHelper.releaseWakeLock()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startForegroundServiceWithNotification(
        timeText: String,
        subText: String,
        isPaused: Boolean,
        isOverrun: Boolean
    ) {
        val notification = notificationHelper.buildTimerNotification(
            timeText = timeText,
            subText = subText,
            isPaused = isPaused,
            isOverrun = isOverrun
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NotificationHelper.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(
        timeText: String,
        subText: String,
        isPaused: Boolean,
        isOverrun: Boolean
    ) {
        val notification = notificationHelper.buildTimerNotification(
            timeText = timeText,
            subText = subText,
            isPaused = isPaused,
            isOverrun = isOverrun
        )
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
    }

    override fun onDestroy() {
        timerJob?.cancel()
        serviceScope.cancel()
        cleanupAndStop()
        super.onDestroy()
        Log.d(TAG, "MeditationTimerService destroyed")
    }

    companion object {
        private const val TAG = "MeditationTimerService"

        const val ACTION_START = "com.samadhi.meditationtimer.action.START"
        const val ACTION_PAUSE = "com.samadhi.meditationtimer.action.PAUSE"
        const val ACTION_RESUME = "com.samadhi.meditationtimer.action.RESUME"
        const val ACTION_FINISH = "com.samadhi.meditationtimer.action.FINISH"
        const val ACTION_CANCEL = "com.samadhi.meditationtimer.action.CANCEL"

        const val EXTRA_TARGET_SEC = "extra_target_sec"
        const val EXTRA_PREP_SEC = "extra_prep_sec"
        const val EXTRA_AUTO_DND = "extra_auto_dnd"
        const val EXTRA_OPEN_ENDED = "extra_open_ended"
        const val EXTRA_SOUND_BELL = "extra_sound_bell"
        const val EXTRA_MINDFULNESS_INTERVAL_MIN = "extra_mindfulness_interval_min"

        private val _timerStatus = MutableStateFlow<TimerStatus>(TimerStatus.Idle)
        val timerStatus: StateFlow<TimerStatus> = _timerStatus.asStateFlow()

        fun resetStatus() {
            _timerStatus.value = TimerStatus.Idle
        }
    }
}
