package com.timely.msminutes.appfunctions

import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionService
import androidx.appfunctions.AppFunctionServiceEntryPoint
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.TimerScheduler

@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "TimelyFunctions",
    appFunctionXmlFileName = "app_functions"
)
abstract class TimelyAppFunctionService : AppFunctionService() {

    @AppFunction
    fun createAlarm(
        context: AppFunctionContext,
        hour: Int,
        minute: Int,
        label: String? = null
    ) {
        val repo = AlarmRepository(context.context)
        val alarm = Alarm().apply {
            this.hour = hour
            this.minute = minute
            this.label = label ?: "Alarm"
            this.isEnabled = true
        }
        repo.insert(alarm)
        AlarmScheduler.schedule(context.context, alarm)
    }

    @AppFunction
    fun startTimer(
        context: AppFunctionContext,
        durationSeconds: Int,
        label: String? = null
    ) {
        val repo = TimerRepository(context.context)
        val timer = TimerItem().apply {
            this.totalMillis = durationSeconds * 1000L
            this.remainingMillis = totalMillis
            this.label = label ?: "Timer"
            this.state = TimerItem.STATE_RUNNING
            this.endTimestamp = System.currentTimeMillis() + totalMillis
        }
        val id = repo.insert(timer)
        TimerScheduler.schedule(context.context, id, timer.endTimestamp)
    }
}
