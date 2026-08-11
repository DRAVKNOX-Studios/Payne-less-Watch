package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens

class DayToggleRenderer(
    context: Context,
    private val selectedDays: BooleanArray,
    private val onToggle: (Int) -> Unit
) : ItemRenderer {
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 56f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private val dayLabels = arrayOf("M", "T", "W", "T", "F", "S", "S")

    private var lastWidth: Float = 0f

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        lastWidth = width
        val itemWidth = width / 7f
        val radius = 18f * density
        val centerY = height / 2f
        
        for (i in 0..6) {
            val centerX = i * itemWidth + itemWidth / 2f
            val isSelected = selectedDays[i]
            
            bgPaint.color = if (isSelected) tokens.accent else tokens.surface
            canvas.drawCircle(centerX, centerY, radius, bgPaint)
            
            paint.color = if (isSelected) tokens.font else tokens.textPrimary
            canvas.drawText(dayLabels[i], centerX, centerY + 5f * density, paint)
        }
    }

    override fun onClick(x: Float, y: Float) {
        if (lastWidth <= 0f) return
        val itemWidth = lastWidth / 7f
        val index = (x / itemWidth).toInt().coerceIn(0, 6)
        onToggle(index)
    }
}
