package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.data.LapStore
import com.timely.msminutes.data.StopwatchHistoryItem
import com.timely.msminutes.ui.canvas.CanvasRenderer
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryItemRenderer(
    context: Context,
    private val item: StopwatchHistoryItem,
    private val onUpdate: () -> Unit,
    private val onDelete: () -> Unit
) : BaseItemRenderer(context) {

    private var isExpanded = false
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    private val laps = LapStore.decode(item.laps)

    override var height: Float = 72f * density
        get() = if (isExpanded) {
            val lapCount = laps.size
            72f * density + (lapCount * 24f * density) + (if (lapCount > 0) 16f * density else 0f)
        } else {
            72f * density
        }

    private val cardRect = RectF()

    override val isSwipeable: Boolean = true

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density, tokens)
        val r = 16f * density
        val hMargin = 12f * density
        cardRect.set(hMargin, 4f * density, width - hMargin, height - 4f * density)
        
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)

        // Label - Ensure it's not empty and visible
        val displayLabel = if (item.label.isNullOrBlank()) "Stopwatch" else item.label
        textPaint.color = tokens.textPrimary
        textPaint.textSize = 16f * density
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(displayLabel, cardRect.left + 16f * density, cardRect.top + 28f * density, textPaint)

        // Date
        subTextPaint.color = tokens.textSecondary
        subTextPaint.textSize = 12f * density
        subTextPaint.textAlign = Paint.Align.LEFT
        val dateStr = dateFormat.format(Date(item.timestamp))
        canvas.drawText(dateStr, cardRect.left + 16f * density, cardRect.top + 52f * density, subTextPaint)

        // Total Time
        textPaint.color = tokens.accent
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.textSize = 18f * density
        val timeStr = TimeFormatUtil.formatStopwatch(item.elapsedTime)
        canvas.drawText(timeStr, cardRect.right - 16f * density, cardRect.top + 38f * density, textPaint)

        if (isExpanded && laps.isNotEmpty()) {
            val startY = cardRect.top + 72f * density
            subTextPaint.textSize = 14f * density
            laps.forEachIndexed { index, lapTime ->
                val y = startY + index * 24f * density
                subTextPaint.color = tokens.textSecondary
                subTextPaint.textAlign = Paint.Align.LEFT
                canvas.drawText("Lap ${laps.size - index}", cardRect.left + 24f * density, y, subTextPaint)
                
                subTextPaint.color = tokens.textPrimary
                subTextPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(lapTime, cardRect.right - 24f * density, y, subTextPaint)
            }
        }
    }

    override fun onClick(x: Float, y: Float) {
        isExpanded = !isExpanded
        onUpdate()
    }

    override fun onDelete() {
        onDelete.invoke()
    }

    override fun populateAccessibility(
        items: MutableList<CanvasRenderer.AccessibilityItem>,
        listBounds: RectF,
        absoluteTop: Float,
        label: String
    ) {
        val displayLabel = if (item.label.isNullOrBlank()) "Stopwatch" else item.label
        val dateStr = dateFormat.format(Date(item.timestamp))
        val timeStr = TimeFormatUtil.formatStopwatch(item.elapsedTime)
        val fullDesc = "$displayLabel, $timeStr, $dateStr" + if (laps.isNotEmpty()) ", ${laps.size} laps" else ""
        
        super.populateAccessibility(items, listBounds, absoluteTop, fullDesc)
    }
}
