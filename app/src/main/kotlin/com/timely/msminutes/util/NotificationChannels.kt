package com.timely.msminutes.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {
    const val ALARM_RING: String = "alarm_ring_v2"
    const val ALARM_MISSED: String = "alarm_missed"
    const val TIMER_RUNNING: String = "timer_running"
    const val STOPWATCH_RUNNING: String = "stopwatch_running"

    fun createAll(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java) ?: return

        nm.deleteNotificationChannel("alarm_ring")

        val ring = NotificationChannel(
            ALARM_RING, "Alarm",
            NotificationManager.IMPORTANCE_HIGH
        )
        ring.setBypassDnd(true)
        ring.setSound(null, null)
        ring.enableVibration(false)
        ring.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
        nm.createNotificationChannel(ring)

        val missed = NotificationChannel(
            ALARM_MISSED, "Missed Alarms",
            NotificationManager.IMPORTANCE_HIGH
        )
        nm.createNotificationChannel(missed)

        val timer = NotificationChannel(
            TIMER_RUNNING, "Timers",
            NotificationManager.IMPORTANCE_LOW
        )
        timer.setSound(null, null)
        nm.createNotificationChannel(timer)

        val sw = NotificationChannel(
            STOPWATCH_RUNNING, "Stopwatch",
            NotificationManager.IMPORTANCE_LOW
        )
        sw.setSound(null, null)
        nm.createNotificationChannel(sw)
    }
}
