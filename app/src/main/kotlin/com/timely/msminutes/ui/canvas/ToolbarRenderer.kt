package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.timely.msminutes.ui.canvas.items.BaseItemRenderer
import com.timely.msminutes.util.ThemeTokens

class ToolbarRenderer(
    context: android.content.Context,
    private val title: String,
    private val showBack: Boolean = false,
    private val onActionClick: () -> Unit
) : CanvasRenderer {

    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val clockRenderer = AnalogClockRenderer(density)
    
    private val actionIconBounds = RectF()

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        val size = 24f * density
        val margin = (bottom - top - size) / 2f
        if (showBack) {
            actionIconBounds.set(left + 16f * density, top + margin, left + 16f * density + size, bottom - margin)
        } else {
            actionIconBounds.set(right - size - margin - 8f * density, top + margin, right - margin - 8f * density, bottom - margin)
        }
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        BaseItemRenderer.resetPaints(density)
        val textPaint = BaseItemRenderer.textPaint

        val iconSize = if (showBack) 24f * density else 42f * density
        val iconMargin = if (showBack) 16f * density else 12f * density
        val iconY = bounds.centerY() - iconSize / 2f

        if (showBack) {
            CanvasIcons.drawBack(canvas, actionIconBounds.left, actionIconBounds.top, actionIconBounds.width(), tokens.textPrimary)
        } else {
            clockRenderer.draw(canvas, bounds.left + iconMargin, iconY, iconSize, tokens)
        }

        // Draw title
        textPaint.color = tokens.textPrimary
        textPaint.textSize = 20f * density
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val textX = if (showBack) actionIconBounds.right + 24f * density else bounds.left + iconMargin + iconSize + 16f * density
        val textY = bounds.centerY() - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(title, textX, textY, textPaint)

        if (!showBack) {
            CanvasIcons.drawSettings(
                canvas,
                actionIconBounds.left,
                actionIconBounds.top,
                actionIconBounds.width(),
                tokens.textPrimary
            )
        }
    }

    override fun isAnimating(): Boolean = !showBack

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return actionIconBounds.contains(event.x, event.y)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            if (actionIconBounds.contains(event.x, event.y)) {
                onActionClick()
                return true
            }
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        // Icon / Action button
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = actionIconBounds,
                label = if (showBack) "Back" else "Settings",
                className = "android.widget.Button"
            )
        )
        
        // Title (non-interactive header)
        val titleBounds = RectF(bounds.left + 72f * density, bounds.top, bounds.right - 72f * density, bounds.bottom)
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 1,
                bounds = titleBounds,
                label = title,
                clickable = false
            )
        )
    }
}
