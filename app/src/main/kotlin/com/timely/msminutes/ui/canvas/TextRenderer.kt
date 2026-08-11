package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.timely.msminutes.util.ThemeTokens

class TextRenderer(
    context: android.content.Context,
    private val text: String,
    private val sizeSp: Float = 16f,
    private val isBold: Boolean = false
) : CanvasRenderer {

    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = sizeSp * density
        textAlign = Paint.Align.CENTER
        if (isBold) isFakeBoldText = true
    }

    var isVisible = true

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (!isVisible) return
        paint.color = tokens.textSecondary
        val textY = bounds.centerY() - (paint.descent() + paint.ascent()) / 2f
        canvas.drawText(text, bounds.centerX(), textY, paint)
    }
}
