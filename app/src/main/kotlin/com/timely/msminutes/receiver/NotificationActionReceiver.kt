package com.timely.msminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timely.msminutes.service.AlarmRingService
import com.timely.msminutes.service.StopwatchService
import com.timely.msminutes.service.TimerService

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getAction()
        if (action == null) {
            return
        }
        when (action) {
            ACTION_ALARM_DISMISS -> forward(
                context,
                AlarmRingService::class.java,
                AlarmRingService.Companion.ACTION_DISMISS,
                intent
            )

            ACTION_ALARM_SNOOZE -> forward(
                context,
                AlarmRingService::class.java,
                AlarmRingService.Companion.ACTION_SNOOZE,
                intent
            )

            ACTION_TIMER_END -> forward(
                context,
                TimerService::class.java,
                TimerService.Companion.ACTION_STOP,
                intent
            )

            ACTION_STOPWATCH_END -> forward(
                context,
                StopwatchService::class.java,
                StopwatchService.Companion.ACTION_STOP,
                intent
            )

            ACTION_STOPWATCH_LAP -> forward(
                context,
                StopwatchService::class.java,
                StopwatchService.Companion.ACTION_LAP,
                intent
            )
        }
    }

    private fun forward(
        context: Context,
        serviceClass: Class<*>?,
        action: String?,
        source: Intent
    ) {
        val service = Intent(context, serviceClass)
        service.setAction(action)
        service.putExtras(source)
        context.startForegroundService(service)
    }

    companion object {
        const val ACTION_ALARM_DISMISS: String = "action_alarm_dismiss"
        const val ACTION_ALARM_SNOOZE: String = "action_alarm_snooze"
        const val ACTION_TIMER_END: String = "action_timer_end"
        const val ACTION_STOPWATCH_END: String = "action_stopwatch_end"
        const val ACTION_STOPWATCH_LAP: String = "action_stopwatch_lap"
    }
}
