package com.samadhi.meditationtimer.service

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.CombinedVibration
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
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

    /**
     * Plays a rich, soothing Tibetan Singing Bowl / Gong acoustic chime synthesized in real-time
     * with harmonic overtones and exponential decay, followed by gentle haptics.
     */
    fun playChime(scope: CoroutineScope = CoroutineScope(Dispatchers.Default)) {
        triggerGentleVibration()

        scope.launch(Dispatchers.IO) {
            try {
                synthesizeSingingBowlSound()
            } catch (e: Exception) {
                Log.w(TAG, "Audio synthesis failed, falling back to system notification sound", e)
                playSystemFallbackSound()
            }
        }
    }

    private fun synthesizeSingingBowlSound() {
        val sampleRate = 44100
        val durationSeconds = 3.5
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        // Fundamental frequencies for singing bowl harmonics (F3 = 174.6Hz, 528Hz love frequency overtone)
        val f1 = 216.0  // Deep warm fundamental
        val f2 = 432.0  // Harmonic octave
        val f3 = 864.0  // High shimmer
        val f4 = 1296.0 // Ambient sparkle

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            
            // Envelope with immediate soft attack and long resonant exponential decay
            val envelope = exp(-1.2 * t)

            val s1 = sin(2.0 * PI * f1 * t) * 0.55
            val s2 = sin(2.0 * PI * f2 * t) * 0.28
            val s3 = sin(2.0 * PI * f3 * t) * 0.12
            val s4 = sin(2.0 * PI * f4 * t) * 0.05

            // Subtle beat oscillation
            val beat = sin(2.0 * PI * 2.0 * t) * 0.1

            val sample = (s1 + s2 + s3 + s4) * (1.0 + beat) * envelope
            val clamped = sample.coerceIn(-1.0, 1.0)
            buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
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
                // Gentle pulse: wait 0ms, vibrate 120ms, wait 150ms, vibrate 200ms
                val timings = longArrayOf(0, 120, 150, 200)
                val amplitudes = intArrayOf(0, 80, 0, 120)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator?.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 120, 150, 200), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering vibration", e)
        }
    }

    companion object {
        private const val TAG = "SoundHelper"
    }
}
