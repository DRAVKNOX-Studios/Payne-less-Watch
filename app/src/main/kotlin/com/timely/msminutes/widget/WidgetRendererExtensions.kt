package com.timely.msminutes.widget

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.text.format.DateFormat
import com.timely.msminutes.ui.canvas.CanvasIcons
import com.timely.msminutes.ui.canvas.drawAlarm
import com.timely.msminutes.ui.canvas.drawStopwatch
import com.timely.msminutes.ui.canvas.drawTimer
import java.util.Calendar
import java.util.Locale

internal fun WidgetRenderer.drawTime(
    canvas: Canvas,
    height: Int,
    topPadding: Float,
    bottomLimit: Float,
    bgRect: RectF,
    is24h: Boolean,
    fontColor: Int,
    accent: Int,
    isTransparent: Boolean,
    density: Float
) {
    val cal = Calendar.getInstance()
    val hour = cal[Calendar.HOUR_OF_DAY]
    val minute = cal[Calendar.MINUTE]
    val isEasterEggTime = is24h && hour == 0 && minute == 0

    val timeStr = if (is24h) {
        String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
    } else {
        val hour12 = hour % 12
        val displayHour = if (hour12 == 0) 12 else hour12
        String.format(Locale.getDefault(), "%2d:%02d", displayHour, minute)
    }

    // Area where the time should fit
    val internalGutter = 12f * density
    val availableWidth = bgRect.width() - (internalGutter * 2)
    val availableHeight = (bottomLimit - topPadding) * 0.85f

    // Adaptive text size - allow it to be much larger for scaled widgets
    var finalSize = Math.min(140f * density, availableHeight)
    
    timePaint.textSize = finalSize
    
    fun calculateTotalWidth(size: Float): Float {
        timePaint.textSize = size
        var w = 0f
        for (char in timeStr) {
            val isEye = isEasterEggTime && char == '0'
            timePaint.textScaleX = if (isEye) 1.05f else 1.0f
            w += timePaint.measureText(char.toString())
        }
        // More spacing for soft glows
        val charSpacing = size * 0.12f
        var total = w + (charSpacing * (timeStr.length - 1))
        
        if (!is24h) {
            amPmPaint.textSize = Math.min(14f * density, size * 0.25f)
            total += amPmPaint.measureText("PM") + 8f * density
        }
        return total
    }
    
    var totalW = calculateTotalWidth(finalSize)
    if (totalW > availableWidth) {
        finalSize *= (availableWidth / totalW)
        totalW = calculateTotalWidth(finalSize)
    }
    
    // Recalculate time-only width for centering the main clock part
    timePaint.textSize = finalSize
    var timeOnlyW = 0f
    for (char in timeStr) {
        val isEye = isEasterEggTime && char == '0'
        timePaint.textScaleX = if (isEye) 1.05f else 1.0f
        timeOnlyW += timePaint.measureText(char.toString())
    }
    val spacing = finalSize * 0.12f
    timeOnlyW += (spacing * (timeStr.length - 1))

    // Center vertically between topPadding and bottomLimit
    val timeY = topPadding + (bottomLimit - topPadding) / 2f + (finalSize * 0.38f)
    var currentX = bgRect.left + (bgRect.width() - totalW) / 2f

    timePaint.textSize = finalSize
    
    for (i in timeStr.indices) {
        val char = timeStr[i]
        val isEye = isEasterEggTime && char == '0'
        timePaint.textScaleX = if (isEye) 1.05f else 1.0f
        val charWidth = timePaint.measureText(char.toString())
        
        if (isEye) {
            // Draw eye socket instead of '0'
            val eyeWidth = finalSize * 0.65f 
            val eyeRect = RectF(
                currentX + (charWidth - eyeWidth) / 2f,
                timeY - finalSize * 0.8f,
                currentX + (charWidth + eyeWidth) / 2f,
                timeY + finalSize * 0.2f
            )
            timePaint.style = Paint.Style.STROKE
            timePaint.strokeWidth = finalSize * 0.08f
            timePaint.color = accent
            timePaint.setShadowLayer(finalSize * 0.28f, 0f, 0f, accent)
            canvas.drawOval(eyeRect, timePaint)
            timePaint.clearShadowLayer()

            // Draw pupil
            val eyeIdx = if (i < 2) i else i - 1
            val offset = GooglyEyesController.getPupilOffset(eyeIdx)
            val scaledOffsetX = (offset[0] / 6f) * (finalSize * 0.12f)
            val scaledOffsetY = (offset[1] / 6f) * (finalSize * 0.12f)
            val pupilRadius = (finalSize * 0.07f).coerceAtLeast(1f)
            timePaint.style = Paint.Style.FILL
            canvas.drawCircle(currentX + charWidth / 2f + scaledOffsetX, timeY - finalSize * 0.45f + scaledOffsetY, pupilRadius, timePaint)
        } else {
            timePaint.style = Paint.Style.STROKE
            timePaint.strokeWidth = finalSize * 0.06f
            timePaint.color = accent
            timePaint.setShadowLayer(finalSize * 0.25f, 0f, 0f, accent)
            canvas.drawText(char.toString(), currentX, timeY, timePaint)

            timePaint.clearShadowLayer()
            timePaint.strokeWidth = finalSize * 0.08f
            canvas.drawText(char.toString(), currentX, timeY, timePaint)

            timePaint.style = Paint.Style.FILL
            timePaint.color = fontColor
            canvas.drawText(char.toString(), currentX, timeY, timePaint)
        }
        currentX += charWidth + spacing
    }

    // AM/PM
    if (!is24h) {
        val amPm = if (hour < 12) "AM" else "PM"
        amPmPaint.apply {
            color = if (isTransparent) accent else fontColor
            textSize = Math.min(14f * density, finalSize * 0.25f)
            if (isTransparent) {
                setShadowLayer(2f * density, 1f * density, 1f * density, Color.parseColor("#80000000"))
            } else {
                clearShadowLayer()
            }
        }
        canvas.drawText(amPm, currentX + 2f * density, timeY - (finalSize * 0.1f), amPmPaint)
    }
}

internal fun WidgetRenderer.drawBottomRow(
    canvas: Canvas,
    width: Int,
    height: Int,
    bgRect: RectF,
    accent: Int,
    fontColor: Int,
    is24h: Boolean,
    isTransparent: Boolean,
    density: Float,
    alarmInfo: String?,
    alarmLabel: String?,
    timerMillis: Long?,
    timerLabel: String?,
    stopwatchMillis: Long?
) {
    val bottomInset = 16f * density
    val bottomY = bgRect.bottom - bottomInset
    var startX = bgRect.left + 20f * density

    statusPaint.color = accent
    statusPaint.textSize = Math.min(10f * density, height * 0.1f)
    
    if (isTransparent) {
        statusPaint.setShadowLayer(1.5f * density, 0.5f * density, 0.5f * density, Color.parseColor("#80000000"))
    } else {
        statusPaint.clearShadowLayer()
    }

    val iconSize = statusPaint.textSize
    
    // Alarm
    if (!alarmInfo.isNullOrBlank()) {
        CanvasIcons.drawAlarm(canvas, startX, bottomY - iconSize, iconSize, accent)
        startX += iconSize * 1.2f
        canvas.drawText(alarmInfo, startX, bottomY, statusPaint)
        startX += statusPaint.measureText(alarmInfo) + 4f * density
        if (!alarmLabel.isNullOrBlank()) {
            val labelColor = (accent and 0x00FFFFFF) or (0xCC shl 24) // Slightly faded
            statusPaint.color = labelColor
            canvas.drawText(alarmLabel, startX, bottomY, statusPaint)
            statusPaint.color = accent
            startX += statusPaint.measureText(alarmLabel) + 12f * density
        } else {
            startX += 8f * density
        }
    }

    // Timer
    if (timerMillis != null) {
        CanvasIcons.drawTimer(canvas, startX, bottomY - iconSize, iconSize, accent)
        startX += iconSize * 1.2f
        val timerStr = formatDuration(timerMillis)
        canvas.drawText(timerStr, startX, bottomY, statusPaint)
        startX += statusPaint.measureText(timerStr) + 4f * density
        if (!timerLabel.isNullOrBlank()) {
            canvas.drawText(timerLabel, startX, bottomY, statusPaint)
            startX += statusPaint.measureText(timerLabel) + 12f * density
        } else {
            startX += 8f * density
        }
    }

    // Stopwatch
    if (stopwatchMillis != null) {
        CanvasIcons.drawStopwatch(canvas, startX, bottomY - iconSize, iconSize, accent)
        startX += iconSize * 1.2f
        val swStr = formatDuration(stopwatchMillis)
        canvas.drawText(swStr, startX, bottomY, statusPaint)
    }

    // Date
    datePaint.apply {
        color = (fontColor and 0x00FFFFFF) or (0x99 shl 24)
        textSize = Math.min(12.5f * density, height * 0.15f)
        if (isTransparent) {
            setShadowLayer(1.5f * density, 0.5f * density, 0.5f * density, Color.parseColor("#80000000"))
            color = accent
        } else {
            clearShadowLayer()
        }
    }
    val datePattern = if (is24h) "EEE, d MMM" else "EEE, MMM d"
    val dateStr = DateFormat.format(datePattern, Calendar.getInstance()).toString()
    canvas.drawText(dateStr, bgRect.right - 20f * density, bottomY, datePaint)
}

internal fun WidgetRenderer.formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    val hours = (millis / (1000 * 60 * 60))
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }
}

internal fun WidgetRenderer.adjustColor(color: Int, factor: Float): Int {
    val a = Color.alpha(color)
    val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
    val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
    val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
    return Color.argb(a, r, g, b)
}
