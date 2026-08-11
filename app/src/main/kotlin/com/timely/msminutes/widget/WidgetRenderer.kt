package com.timely.msminutes.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.timely.msminutes.data.Prefs

object WidgetRenderer {

    internal val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    internal val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 20
    }

    internal val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        alpha = 10
        style = Paint.Style.STROKE
    }

    internal val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        alpha = 25
        style = Paint.Style.STROKE
    }

    internal val notePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }

    internal val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif-black", Typeface.BOLD)
    }

    internal val statusPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    internal val amPmPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    internal val datePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.RIGHT
    }

    fun render(
        width: Int,
        height: Int,
        renderDensity: Float,
        prefs: Prefs,
        state: WidgetState
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val density = renderDensity

        val accent = prefs.accentColor
        val bgColor = prefs.backgroundColor
        val fontColor = prefs.fontColor
        val is24h = prefs.is24Hour()
        val isTransparent = prefs.isWidgetTransparent

        val bgMargin = if (isTransparent) 0f else 4f * density
        val contentPadding = bgMargin + 8f * density
        val bgRect = RectF(bgMargin, bgMargin, width - bgMargin, height - bgMargin)

        // 1. Background
        if (!isTransparent) {
            val radius = 16f * density

            // Drop shadow - smoother
            shadowPaint.setShadowLayer(6f * density, 0f, 2f * density, 0x10000000)
            canvas.drawRoundRect(bgRect, radius, radius, shadowPaint)

            // Main Background
            bgPaint.shader = LinearGradient(
                0f, bgRect.top, 0f, bgRect.bottom,
                adjustColor(bgColor, 1.01f),
                adjustColor(bgColor, 0.99f),
                Shader.TileMode.CLAMP
            )
            canvas.drawRoundRect(bgRect, radius, radius, bgPaint)

            // Inner Highlight
            highlightPaint.strokeWidth = 1f * density
            val innerMargin = 0.5f * density
            val innerRect = RectF(bgRect.left + innerMargin, bgRect.top + innerMargin, bgRect.right - innerMargin, bgRect.bottom - innerMargin)
            canvas.drawRoundRect(innerRect, (radius - innerMargin).coerceAtLeast(0f), (radius - innerMargin).coerceAtLeast(0f), highlightPaint)

            // Outer Accent Border
            borderPaint.color = accent
            borderPaint.strokeWidth = 1f * density
            canvas.drawRoundRect(bgRect, radius, radius, borderPaint)
        }

        // 2. Note
        val note = prefs.widgetNote
        var topPadding = contentPadding
        if (!note.isNullOrBlank()) {
            notePaint.color = fontColor
            notePaint.textSize = Math.min(12f * density, height * 0.1f)
            
            if (isTransparent) {
                notePaint.setShadowLayer(2f * density, 1f * density, 1f * density, Color.parseColor("#80000000"))
            } else {
                notePaint.clearShadowLayer()
            }
            canvas.drawText(note, width / 2f, topPadding + notePaint.textSize, notePaint)
            topPadding += notePaint.textSize * 1.4f
        }

        // 3. Time
        val bottomLimit = bgRect.bottom - (36f * density)
        drawTime(canvas, height, topPadding, bottomLimit, bgRect, is24h, fontColor, accent, isTransparent, density)

        // 4. Bottom Row (Status + Date)
        drawBottomRow(
            canvas, width, height, bgRect, accent, fontColor, is24h, isTransparent, density,
            state.alarmInfo, state.alarmLabel, state.timerMillis, state.timerLabel, state.stopwatchMillis
        )

        return bitmap
    }
}
