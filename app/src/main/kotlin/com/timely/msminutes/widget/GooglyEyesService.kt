package com.timely.msminutes.widget

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper

/**
 * Service that handles frequent widget updates during the Easter Egg minute.
 * Ticks every 100ms for smooth pupil interpolation.
 */
class GooglyEyesService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val TICK_MS = 100L

    private val updater: Runnable = object : Runnable {
        override fun run() {
            if (GooglyEyesController.isEasterEggMinute(this@GooglyEyesService)) {
                GooglyEyesController.tick(System.currentTimeMillis())
                WidgetNotifier.notifyUpdate(this@GooglyEyesService)
                handler.postDelayed(this, TICK_MS)
            } else {
                GooglyEyesController.reset()
                stopSelf()
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(updater)
        handler.post(updater)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(updater)
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
