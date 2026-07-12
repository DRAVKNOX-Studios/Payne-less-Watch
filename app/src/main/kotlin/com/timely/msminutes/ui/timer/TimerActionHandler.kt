package com.timely.msminutes.ui.timer

import android.content.Context
import android.content.Intent
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.service.TimerService
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.TimerScheduler
import com.timely.msminutes.widget.WidgetNotifier
import kotlin.math.max

/**
 * Handles business logic for timer actions like toggle, reset, and start.
 */
class TimerActionHandler(
    private val context: Context,
    private val repository: TimerRepository,
    private val onUpdate: () -> Unit
) {

    fun toggleTimer(item: TimerItem) {
        AppExecutors.get().diskIO {
            if (item.state == TimerItem.STATE_RUNNING) {
                item.remainingMillis = max(0, item.endTimestamp - System.currentTimeMillis())
                item.state = TimerItem.STATE_PAUSED
                TimerScheduler.cancel(context, item.id)
            } else {
                if (item.state == TimerItem.STATE_FINISHED) stopTimerService(item.id)
                if (item.state == TimerItem.STATE_FINISHED || item.remainingMillis <= 0)
                    item.remainingMillis = item.totalMillis
                item.endTimestamp = System.currentTimeMillis() + item.remainingMillis
                item.state = TimerItem.STATE_RUNNING
                TimerScheduler.schedule(context, item.id, item.endTimestamp)
            }
            repository.update(item)
            AppExecutors.get().mainThread {
                WidgetNotifier.notifyUpdate(context)
                onUpdate()
            }
        }
    }

    fun resetTimer(item: TimerItem) {
        AppExecutors.get().diskIO {
            if (item.state == TimerItem.STATE_FINISHED) stopTimerService(item.id)
            TimerScheduler.cancel(context, item.id)
            item.state = TimerItem.STATE_PAUSED
            item.remainingMillis = item.totalMillis
            repository.update(item)
            AppExecutors.get().mainThread {
                WidgetNotifier.notifyUpdate(context)
                onUpdate()
            }
        }
    }

    fun stopTimerService(timerId: Long) {
        context.startService(
            Intent(context, TimerService::class.java).apply {
                action = TimerService.ACTION_STOP
                putExtra(TimerScheduler.EXTRA_TIMER_ID, timerId)
            }
        )
    }

    fun startTimerAsync(timer: TimerItem) {
        timer.endTimestamp    = System.currentTimeMillis() + timer.totalMillis
        timer.remainingMillis = timer.totalMillis
        timer.state           = TimerItem.STATE_RUNNING
        repository.update(timer)
        TimerScheduler.schedule(context, timer.id, timer.endTimestamp)
    }
}
