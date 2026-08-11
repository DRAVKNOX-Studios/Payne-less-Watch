package com.timely.msminutes.service

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.alarm.AlarmRingActivity
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.NotificationChannels
import com.timely.msminutes.util.TimeFormatUtil

object AlarmNotificationHelper {

    fun buildFullscreenPendingIntent(context: Context, alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmRingActivity::class.java).apply {
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                    or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    or Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }
        return PendingIntent.getActivity(
            context, 3, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun buildPlaceholderNotification(context: Context, fullscreenPi: PendingIntent): Notification =
        NotificationCompat.Builder(context, NotificationChannels.ALARM_RING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm")
            .setContentText("Ringing\u2026")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullscreenPi, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

    fun buildNotification(context: Context, alarm: Alarm, prefs: Prefs): Notification {
        val baseFlags    = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val dismissPi    = PendingIntent.getService(
            context, 1,
            Intent(context, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_DISMISS), baseFlags)
        val snoozePi     = PendingIntent.getService(
            context, 2,
            Intent(context, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_SNOOZE), baseFlags)
        val fullscreenPi = buildFullscreenPendingIntent(context, alarm.id)
        val label        = if (!alarm.label.isNullOrEmpty()) alarm.label else "Alarm"
        val timeStr      = TimeFormatUtil.formatClock(alarm.hour, alarm.minute, prefs.is24Hour())
        
        return NotificationCompat.Builder(context, NotificationChannels.ALARM_RING)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Alarm: $label")
            .setContentText("Ringing ($timeStr)")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullscreenPi, true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Dismiss", dismissPi)
            .addAction(0, "Snooze", snoozePi)
            .build()
    }
    
    fun buildMissedNotification(context: Context, alarm: Alarm): Notification {
        val lbl = if (!alarm.label.isNullOrEmpty()) alarm.label else "Alarm"
        return NotificationCompat.Builder(context, NotificationChannels.ALARM_MISSED)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Missed alarm")
            .setContentText(lbl)
            .setAutoCancel(true)
            .build()
    }
}
