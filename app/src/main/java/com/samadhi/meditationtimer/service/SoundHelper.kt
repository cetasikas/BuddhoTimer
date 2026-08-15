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
     * Plays the selected chime profile (Tibetan Singing Bowl, Zen Bell, Ting-Sha, or Silent Haptic)
     */
    fun playChime(
        soundType: String = SOUND_TIBETAN_BOWL,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
    ) {
        triggerGentleVibration()

        if (soundType == SOUND_SILENT_HAPTIC) {
            // Haptic-only mode
            return
        }

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
     * Deep Tibetan Singing Bowl: Warm sub-harmonics, gentle pulsing beat, soothing resonant decay
     */
    private fun synthesizeSingingBowlSound() {
        val sampleRate = 44100
        val durationSeconds = 4.5
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        // Fundamental frequencies for warm Tibetan bronze bowl (F3 fundamental ~174Hz, soothing harmonics)
        val f1 = 174.61 // Deep fundamental
        val f2 = 432.00 // Resonant 2nd harmonic
        val f3 = 528.00 // Solfeggio overtone
        val f4 = 864.00 // Shimmer

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            // Smooth 20ms attack to eliminate clicks + exponential decay
            val attack = if (t < 0.02) (t / 0.02) else 1.0
            val decay = exp(-0.85 * t)
            val envelope = attack * decay

            // Gentle 2.5 Hz binaural amplitude modulation (singing bowl rim pulse)
            val beat = 1.0 + 0.12 * sin(2.0 * PI * 2.5 * t)

            val s1 = sin(2.0 * PI * f1 * t) * 0.58
            val s2 = sin(2.0 * PI * f2 * t) * 0.25 * exp(-1.2 * t)
            val s3 = sin(2.0 * PI * f3 * t) * 0.12 * exp(-1.8 * t)
            val s4 = sin(2.0 * PI * f4 * t) * 0.05 * exp(-2.5 * t)

            val sample = (s1 + s2 + s3 + s4) * beat * envelope
            val clamped = sample.coerceIn(-0.95, 0.95)
            buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Zen Bronze Temple Bell (Keisu): Rich bronze strike with clear harmonic ring and pure sustained overtone
     */
    private fun synthesizeZenTempleBell() {
        val sampleRate = 44100
        val durationSeconds = 5.0
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 261.63 // C4 Temple fundamental
        val f2 = 523.25 // C5 Octave
        val f3 = 784.00 // G5 Harmonic fifth
        val f4 = 1174.66 // D6 Bronze shimmer

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val attack = if (t < 0.008) (t / 0.008) else 1.0
            val decay = exp(-0.75 * t)
            val envelope = attack * decay

            // Bronze strike transient
            val strike = if (t < 0.05) sin(2.0 * PI * 800.0 * t) * exp(-40.0 * t) * 0.15 else 0.0

            val s1 = sin(2.0 * PI * f1 * t) * 0.60
            val s2 = sin(2.0 * PI * f2 * t) * 0.22 * exp(-1.0 * t)
            val s3 = sin(2.0 * PI * f3 * t) * 0.12 * exp(-1.5 * t)
            val s4 = sin(2.0 * PI * f4 * t) * 0.06 * exp(-3.0 * t)

            val sample = (s1 + s2 + s3 + s4 + strike) * envelope
            val clamped = sample.coerceIn(-0.95, 0.95)
            buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Ting-Sha Chimes: High, crystal-clear Tibetan cymbal chime with pure lingering resonance
     */
    private fun synthesizeTingshaChime() {
        val sampleRate = 44100
        val durationSeconds = 4.0
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 2093.00 // C7 Bright celestial fundamental
        val f2 = 2101.00 // Slight detuning for authentic Ting-Sha shimmer
        val f3 = 4186.00 // High sparkle

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val attack = if (t < 0.005) (t / 0.005) else 1.0
            val decay = exp(-0.95 * t)
            val envelope = attack * decay

            val s1 = sin(2.0 * PI * f1 * t) * 0.50
            val s2 = sin(2.0 * PI * f2 * t) * 0.40
            val s3 = sin(2.0 * PI * f3 * t) * 0.10 * exp(-2.0 * t)

            val sample = (s1 + s2 + s3) * envelope
            val clamped = sample.coerceIn(-0.95, 0.95)
            buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    /**
     * Moktak / Temple Woodblock: Organic mellow wooden strike
     */
    private fun synthesizeMoktak() {
        val sampleRate = 44100
        val durationSeconds = 1.2
        val numSamples = (durationSeconds * sampleRate).toInt()
        val buffer = ShortArray(numSamples)

        val f1 = 587.33 // D5 Wooden body
        val f2 = 880.00 // A5 Wood overtone

        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate

            val attack = if (t < 0.003) (t / 0.003) else 1.0
            val decay = exp(-7.0 * t)
            val envelope = attack * decay

            val s1 = sin(2.0 * PI * f1 * t) * 0.70
            val s2 = sin(2.0 * PI * f2 * t) * 0.30

            val sample = (s1 + s2) * envelope
            val clamped = sample.coerceIn(-0.95, 0.95)
            buffer[i] = (clamped * Short.MAX_VALUE).toInt().toShort()
        }

        playPcmBuffer(buffer, sampleRate)
    }

    private fun playPcmBuffer(buffer: ShortArray, sampleRate: Int) {
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
                // Triple gentle meditative wave: wait 0ms, vibrate 120ms, wait 180ms, vibrate 160ms, wait 180ms, vibrate 220ms
                val timings = longArrayOf(0, 120, 180, 160, 180, 220)
                val amplitudes = intArrayOf(0, 70, 0, 90, 0, 120)
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

    companion object {
        private const val TAG = "SoundHelper"

        const val SOUND_TIBETAN_BOWL = "tibetan_singing_bowl"
        const val SOUND_ZEN_BELL = "zen_temple_bell"
        const val SOUND_TINGSHA = "tingsha_chime"
        const val SOUND_MOKTAK = "woodblock_moktak"
        const val SOUND_SILENT_HAPTIC = "silent_haptic"

        val soundOptions = listOf(
            SoundOption(SOUND_TIBETAN_BOWL, "Tibetan Singing Bowl", "Warm, deep harmonic resonance"),
            SoundOption(SOUND_ZEN_BELL, "Zen Bronze Bell", "Pure temple bell strike and sustain"),
            SoundOption(SOUND_TINGSHA, "Ting-Sha Cymbals", "Bright, clear celestial chime"),
            SoundOption(SOUND_MOKTAK, "Temple Woodblock", "Organic wooden meditation knock"),
            SoundOption(SOUND_SILENT_HAPTIC, "Silent Retreat (Haptic Only)", "Gentle soothing vibration without audio")
        )
    }
}

data class SoundOption(
    val id: String,
    val title: String,
    val description: String
)
