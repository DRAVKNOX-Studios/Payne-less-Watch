package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.timely.msminutes.ui.canvas.items.BaseItemRenderer
import com.timely.msminutes.util.ThemeTokens

class AlarmRingRenderer(
    context: Context,
    private val time: String,
    private val label: String,
    private val note: String?,
    private val onDismiss: () -> Unit,
    private val onSnooze: () -> Unit
) : CanvasRenderer {
    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val eyesRenderer = GooglyEyesRenderer(density)
    
    private var startY = 0f
    private val threshold = 120f * density

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        val h = 200f * density
        // Shifted down to clear the heads-up notification height (approx 280dp from top)
        eyesRenderer.onLayout(left, top + 280f * density, right, top + 280f * density + h)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        // Draw background with accent color
        canvas.drawColor(tokens.accent)

        BaseItemRenderer.resetPaints(density)
        val timePaint = BaseItemRenderer.timePaint
        val textPaint = BaseItemRenderer.textPaint
        val subTextPaint = BaseItemRenderer.subTextPaint

        val centerX = bounds.centerX()
        
        // 1. Original Googly Eyes
        eyesRenderer.draw(canvas, tokens)

        // 2. Time
        timePaint.color = tokens.font
        timePaint.textSize = 64f * density
        timePaint.textAlign = Paint.Align.CENTER
        canvas.drawText(time, centerX, bounds.top + 536f * density, timePaint)

        // 3. Note
        if (!note.isNullOrEmpty()) {
            textPaint.color = tokens.font
            textPaint.textSize = 24f * density
            textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(note, centerX, bounds.top + 581f * density, textPaint)
        }

        // 4. Label
        subTextPaint.color = tokens.font
        subTextPaint.textSize = 14f * density
        subTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(label, centerX, bounds.top + 601f * density, subTextPaint)

        // 5. Hint
        subTextPaint.color = tokens.font
        subTextPaint.textSize = 14f * density
        subTextPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Swipe up to dismiss, down to snooze", centerX, bounds.top + 676f * density, subTextPaint)
    }

    override fun isAnimating(): Boolean = true

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.y
                return true
            }
            MotionEvent.ACTION_UP -> {
                val deltaY = event.y - startY
                if (deltaY < -threshold) {
                    onDismiss()
                } else if (deltaY > threshold) {
                    onSnooze()
                }
                return true
            }
        }
        return false
    }
}
