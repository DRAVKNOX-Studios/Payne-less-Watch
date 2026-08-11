package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class SliderItemRenderer(
    context: Context,
    private val label: String,
    var value: Int,
    private val max: Int = 255,
    private val onValueChange: (Int) -> Unit
) : BaseItemRenderer(context) {
    override var height: Float = 64f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    private val trackRect = RectF()

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        textPaint.color = tokens.textPrimary
        canvas.drawText("$label: $value", paddingStart, 24f * density, textPaint)

        val margin = paddingStart
        val trackH = 4f * density
        val trackY = 48f * density
        trackRect.set(margin, trackY - trackH / 2f, width - paddingEnd, trackY + trackH / 2f)
        
        bgPaint.color = (tokens.textSecondary and 0x00FFFFFF) or (0x44 shl 24)
        canvas.drawRoundRect(trackRect, trackH / 2f, trackH / 2f, bgPaint)
        
        val progressX = margin + (value.toFloat() / max) * trackRect.width()
        bgPaint.color = tokens.accent
        canvas.drawRoundRect(margin, trackY - trackH / 2f, progressX, trackY + trackH / 2f, trackH / 2f, trackH / 2f, bgPaint)
        
        // Draw thumb
        bgPaint.color = tokens.accent
        canvas.drawCircle(progressX, trackY, 10f * density, bgPaint)
        
        // Use accent for outline of thumb if needed or just solid
        bgPaint.color = tokens.background
        canvas.drawCircle(progressX, trackY, 4f * density, bgPaint)
    }

    override fun onClick(x: Float, y: Float) {}

    override fun onTouchEvent(event: MotionEvent, x: Float, y: Float): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Only handle if touch is within vertical bounds
                return y >= 0 && y <= height
            }
            MotionEvent.ACTION_MOVE -> {
                if (trackRect.width() > 0) {
                    // Vertical tolerance to allow some drift during drag
                    val tolerance = 80f * density
                    if (y < -tolerance || y > height + tolerance) return false
                    
                    val progress = ((x - trackRect.left) / trackRect.width()).coerceIn(0f, 1f)
                    value = (progress * max).toInt()
                    onValueChange(value)
                    return true
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                return true
            }
        }
        return false
    }
}
