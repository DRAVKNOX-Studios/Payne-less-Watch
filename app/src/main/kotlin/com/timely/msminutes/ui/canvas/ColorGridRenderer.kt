package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class ColorGridRenderer(
    context: Context,
    private val colors: IntArray,
    private val onColorSelected: (Int) -> Unit
) : CanvasRenderer {
    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        val columns = 4
        val swatchSize = 48f * density
        val spacing = (bounds.width() - (columns * swatchSize)) / (columns + 1)
        
        for (i in colors.indices) {
            val row = i / columns
            val col = i % columns
            
            val cx = bounds.left + spacing + col * (swatchSize + spacing) + swatchSize / 2f
            val cy = bounds.top + spacing + row * (swatchSize + spacing) + swatchSize / 2f
            
            paint.color = colors[i]
            canvas.drawCircle(cx, cy, swatchSize / 2f, paint)
            
            strokePaint.color = (tokens.textPrimary and 0x00FFFFFF) or 0x33000000
            canvas.drawCircle(cx, cy, swatchSize / 2f, strokePaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action == MotionEvent.ACTION_UP) {
            val columns = 4
            val swatchSize = 48f * density
            val spacing = (bounds.width() - (columns * swatchSize)) / (columns + 1)
            
            for (i in colors.indices) {
                val row = i / columns
                val col = i % columns
                
                val cx = bounds.left + spacing + col * (swatchSize + spacing) + swatchSize / 2f
                val cy = bounds.top + spacing + row * (swatchSize + spacing) + swatchSize / 2f
                
                val dx = event.x - cx
                val dy = event.y - cy
                if (dx * dx + dy * dy <= (swatchSize / 2f) * (swatchSize / 2f)) {
                    onColorSelected(colors[i])
                    return true
                }
            }
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        val columns = 4
        val swatchSize = 48f * density
        val spacing = (bounds.width() - (columns * swatchSize)) / (columns + 1)
        
        for (i in colors.indices) {
            val row = i / columns
            val col = i % columns
            
            val cx = bounds.left + spacing + col * (swatchSize + spacing) + swatchSize / 2f
            val cy = bounds.top + spacing + row * (swatchSize + spacing) + swatchSize / 2f
            
            val itemBounds = RectF(cx - swatchSize / 2f, cy - swatchSize / 2f, cx + swatchSize / 2f, cy + swatchSize / 2f)
            items.add(
                CanvasRenderer.AccessibilityItem(
                    id = i,
                    bounds = itemBounds,
                    label = "Color ${i + 1}", // Could be better if we had color names
                    className = "android.widget.Button"
                )
            )
        }
    }
}
