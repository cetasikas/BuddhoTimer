package com.samadhi.meditationtimer.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

class SoundHelper(private val context: Context) {

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val manager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            manager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private val audioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * Plays the selected chime profile (Mindfulness Bell, Great Temple Bell, Awakening Bell, Deep Singing Bowl, Zen Tone, Mindful Chime, or Silent Haptic)
     */
    fun playChime(
        soundType: String = SOUND_MINDFULNESS_BELL,
        scope: CoroutineScope = audioScope
    ) {
        triggerGentleVibration()

        if (soundType == SOUND_SILENT_HAPTIC) {
            // Haptic-only mode
            return
        }

        val rawResId = when (soundType) {
            SOUND_TEMPLE_DEEP -> com.samadhi.meditationtimer.R.raw.bell_temple_deep
            SOUND_MINDFULNESS_BELL -> com.samadhi.meditationtimer.R.raw.bell_mindfulness
            SOUND_AWAKENING -> com.samadhi.meditationtimer.R.raw.bell_awakening
            SOUND_SINGING_BOWL -> com.samadhi.meditationtimer.R.raw.singing_bowl
            SOUND_ZEN_TONE -> com.samadhi.meditationtimer.R.raw.zen_tone
            SOUND_CHIME -> com.samadhi.meditationtimer.R.raw.chime
            // Aliases for backwards compatibility
            "pv_bell_big", "zen_temple_bell" -> com.samadhi.meditationtimer.R.raw.bell_temple_deep
            "pv_bell_medium", "tibetan_singing_bowl" -> com.samadhi.meditationtimer.R.raw.bell_mindfulness
            "pv_bell_small", "tingsha_chime" -> com.samadhi.meditationtimer.R.raw.bell_awakening
            "woodblock_moktak" -> com.samadhi.meditationtimer.R.raw.zen_tone
            else -> com.samadhi.meditationtimer.R.raw.bell_mindfulness
        }

        playRawSound(rawResId, soundType, scope)
    }

    private fun playRawSound(rawResId: Int, soundType: String, scope: CoroutineScope) {
        try {
            val mediaPlayer = android.media.MediaPlayer.create(context, rawResId)
            if (mediaPlayer != null) {
                mediaPlayer.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                mediaPlayer.setOnCompletionListener { mp ->
                    try {
                        mp.release()
                    } catch (e: Exception) {
                        Log.w(TAG, "Error releasing MediaPlayer", e)
                    }
                }
                mediaPlayer.start()
                return
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaPlayer failed for raw res $rawResId, falling back to synthesis", e)
        }

        // Fallback to synthesis if raw media failed
        scope.launch(Dispatchers.IO) {
            try {
                when (soundType) {
                    SOUND_ZEN_BELL -> synthesizeZenTempleBell()
                    SOUND_TINGSHA -> synthesizeTingshaChime()
                    SOUND_MOKTAK -> synthesizeMoktak()
                    else -> synthesizeSingingBowlSound()
                }
            } catch (e: Exception) {
                Log.w(TAG, "Audio synthesis failed, falling back to system notification sound", e)
                playSystemFallbackSound()
            }
        }
    }

    /**
     * Deep Tibetan Singing Bowl: Warm, mellow bronze resonance with soft 50ms attack & soothing 1.8Hz binaural pulse
     */
    private suspend fun synthesizeSingingBowlSound() {
        val sampleRate = 44100
        val durationSeconds = 5.0
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        // Warm, grounding frequencies (F3 fundamental ~174.6Hz with pure harmonics)
        val f1 = 174.61 // Deep fundamental
        val f2 = 349.23 // Octave F4
        val f3 = 523.25 // Fifth C5
        val f4 = 784.00 // G5 Soft overtone

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            // Smooth 60ms non-linear attack to ensure zero jarring click
            val attack = if (t < 0.06) (sin(PI * t / (2.0 * 0.06))) else 1.0
            val decay = exp(-0.65 * t)
            val envelope = attack * decay

            // Gentle 1.8 Hz binaural pulsation (singing bowl rim resonance)
            val beat = 1.0 + 0.08 * sin(2.0 * PI * 1.8 * t)

            val s1 = sin(2.0 * PI * f1 * t) * 0.65
            val s2 = sin(2.0 * PI * f2 * t) * 0.20 * exp(-0.9 * t)
            val s3 = sin(2.0 * PI * f3 * t) * 0.10 * exp(-1.4 * t)
            val s4 = sin(2.0 * PI * f4 * t) * 0.04 * exp(-2.0 * t)

            val sample = (s1 + s2 + s3 + s4) * beat * envelope
            val clamped = sample.coerceIn(-0.90, 0.90)
            buffer[i] = (clamped * Short.MAX_VALUE * 0.85).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Zen Bronze Temple Bell (Keisu): Rich bronze warmth with gentle mallet strike and pure sustained ring
     */
    private suspend fun synthesizeZenTempleBell() {
        val sampleRate = 44100
        val durationSeconds = 5.5
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 220.00 // A3 Warm temple body
        val f2 = 440.00 // A4 Octave
        val f3 = 659.25 // E5 Harmonious fifth
        val f4 = 880.00 // A5 Shimmer

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            // Smooth 40ms attack (soft padded wooden mallet)
            val attack = if (t < 0.04) (sin(PI * t / (2.0 * 0.04))) else 1.0
            val decay = exp(-0.55 * t)
            val envelope = attack * decay

            // Low frequency mallet thump (subtle 80Hz cushion)
            val mallet = if (t < 0.08) sin(2.0 * PI * 80.0 * t) * exp(-25.0 * t) * 0.12 else 0.0

            val s1 = sin(2.0 * PI * f1 * t) * 0.62
            val s2 = sin(2.0 * PI * f2 * t) * 0.22 * exp(-0.8 * t)
            val s3 = sin(2.0 * PI * f3 * t) * 0.10 * exp(-1.2 * t)
            val s4 = sin(2.0 * PI * f4 * t) * 0.04 * exp(-2.0 * t)

            val sample = (s1 + s2 + s3 + s4 + mallet) * envelope
            val clamped = sample.coerceIn(-0.90, 0.90)
            buffer[i] = (clamped * Short.MAX_VALUE * 0.85).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Ting-Sha Chimes: Crystal-clear, soft celestial chime with pure lingering resonance
     */
    private suspend fun synthesizeTingshaChime() {
        val sampleRate = 44100
        val durationSeconds = 4.5
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 1760.00 // A6 Silky celestial fundamental
        val f2 = 1766.00 // Gentle natural shimmer beating (6Hz)
        val f3 = 3520.00 // A7 Soft sparkle

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val attack = if (t < 0.03) (sin(PI * t / (2.0 * 0.03))) else 1.0
            val decay = exp(-0.70 * t)
            val envelope = attack * decay

            val s1 = sin(2.0 * PI * f1 * t) * 0.55
            val s2 = sin(2.0 * PI * f2 * t) * 0.38
            val s3 = sin(2.0 * PI * f3 * t) * 0.06 * exp(-1.5 * t)

            val sample = (s1 + s2 + s3) * envelope
            val clamped = sample.coerceIn(-0.85, 0.85)
            buffer[i] = (clamped * Short.MAX_VALUE * 0.75).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Moktak / Temple Woodblock: Organic warm resonant wooden knock
     */
    private suspend fun synthesizeMoktak() {
        val sampleRate = 44100
        val durationSeconds = 1.4
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 440.00 // A4 Warm wood cavity
        val f2 = 660.00 // E5 Wood body

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val attack = if (t < 0.015) (sin(PI * t / (2.0 * 0.015))) else 1.0
            val decay = exp(-5.5 * t)
            val envelope = attack * decay

            val s1 = sin(2.0 * PI * f1 * t) * 0.75
            val s2 = sin(2.0 * PI * f2 * t) * 0.25 * exp(-8.0 * t)

            val sample = (s1 + s2) * envelope
            val clamped = sample.coerceIn(-0.90, 0.90)
            buffer[i] = (clamped * Short.MAX_VALUE * 0.80).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private suspend fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
        var audioTrack: AudioTrack? = null
        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(buffer.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()

            audioTrack.write(buffer, 0, buffer.size)
            audioTrack.play()

            val durationMs = (buffer.size.toDouble() / sampleRate * 1000.0).toLong() + 250L
            kotlinx.coroutines.delay(durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack playback error", e)
            playSystemFallbackSound()
        } finally {
            try {
                if (audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING) {
                    audioTrack.stop()
                }
                audioTrack?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing AudioTrack", e)
            }
        }
    }

    private fun playSystemFallbackSound() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing fallback ringtone", e)
        }
    }

    fun triggerGentleVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Triple gentle meditative wave: wait 0ms, vibrate 120ms, wait 180ms, vibrate 160ms, wait 180ms, vibrate 220ms
                val timings = longArrayOf(0, 120, 180, 160, 180, 220)
                val amplitudes = intArrayOf(0, 50, 0, 75, 0, 95)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 120, 180, 160, 180, 220), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    fun triggerHarmonicIntervalHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Gentle dual-pulse interval ripple
                val timings = longArrayOf(0, 90, 140, 140)
                val amplitudes = intArrayOf(0, 45, 0, 70)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 90, 140, 140), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering interval haptic", e)
        }
    }

    fun triggerHarmonicCompletionHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Deep resonant 4-stage completion cascade
                val timings = longArrayOf(0, 150, 120, 200, 120, 260, 150, 320)
                val amplitudes = intArrayOf(0, 40, 0, 65, 0, 90, 0, 120)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 150, 120, 200, 120, 260, 150, 320), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering completion haptic", e)
        }
    }

    fun triggerSoftTouchHaptic() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering touch haptic", e)
        }
    }

    companion object {
        private const val TAG = "SoundHelper"

        const val SOUND_MINDFULNESS_BELL = "bell_mindfulness"
        const val SOUND_TEMPLE_DEEP = "bell_temple_deep"
        const val SOUND_AWAKENING = "bell_awakening"
        const val SOUND_SINGING_BOWL = "singing_bowl"
        const val SOUND_ZEN_TONE = "zen_tone"
        const val SOUND_CHIME = "chime"
        const val SOUND_SILENT_HAPTIC = "silent_haptic"

        // Legacy aliases
        const val SOUND_PV_BELL_MEDIUM = SOUND_MINDFULNESS_BELL
        const val SOUND_PV_BELL_BIG = SOUND_TEMPLE_DEEP
        const val SOUND_PV_BELL_SMALL = SOUND_AWAKENING
        const val SOUND_TIBETAN_BOWL = SOUND_MINDFULNESS_BELL
        const val SOUND_ZEN_BELL = SOUND_TEMPLE_DEEP
        const val SOUND_TINGSHA = SOUND_AWAKENING
        const val SOUND_MOKTAK = SOUND_ZEN_TONE

        val soundOptions = listOf(
            SoundOption(SOUND_MINDFULNESS_BELL, "Mindfulness Bell", "Warm, resonant bronze meditation bell (27s)"),
            SoundOption(SOUND_TEMPLE_DEEP, "Great Temple Bell", "Deep sacred bell with gentle awakening pre-strike (29s)"),
            SoundOption(SOUND_AWAKENING, "Awakening Bell", "Clear, gentle bell of presence (16s)"),
            SoundOption(SOUND_CHIME, "Mindful Chime", "Quick soothing acoustic chime (1.6s)"),
            SoundOption(SOUND_SILENT_HAPTIC, "Silent Retreat (Haptic Only)", "Gentle soothing vibration without audio")
        )
    }
}

data class SoundOption(
    val id: String,
    val title: String,
    val description: String
)
