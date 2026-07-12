package com.timely.msminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timely.msminutes.service.AlarmRingService
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.WakeLockHolder

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId  = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        val isSnooze = intent.getBooleanExtra(AlarmScheduler.EXTRA_IS_SNOOZE, false)
        if (alarmId == -1L) return

        WakeLockHolder.acquire(context)

        context.startForegroundService(
            Intent(context, AlarmRingService::class.java)
                .putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
                .putExtra(AlarmScheduler.EXTRA_IS_SNOOZE, isSnooze)
        )
    }
}
