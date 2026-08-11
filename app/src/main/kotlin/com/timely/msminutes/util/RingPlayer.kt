package com.timely.msminutes.util

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
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
                val mp  = MediaPlayer()
                mediaPlayer = mp

                mp.setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                mp.setDataSource(context, uri)
                mp.isLooping = true

                val startVolume = if (gradualVolume) 0.05f else 1.0f
                mp.setVolume(startVolume, startVolume)

                // Use prepareAsync so we never block the calling (main/service) thread.
                // Audio fires from the OnPreparedListener once the codec is ready.
                mp.setOnPreparedListener { player ->
                    if (playing) {
                        player.start()
                        if (gradualVolume) rampVolume(rampSeconds)
                    } else {
                        // stop() was called before preparation completed — release immediately.
                        player.release()
                        mediaPlayer = null
                    }
                }
                mp.setOnErrorListener { player, what, extra ->
                    Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
                    player.release()
                    mediaPlayer = null
                    true // handled
                }

                playing = true
                mp.prepareAsync()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set up MediaPlayer", e)
                mediaPlayer?.release()
                mediaPlayer = null
                playing = false
            }
        }

        if (vibrate) {
            vibrator = context.getSystemService(Vibrator::class.java)
            vibrator?.vibrate(
                VibrationEffect.createWaveform(longArrayOf(0, 800, 500), 0)
            )
        }
    }

    private fun rampVolume(rampSeconds: Int) {
        val steps = max(rampSeconds, 5)
        val stepDelay = 1000L
        val currentStep = intArrayOf(0)
        volumeRunnable = object : Runnable {
            override fun run() {
                val mp = mediaPlayer
                if (!playing || mp == null) return
                currentStep[0]++
                val fraction = min(1f, currentStep[0].toFloat() / steps)
                val volume = 0.05f + fraction * 0.95f
                try {
                    mp.setVolume(volume, volume)
                } catch (ignored: Exception) {
                }
                if (fraction < 1f) handler.postDelayed(this, stepDelay)
            }
        }
        handler.postDelayed(volumeRunnable!!, stepDelay)
    }

    fun stop() {
        playing = false
        volumeRunnable?.let { handler.removeCallbacks(it) }
        volumeRunnable = null
        mediaPlayer?.let { mp ->
            try {
                if (mp.isPlaying) mp.stop()
                mp.release()
            } catch (ignored: Exception) {
            }
        }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
    }

    companion object {
        private const val TAG = "RingPlayer"
    }
}
