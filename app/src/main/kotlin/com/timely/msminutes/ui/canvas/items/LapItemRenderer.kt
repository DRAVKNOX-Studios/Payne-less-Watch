package com.timely.msminutes.ui.canvas.items

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.timely.msminutes.util.ThemeTokens

class LapItemRenderer(
    context: android.content.Context,
    private val lapIndex: Int,
    private val lapTime: String
) : BaseItemRenderer(context) {

    override var height: Float = 60f * density
    private val cardRect = RectF()

    override val isSwipeable: Boolean = false

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        val r = 24f * density
        val hMargin = 14f * density
        cardRect.set(hMargin, 4f * density, width - hMargin, height - 4f * density)
        
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)

        textPaint.color = tokens.textSecondary
        textPaint.textSize = 15f * density
        canvas.drawText("Lap $lapIndex", hMargin + 16f * density, 38f * density, textPaint)

        textPaint.color = tokens.textPrimary
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(lapTime, width - hMargin - 16f * density, 38f * density, textPaint)
    }

    override fun onClick(x: Float, y: Float) {}
}
