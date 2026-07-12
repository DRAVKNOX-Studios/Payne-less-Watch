package com.timely.msminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timely.msminutes.service.TimerService
import com.timely.msminutes.util.TimerScheduler

class TimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1)
        if (timerId == -1L) {
            return
        }
        val service = Intent(context, TimerService::class.java)
        service.setAction(TimerService.Companion.ACTION_FINISHED)
        service.putExtra(TimerScheduler.EXTRA_TIMER_ID, timerId)
        context.startForegroundService(service)
    }
}
