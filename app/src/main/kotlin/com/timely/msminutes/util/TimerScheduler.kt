package com.timely.msminutes.util

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.timely.msminutes.receiver.TimerReceiver

object TimerScheduler {
    const val EXTRA_TIMER_ID: String = "extra_timer_id"

    fun schedule(context: Context, timerId: Long, triggerAt: Long) {
        val am = context.getSystemService<AlarmManager?>(AlarmManager::class.java)
        if (am == null) {
            return
        }
        val pi = buildPendingIntent(context, timerId)
        val info = AlarmClockInfo(triggerAt, pi)
        am.setAlarmClock(info, pi)
    }

    fun cancel(context: Context, timerId: Long) {
        val am = context.getSystemService<AlarmManager?>(AlarmManager::class.java)
        val pi = buildPendingIntent(context, timerId)
        if (am != null) {
            am.cancel(pi)
        }
        pi.cancel()
    }

    private fun buildPendingIntent(context: Context?, timerId: Long): PendingIntent {
        val intent = Intent(context, TimerReceiver::class.java)
        intent.putExtra(EXTRA_TIMER_ID, timerId)
        val requestCode = (5000 + timerId).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context, requestCode, intent, flags)
    }
}
