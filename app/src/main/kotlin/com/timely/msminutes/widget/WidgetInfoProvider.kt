package com.timely.msminutes.widget

import android.content.Context
import android.os.SystemClock
import android.view.View
import android.widget.RemoteViews
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.util.AlarmScheduler
import java.util.Locale

object WidgetInfoProvider {
    fun updateAlarmInfo(context: Context, views: RemoteViews) {
        val repository = AlarmRepository(context)
        val prefs = Prefs(context)

        // 1. Alarm
        val alarms = repository.all
        val nextAlarm = findNextAlarm(alarms)
        if (nextAlarm != null) {
            views.setViewVisibility(R.id.alarm_container, View.VISIBLE)
            val alarmTimeText = if (prefs.is24Hour()) {
                String.format(Locale.getDefault(), "%02d:%02d", nextAlarm.hour, nextAlarm.minute)
            } else {
                val hour12 = nextAlarm.hour % 12
                val displayHour = if (hour12 == 0) 12 else hour12
                val amPm = if (nextAlarm.hour < 12) "AM" else "PM"
                String.format(Locale.getDefault(), "%d:%02d %s", displayHour, nextAlarm.minute, amPm)
            }
            views.setTextViewText(R.id.widget_next_alarm, alarmTimeText)
            val alarmLabel = nextAlarm.label
            if (!alarmLabel.isNullOrEmpty()) {
                views.setTextViewText(R.id.widget_alarm_label, alarmLabel)
                views.setViewVisibility(R.id.widget_alarm_label, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_alarm_label, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.alarm_container, View.GONE)
        }

        // 2. Timer
        val timerRepo = TimerRepository(context)
        val timers = timerRepo.all
        var activeTimer: TimerItem? = null
        for (t in timers) {
            if (t != null && t.state == TimerItem.STATE_RUNNING) {
                activeTimer = t
                break
            }
        }
        if (activeTimer != null) {
            views.setViewVisibility(R.id.timer_container, View.VISIBLE)
            val remaining = activeTimer.endTimestamp - System.currentTimeMillis()
            views.setChronometer(
                R.id.widget_timer_chrono,
                SystemClock.elapsedRealtime() + remaining, null, true
            )
            val timerLabel = activeTimer.label
            if (!timerLabel.isNullOrEmpty()) {
                views.setTextViewText(R.id.widget_timer_label, timerLabel)
                views.setViewVisibility(R.id.widget_timer_label, View.VISIBLE)
            } else {
                views.setViewVisibility(R.id.widget_timer_label, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.timer_container, View.GONE)
        }

        // 3. Stopwatch
        if (prefs.isStopwatchRunning) {
            views.setViewVisibility(R.id.stopwatch_container, View.VISIBLE)
            val elapsed = (prefs.lastStopwatchElapsed
                    + (System.currentTimeMillis() - prefs.stopwatchStartBase))
            views.setChronometer(
                R.id.widget_stopwatch_chrono,
                SystemClock.elapsedRealtime() - elapsed, null, true
            )
        } else {
            views.setViewVisibility(R.id.stopwatch_container, View.GONE)
        }
    }

    /** Updated to accept non-nullable list now that AlarmRepository.all returns MutableList<Alarm>. */
    private fun findNextAlarm(alarms: MutableList<Alarm>): Alarm? {
        var next: Alarm? = null
        var minTrigger = Long.MAX_VALUE
        for (alarm in alarms) {
            if (!alarm.isEnabled) continue
            val trigger = AlarmScheduler.nextTriggerMillis(alarm)
            if (trigger < minTrigger) {
                minTrigger = trigger
                next = alarm
            }
        }
        return next
    }
}
