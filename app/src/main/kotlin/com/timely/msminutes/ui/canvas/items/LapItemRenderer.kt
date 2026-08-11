package com.timely.msminutes.ui.canvas.items

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens

class LapItemRenderer(
    private val androidContext: android.content.Context,
    private val lapIndex: Int,
    private val lapTime: String
) : ItemRenderer {

    private val density = androidContext.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 60f * density
    override var swipeX: Float = 0f

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 15f * density
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cardRect = RectF()

    override val isSwipeable: Boolean = false

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        val r = 24f * density
        val hMargin = 14f * density
        cardRect.set(hMargin, 4f * density, width - hMargin, height - 4f * density)
        
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)

        textPaint.color = tokens.textSecondary
        canvas.drawText("Lap $lapIndex", hMargin + 16f * density, 38f * density, textPaint)

        textPaint.color = tokens.textPrimary
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(lapTime, width - hMargin - 16f * density, 38f * density, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    override fun onClick(x: Float, y: Float) {}
}
