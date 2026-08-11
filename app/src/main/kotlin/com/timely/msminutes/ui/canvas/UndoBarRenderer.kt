package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class UndoBarRenderer(
    context: android.content.Context,
    var onUndoClick: () -> Unit
) : CanvasRenderer {

    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * density
    }
    private val undoBtnBounds = RectF()

    var message: String = ""
    var isVisible = false

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        // Undo button on the right of the bar
        val btnWidth = 50f * density
        undoBtnBounds.set(right - btnWidth - 16f * density, top, right - 16f * density, bottom)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (!isVisible) return

        // Draw background (rounded rect)
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(bounds, 8f * density, 8f * density, bgPaint)

        // Draw message
        textPaint.color = tokens.textPrimary
        textPaint.isFakeBoldText = false
        val textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(message, bounds.left + 16f * density, textY, textPaint)

        // Draw UNDO button
        textPaint.color = tokens.accent
        textPaint.isFakeBoldText = true
        canvas.drawText("UNDO", undoBtnBounds.left, textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            if (undoBtnBounds.contains(event.x, event.y)) {
                onUndoClick()
                return true
            }
        }
        return true
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        if (!isVisible) return
        
        // The message
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = RectF(bounds.left, bounds.top, undoBtnBounds.left, bounds.bottom),
                label = message,
                clickable = false
            )
        )
        
        // The Undo button
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 1,
                bounds = undoBtnBounds,
                label = "Undo",
                className = "android.widget.Button"
            )
        )
    }
}
