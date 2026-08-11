package com.timely.msminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.TimerScheduler
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action.isEmpty()) return

        val alarmRepo = AlarmRepository(context)
        alarmRepo.all.forEach { alarm ->
            if (alarm.isEnabled) {
                AlarmScheduler.schedule(context, alarm)
            }
        }

        val timerRepo = TimerRepository(context)
        timerRepo.all.forEach { timer ->
            if (timer.state == TimerItem.STATE_RUNNING) {
                TimerScheduler.schedule(context, timer.id, timer.endTimestamp)
            }
        }
        notifyUpdate(context)
    }
}
