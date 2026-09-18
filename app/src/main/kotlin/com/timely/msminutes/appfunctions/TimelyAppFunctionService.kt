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
import com.timely.msminutes.widget.WidgetNotifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Provides App Functions for managing alarms and timers.
 */
@RequiresApi(36)
@AppFunctionServiceEntryPoint(
    serviceName = "TimelyFunctions",
    appFunctionXmlFileName = "timely_app_functions"
)
abstract class TimelyAppFunctionService : AppFunctionService() {

    /**
     * Creates and schedules a new alarm.
     *
     * @param context The execution context.
     * @param hour The hour of the day (0-23).
     * @param minute The minute of the hour (0-59).
     * @param label An optional label for the alarm.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun createAlarm(
        context: AppFunctionContext,
        hour: Int,
        minute: Int,
        label: String? = null
    ) = withContext(Dispatchers.IO) {
        val repo = AlarmRepository(context.context)
        val alarm = Alarm().apply {
            this.hour = hour
            this.minute = minute
            this.label = label ?: "Alarm"
            this.isEnabled = true
        }
        repo.insert(alarm)
        AlarmScheduler.schedule(context.context, alarm)
        WidgetNotifier.notifyUpdate(context.context)
    }

    /**
     * Starts a new countdown timer.
     *
     * @param context The execution context.
     * @param durationSeconds The duration of the timer in seconds.
     * @param label An optional label for the timer.
     */
    @AppFunction(isDescribedByKDoc = true)
    suspend fun startTimer(
        context: AppFunctionContext,
        durationSeconds: Int,
        label: String? = null
    ) = withContext(Dispatchers.IO) {
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
        WidgetNotifier.notifyUpdate(context.context)
    }
}
