package com.timely.msminutes.widget

import android.content.Context
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.util.AlarmScheduler
import java.util.Locale

object WidgetInfoProvider {
    fun getWidgetState(context: Context): WidgetState {
        val repository = AlarmRepository(context)
        val prefs = Prefs(context)
        var alarmInfo: String? = null
        var alarmLabel: String? = null

        val alarms = repository.all
        val nextAlarm = findNextAlarm(alarms)
        if (nextAlarm != null) {
            alarmInfo = if (prefs.is24Hour()) {
                String.format(Locale.getDefault(), "%02d:%02d", nextAlarm.hour, nextAlarm.minute)
            } else {
                val hour12 = nextAlarm.hour % 12
                val displayHour = if (hour12 == 0) 12 else hour12
                val amPm = if (nextAlarm.hour < 12) "AM" else "PM"
                String.format(Locale.getDefault(), "%d:%02d %s", displayHour, nextAlarm.minute, amPm)
            }
            alarmLabel = nextAlarm.label
        }

        val timerRepo = TimerRepository(context)
        val timers = timerRepo.all
        var activeTimer: TimerItem? = null
        for (t in timers) {
            if (t != null && t.state == TimerItem.STATE_RUNNING) {
                activeTimer = t
                break
            }
        }
        val timerMillis = activeTimer?.let { it.endTimestamp - System.currentTimeMillis() }
        val timerLabel = activeTimer?.label

        val stopwatchMillis = if (prefs.isStopwatchRunning) {
            (prefs.lastStopwatchElapsed
                    + (System.currentTimeMillis() - prefs.stopwatchStartBase))
        } else null

        val isEasterEgg = GooglyEyesController.isEasterEggMinute(context)
        val pupils = mutableListOf<Float>()
        if (isEasterEgg) {
            for (i in 0..3) {
                val offset = GooglyEyesController.getPupilOffset(i)
                pupils.add(offset[0])
                pupils.add(offset[1])
            }
        }

        return WidgetState(
            minute = System.currentTimeMillis() / 60_000,
            backgroundColor = prefs.backgroundColor,
            accentColor = prefs.accentColor,
            fontColor = prefs.fontColor,
            isWidgetTransparent = prefs.isWidgetTransparent,
            widgetNote = prefs.widgetNote,
            is24Hour = prefs.is24Hour(),
            pupilOffsets = pupils,
            alarmInfo = alarmInfo,
            alarmLabel = alarmLabel,
            timerMillis = timerMillis,
            timerLabel = timerLabel,
            stopwatchMillis = stopwatchMillis
        )
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
