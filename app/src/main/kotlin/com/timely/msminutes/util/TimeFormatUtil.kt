package com.timely.msminutes.util

import java.util.Locale
import kotlin.math.max

object TimeFormatUtil {
    fun formatClock(hour: Int, minute: Int, is24h: Boolean): String {
        if (is24h) {
            return String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }
        var h = hour % 12
        if (h == 0) {
            h = 12
        }
        val suffix = if (hour >= 12) "PM" else "AM"
        return String.format(Locale.getDefault(), "%d:%02d %s", h, minute, suffix)
    }

    fun formatStopwatch(millis: Long): String {
        val totalSeconds = millis / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        val centis = (millis % 1000) / 10
        
        val sb = StringBuilder(12)
        if (hours > 0) {
            if (hours < 10) sb.append('0')
            sb.append(hours).append(':')
        }
        if (minutes < 10) sb.append('0')
        sb.append(minutes).append(':')
        if (seconds < 10) sb.append('0')
        sb.append(seconds).append('.')
        if (centis < 10) sb.append('0')
        sb.append(centis)
        return sb.toString()
    }

    fun formatTimer(millis: Long): String {
        val totalSeconds = max(0, millis) / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        val sb = StringBuilder(8)
        if (hours > 0) {
            if (hours < 10) sb.append('0')
            sb.append(hours).append(':')
        }
        if (minutes < 10) sb.append('0')
        sb.append(minutes).append(':')
        if (seconds < 10) sb.append('0')
        sb.append(seconds)
        return sb.toString()
    }
}
