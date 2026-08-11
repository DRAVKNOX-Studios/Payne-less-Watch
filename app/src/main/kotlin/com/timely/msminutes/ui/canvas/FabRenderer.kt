package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class FabRenderer(
    context: android.content.Context,
    private val onClick: () -> Unit
) : CanvasRenderer {

    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f * density
        textAlign = Paint.Align.CENTER
    }

    var isVisible = false

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (!isVisible) return

        bgPaint.color = tokens.accent
        canvas.drawCircle(bounds.centerX(), bounds.centerY(), bounds.width() / 2f, bgPaint)

        textPaint.color = tokens.textPrimary
        val textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText("+", bounds.centerX(), textY, textPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isVisible) return false
        val dist = Math.hypot((event.x - bounds.centerX()).toDouble(), (event.y - bounds.centerY()).toDouble())
        val isInside = dist <= bounds.width() / 2f
        
        if (event.action == MotionEvent.ACTION_DOWN) {
            return isInside
        }
        if (event.action == MotionEvent.ACTION_UP && isInside) {
            onClick()
            return true
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        if (!isVisible) return
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = bounds,
                label = "Add",
                className = "android.widget.Button"
            )
        )
    }
}
