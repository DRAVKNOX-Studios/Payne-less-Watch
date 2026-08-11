package com.timely.msminutes.util

import com.timely.msminutes.data.Alarm

object AlarmTimeUtil {
    @JvmStatic
    fun getRemainingTimeText(alarm: Alarm?): String? {
        if (alarm == null || !alarm.isEnabled) return null

        val nextTrigger = AlarmScheduler.nextTriggerMillis(alarm)
        val diff = nextTrigger - System.currentTimeMillis()
        if (diff < 0) return null

        val days = diff / (24 * 60 * 60 * 1000)
        val hours = (diff / (60 * 60 * 1000)) % 24
        val minutes = (diff / (60 * 1000)) % 60
        val seconds = (diff / 1000) % 60

        val sb = StringBuilder("Alarm in ")
        if (days > 0) sb.append(days).append(if (days == 1L) " day " else " days ")
        if (hours > 0) sb.append(hours).append(if (hours == 1L) " hour " else " hours ")
        if (minutes > 0) sb.append(minutes).append(if (minutes == 1L) " minute " else " minutes ")
        if (seconds > 0 || (days == 0L && hours == 0L && minutes == 0L)) sb.append(seconds)
            .append(if (seconds == 1L) " second" else " seconds")
        return sb.toString().trim { it <= ' ' }
    }
}
