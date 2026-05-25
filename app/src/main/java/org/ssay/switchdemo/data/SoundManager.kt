package org.ssay.switchdemo.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import org.ssay.switchdemo.R

/**
 * FIXED #42: was named "AudioManager", which shadowed `android.media.AudioManager`.
 * Renamed to SoundManager to avoid confusing import resolution.
 *
 * FIXED #56: provides a custom horn-vibration pattern alongside the audio cue
 * so the haptic feedback is no longer the same generic LongPress for every event.
 */
class SoundManager(private val context: Context) {

    private var enginePlayer: MediaPlayer? = null
    private var soundPool: SoundPool? = null
    private var hornSoundId: Int = 0
    private var hornStreamId: Int = 0
    private var isHornLoaded = false
    private var isEngineLooping = false

    init {
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(2)
            .setAudioAttributes(attrs)
            .build()
            .also { pool ->
                pool.setOnLoadCompleteListener { _, _, status ->
                    if (status == 0) isHornLoaded = true
                }
                hornSoundId = pool.load(context, R.raw.horn, 1)
            }
    }

    fun playEngineLoop() {
        if (isEngineLooping) return
        try {
            enginePlayer = MediaPlayer.create(context, R.raw.two_wheeler_start)?.apply {
                isLooping = true
                start()
            }
            isEngineLooping = true
        } catch (_: Exception) { /* best-effort */ }
    }

    fun stopEngineLoop() {
        try {
            enginePlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) { /* best-effort */ }
        enginePlayer = null
        isEngineLooping = false
    }

    fun playHorn() {
        if (isHornLoaded) {
            hornStreamId = soundPool?.play(hornSoundId, 1f, 1f, 1, -1, 1f) ?: 0
        }
        // FIXED #56: distinct horn-pattern haptic.
        vibrateHornPattern()
    }

    fun stopHorn() {
        if (hornStreamId != 0) {
            soundPool?.stop(hornStreamId)
            hornStreamId = 0
        }
    }

    private fun vibrateHornPattern() {
        val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        vibrator ?: return
        try {
            // Three short pulses to mimic a horn beep.
            val timings   = longArrayOf(0, 80, 60, 80, 60, 80)
            val amplitude = intArrayOf(0, 200, 0, 200, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitude, -1))
        } catch (_: Exception) { /* best-effort */ }
    }

    fun release() {
        stopEngineLoop()
        stopHorn()
        soundPool?.release()
        soundPool = null
    }
}
