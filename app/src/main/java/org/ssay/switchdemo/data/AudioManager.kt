package org.ssay.switchdemo.data

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import org.ssay.switchdemo.R

class AudioManager(private val context: Context) {

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
        } catch (_: Exception) { }
    }

    fun stopEngineLoop() {
        try {
            enginePlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) { }
        enginePlayer = null
        isEngineLooping = false
    }

    fun playHorn() {
        if (isHornLoaded) {
            hornStreamId = soundPool?.play(hornSoundId, 1f, 1f, 1, -1, 1f) ?: 0
        }
    }

    fun stopHorn() {
        if (hornStreamId != 0) {
            soundPool?.stop(hornStreamId)
            hornStreamId = 0
        }
    }

    fun release() {
        stopEngineLoop()
        stopHorn()
        soundPool?.release()
        soundPool = null
    }
}
