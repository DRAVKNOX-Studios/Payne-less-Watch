package com.timely.msminutes.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.receiver.NotificationActionReceiver
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.util.NotificationChannels
import com.timely.msminutes.util.TimeFormatUtil

class StopwatchService : Service() {
    private var prefs: Prefs? = null
    private val handler = Handler(Looper.getMainLooper())
    private var tickRunnable: Runnable? = null
    private var running = false

    private var stopPi: PendingIntent? = null
    private var lapPi: PendingIntent? = null
    private var contentPi: PendingIntent? = null

    private var lastDisplayedText: String = ""
    private var builder: NotificationCompat.Builder? = null

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        buildPendingIntents()
        builder = NotificationCompat.Builder(this, NotificationChannels.STOPWATCH_RUNNING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Stopwatch")
            .setOngoing(true)
            .setContentIntent(contentPi)
            .addAction(0, "Lap", lapPi)
            .addAction(0, "End", stopPi)
    }

    private fun buildPendingIntents() {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

        val stopIntent = Intent(this, NotificationActionReceiver::class.java)
            .setAction(NotificationActionReceiver.ACTION_STOPWATCH_END)
        stopPi = PendingIntent.getBroadcast(this, 1, stopIntent, flags)

        val lapIntent = Intent(this, NotificationActionReceiver::class.java)
            .setAction(NotificationActionReceiver.ACTION_STOPWATCH_LAP)
        lapPi = PendingIntent.getBroadcast(this, 2, lapIntent, flags)

        val contentIntent = Intent(this, MainActivity::class.java)
        contentPi = PendingIntent.getActivity(this, 3, contentIntent, flags)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent.action

        if (ACTION_STOP == action) {
            prefs?.isStopwatchRunning = false
            stopTicking()
            NotificationManagerCompat.from(this).cancel(NOTIF_ID)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            stopSelf()
            return START_NOT_STICKY
        }

        if (ACTION_LAP == action) {
            return START_STICKY
        }

        prefs?.isStopwatchRunning = true
        startForeground(NOTIF_ID, buildNotification())
        startTicking()
        return START_STICKY
    }

    private fun startTicking() {
        stopTicking()
        running = true
        tickRunnable = object : Runnable {
            override fun run() {
                if (!running) return
                val p = prefs ?: return
                val elapsed = p.lastStopwatchElapsed +
                        (System.currentTimeMillis() - p.stopwatchStartBase)
                val text = TimeFormatUtil.formatStopwatch(elapsed)
                if (text != lastDisplayedText) {
                    lastDisplayedText = text
                    val nm = NotificationManagerCompat.from(this@StopwatchService)
                    try {
                        nm.notify(NOTIF_ID, buildNotification(text))
                    } catch (ignored: SecurityException) {
                    }
                }
                handler.postDelayed(this, TICK_INTERVAL_MS)
            }
        }
        handler.postDelayed(tickRunnable!!, TICK_INTERVAL_MS)
    }

    private fun stopTicking() {
        running = false
        tickRunnable?.let { handler.removeCallbacks(it) }
        tickRunnable = null
    }

    private fun buildNotification(text: String = ""): Notification {
        val p = prefs
        val displayText = if (text.isNotEmpty()) text else {
            val base = p?.stopwatchStartBase ?: 0L
            val elapsed = (p?.lastStopwatchElapsed ?: 0L) +
                    if (base > 0) (System.currentTimeMillis() - base) else 0L
            TimeFormatUtil.formatStopwatch(elapsed)
        }
        val b = builder ?: NotificationCompat.Builder(this, NotificationChannels.STOPWATCH_RUNNING)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Stopwatch")
            .setOngoing(true)
            .setContentIntent(contentPi)
            .addAction(0, "Lap", lapPi)
            .addAction(0, "End", stopPi)
            .also { builder = it }

        return b.setContentText(displayText).build()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopTicking()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START: String = "action_stopwatch_start"
        const val ACTION_STOP: String = "action_stopwatch_stop"
        const val ACTION_LAP: String = "action_stopwatch_lap"

        private const val NOTIF_ID = 9201
        private const val TICK_INTERVAL_MS = 1000L
    }
}
