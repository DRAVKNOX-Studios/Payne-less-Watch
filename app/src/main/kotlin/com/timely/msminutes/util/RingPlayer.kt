package com.timely.msminutes.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import kotlin.math.max
import kotlin.math.min

class RingPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private val handler = Handler(Looper.getMainLooper())
    private var volumeRunnable: Runnable? = null
    private var playing = false

    fun start(
        context: Context,
        soundUri: String?,
        vibrate: Boolean,
        gradualVolume: Boolean,
        rampSeconds: Int
    ) {
        stop()
        if (soundUri != null) {
            try {
                val uri = Uri.parse(soundUri)
                mediaPlayer = MediaPlayer()
                mediaPlayer!!.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mediaPlayer!!.setDataSource(context, uri)
                mediaPlayer!!.setLooping(true)
                val startVolume = if (gradualVolume) 0.05f else 1.0f
                mediaPlayer!!.setVolume(startVolume, startVolume)
                mediaPlayer!!.prepare()
                mediaPlayer!!.start()
                playing = true

                if (gradualVolume) {
                    rampVolume(rampSeconds)
                }
            } catch (ignored: Exception) {
            }
        }

        if (vibrate) {
            vibrator = context.getSystemService(Vibrator::class.java)
            if (vibrator != null) {
                val pattern = longArrayOf(0, 800, 500)
                vibrator!!.vibrate(VibrationEffect.createWaveform(pattern, 0))
            }
        }
    }

    private fun rampVolume(rampSeconds: Int) {
        val steps = max(rampSeconds, 5)
        val stepDelay = 1000L
        val currentStep = intArrayOf(0)
        volumeRunnable = object : Runnable {
            override fun run() {
                if (!playing || mediaPlayer == null) {
                    return
                }
                currentStep[0]++
                val fraction = min(1f, currentStep[0].toFloat() / steps)
                val volume = 0.05f + fraction * 0.95f
                try {
                    mediaPlayer!!.setVolume(volume, volume)
                } catch (ignored: Exception) {
                }
                if (fraction < 1f) {
                    handler.postDelayed(this, stepDelay)
                }
            }
        }
        handler.postDelayed(volumeRunnable!!, stepDelay)
    }

    fun stop() {
        playing = false
        if (volumeRunnable != null) {
            handler.removeCallbacks(volumeRunnable!!)
            volumeRunnable = null
        }
        if (mediaPlayer != null) {
            try {
                if (mediaPlayer!!.isPlaying()) {
                    mediaPlayer!!.stop()
                }
                mediaPlayer!!.release()
            } catch (ignored: Exception) {
            }
            mediaPlayer = null
        }
        if (vibrator != null) {
            vibrator!!.cancel()
            vibrator = null
        }
    }
}
