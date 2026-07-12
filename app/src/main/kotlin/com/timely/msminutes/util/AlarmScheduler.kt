package com.timely.msminutes.util

import android.app.AlarmManager
import android.app.AlarmManager.AlarmClockInfo
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.receiver.AlarmReceiver
import java.util.Calendar

object AlarmScheduler {
    const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    const val EXTRA_IS_SNOOZE: String = "extra_is_snooze"

    fun schedule(context: Context, alarm: Alarm) {
        val triggerAt = nextTriggerMillis(alarm)
        scheduleExact(context, alarm.id, triggerAt, false)
    }

    fun scheduleSnooze(context: Context, alarmId: Long, minutes: Int) {
        val triggerAt = System.currentTimeMillis() + (minutes * 60000L)
        scheduleExact(context, alarmId, triggerAt, true)
    }

    fun cancel(context: Context, alarmId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = buildPendingIntent(context, alarmId, false)
        am?.cancel(pi)
        pi.cancel()
        val piSnooze = buildPendingIntent(context, alarmId, true)
        am?.cancel(piSnooze)
        piSnooze.cancel()
    }

    private fun scheduleExact(context: Context, alarmId: Long, triggerAt: Long, isSnooze: Boolean) {
        val am = context.getSystemService(AlarmManager::class.java)
        if (am == null) return
        val pi = buildPendingIntent(context, alarmId, isSnooze)
        val info = AlarmClockInfo(triggerAt, pi)
        am.setAlarmClock(info, pi)
    }

    private fun buildPendingIntent(
        context: Context?,
        alarmId: Long,
        isSnooze: Boolean
    ): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra(EXTRA_ALARM_ID, alarmId)
        intent.putExtra(EXTRA_IS_SNOOZE, isSnooze)
        val requestCode = (alarmId * 2 + (if (isSnooze) 1 else 0)).toInt()
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getBroadcast(context!!, requestCode, intent, flags)
    }

    @JvmStatic
    fun nextTriggerMillis(alarm: Alarm): Long {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance()
        candidate.set(Calendar.HOUR_OF_DAY, alarm.hour)
        candidate.set(Calendar.MINUTE, alarm.minute)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)

        if (!alarm.isRepeating) {
            if (candidate.timeInMillis <= now.timeInMillis) {
                candidate.add(Calendar.DAY_OF_YEAR, 1)
            }
            return candidate.timeInMillis
        }

        for (i in 0..7) {
            val dayIndex = (candidate.get(Calendar.DAY_OF_WEEK) + 5) % 7
            val valid = alarm.isDayEnabled(dayIndex)
            val isFuture = candidate.timeInMillis > now.timeInMillis
            if (valid && isFuture) return candidate.timeInMillis
            candidate.add(Calendar.DAY_OF_YEAR, 1)
        }
        return candidate.timeInMillis
    }
}
