package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import com.timely.msminutes.util.ThemeTokens

class DurationPickerItemRenderer(
    context: Context,
    private val label: String,
    var value: Int,
    private val onValueChange: (Int) -> Unit
) : BaseItemRenderer(context) {
    override var height: Float = 108f * density
    
    private val btnBoundsMinus = RectF()
    private val btnBoundsPlus = RectF()

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.color = tokens.textPrimary
        val labelY = 36f * density
        canvas.drawText(label, paddingStart, labelY, textPaint)

        val btnSize = 40f * density
        val centerY = 76f * density
        
        btnBoundsMinus.set(paddingStart, centerY - btnSize / 2f, paddingStart + btnSize, centerY + btnSize / 2f)
        btnBoundsPlus.set(width - btnSize - paddingEnd, centerY - btnSize / 2f, width - paddingEnd, centerY + btnSize / 2f)
        
        // Draw Minus
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(btnBoundsMinus, 8f * density, 8f * density, bgPaint)
        textPaint.color = tokens.textPrimary
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("-", btnBoundsMinus.centerX(), btnBoundsMinus.centerY() + 6f * density, textPaint)
        
        // Draw Plus
        canvas.drawRoundRect(btnBoundsPlus, 8f * density, 8f * density, bgPaint)
        canvas.drawText("+", btnBoundsPlus.centerX(), btnBoundsPlus.centerY() + 6f * density, textPaint)
        
        // Draw Value
        textPaint.color = tokens.textPrimary
        canvas.drawText("$value min", width / 2f, centerY + 6f * density, textPaint)
    }

    override fun onClick(x: Float, y: Float) {
        if (btnBoundsMinus.contains(x, y)) {
            if (value > 1) {
                value--
                onValueChange(value)
            }
        } else if (btnBoundsPlus.contains(x, y)) {
            value++
            onValueChange(value)
        }
    }
}
