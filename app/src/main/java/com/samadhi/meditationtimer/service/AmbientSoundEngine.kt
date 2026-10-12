package com.samadhi.meditationtimer.service

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.samadhi.meditationtimer.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Loops a bundled ambient field recording at low volume.
 *
 * All clips are seamless loops or long enough that repetition is unnoticeable.
 * Playback fades in/out to avoid abrupt starts and stops.
 *
 * ---------------------------------------------------------------------------
 * AUDIO ATTRIBUTIONS (kept in code per project preference; not shown in-app)
 * ---------------------------------------------------------------------------
 * ambient_rain.ogg   - "Rain Slowly Passing (Treated Loop)" by speakwithanimals
 *                      (Freesound), CC0 1.0. Trimmed, softened + crossfaded loop.
 * ambient_ocean.ogg  - BigSoundBank #0266 "Sea Waves" (Joseph SARDIN), CC0 1.0.
 * ambient_stream.ogg - BigSoundBank #0864 "Brooklet - small mountain stream"
 *                      (Joseph SARDIN), CC0 1.0.
 * ambient_chant.ogg  - "Free Soul Opera Style Om Mani Padme Hum Chants" by
 *                      kalsstockmedia via Pixabay (Pixabay Content License).
 * ambient_noise.ogg  - Seamless brown noise generated with FFmpeg
 *                      (`anoisesrc=color=brown`); public domain / no copyright.
 * guided_thanissaro_metta.mp3  - "01 Guided Metta (4min)" by Thanissaro Bhikkhu,
 *                                dhammatalks.org. MP3 metadata allows free
 *                                redistribution in any medium.
 * guided_thanissaro_breath.mp3 - "05 Guided Breath - Quick Version (12min)" by
 *                                Thanissaro Bhikkhu, same free-distribution terms.
 * guided_sundara_intro.mp3     - "Guided Meditation" by Ajahn Sundara,
 *                                AudioDharma, CC BY-NC-ND 4.0, unchanged.
 * guided_gloria_short.mp3      - "Guided Meditation" by Gloria Taraniya Ambrosia,
 *                                AudioDharma, CC BY-NC-ND 4.0, unchanged.
 * guided_gil_short.mp3         - "Guided Meditation" by Gil Fronsdal,
 *                                AudioDharma, CC BY-NC-ND 4.0, unchanged.
 *
 * CC0 sources require no attribution; the Pixabay Content License allows
 * royalty-free commercial use inside the app (not as a standalone download).
 */
class AmbientSoundEngine(context: Context) {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var player: MediaPlayer? = null
    private var job: Job? = null

    @Volatile private var paused: Boolean = false
    @Volatile private var targetVolume: Float = DEFAULT_VOLUME

    /** Begins (or replaces) the ambient soundscape. [TYPE_SILENCE] stops playback. */
    fun start(type: String, volume: Float = DEFAULT_VOLUME, loop: Boolean = true) {
        stop()
        val resId = resIdFor(type) ?: return
        targetVolume = volume
        paused = false
        job = scope.launch {
            try {
                val attributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
                val mp = MediaPlayer.create(appContext, resId, attributes, 0) ?: return@launch
                mp.isLooping = loop
                mp.setVolume(0f, 0f)
                mp.start()
                player = mp
                fadeIn(mp)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to start ambient soundscape ($type)", e)
            }
        }
    }

    /** Gracefully fades out and releases the current soundscape. */
    fun stop() {
        val mp = player
        player = null
        job?.cancel()
        job = null
        paused = false
        if (mp != null) {
            scope.launch {
                try {
                    val steps = 9
                    for (i in steps downTo 0) {
                        val v = targetVolume * i / steps
                        mp.setVolume(v, v)
                        delay(FADE_OUT_MS / steps)
                    }
                    mp.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping ambient soundscape", e)
                } finally {
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }

    /** Silences playback while keeping the soundscape loaded. */
    fun pause() {
        paused = true
        try {
            player?.pause()
        } catch (e: Exception) {
            Log.w(TAG, "Error pausing ambient soundscape", e)
        }
    }

    /** Resumes playback after [pause]. */
    fun resume() {
        paused = false
        try {
            player?.start()
        } catch (e: Exception) {
            Log.w(TAG, "Error resuming ambient soundscape", e)
        }
    }

    fun isPlaying(): Boolean = try {
        player?.isPlaying == true
    } catch (_: Exception) {
        false
    }

    private suspend fun fadeIn(mp: MediaPlayer) {
        val steps = 25
        for (i in 1..steps) {
            if (player !== mp) return
            val v = targetVolume * i / steps
            try {
                mp.setVolume(v, v)
            } catch (_: Exception) {
                return
            }
            delay(FADE_IN_MS / steps)
        }
    }

    companion object {
        private const val TAG = "AmbientSoundEngine"
        private const val FADE_IN_MS = 2500L
        private const val FADE_OUT_MS = 900L
        const val DEFAULT_VOLUME = 0.55f

        const val TYPE_SILENCE = "silence"
        const val TYPE_RAIN = "ambient_rain"
        const val TYPE_OCEAN = "ambient_ocean"
        const val TYPE_STREAM = "ambient_stream"
        const val TYPE_CHANT = "ambient_chant"
        const val TYPE_NOISE = "ambient_noise"
        const val TYPE_GUIDED_THANISSARO_METTA = "guided_thanissaro_metta"
        const val TYPE_GUIDED_THANISSARO_BREATH = "guided_thanissaro_breath"
        const val TYPE_GUIDED_SUNDARA_INTRO = "guided_sundara_intro"
        const val TYPE_GUIDED_GLORIA_SHORT = "guided_gloria_short"
        const val TYPE_GUIDED_GIL_SHORT = "guided_gil_short"

        fun resIdFor(type: String): Int? = when (type) {
            TYPE_RAIN -> R.raw.ambient_rain
            TYPE_OCEAN -> R.raw.ambient_ocean
            TYPE_STREAM -> R.raw.ambient_stream
            TYPE_CHANT -> R.raw.ambient_chant
            TYPE_NOISE -> R.raw.ambient_noise
            TYPE_GUIDED_THANISSARO_METTA -> R.raw.guided_thanissaro_metta
            TYPE_GUIDED_THANISSARO_BREATH -> R.raw.guided_thanissaro_breath
            TYPE_GUIDED_SUNDARA_INTRO -> R.raw.guided_sundara_intro
            TYPE_GUIDED_GLORIA_SHORT -> R.raw.guided_gloria_short
            TYPE_GUIDED_GIL_SHORT -> R.raw.guided_gil_short
            else -> null
        }
    }
}
