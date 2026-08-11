package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.util.ThemeTokens

class ButtonItemRenderer(
    context: Context,
    private val text: String,
    private val isDanger: Boolean = false,
    private val onClickAction: () -> Unit
) : BaseItemRenderer(context) {
    override var height: Float = 72f * density

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        val r = 16f * density
        val rect = RectF(paddingStart, 8f * density, width - paddingEnd, height - 8f * density)
        
        bgPaint.color = if (isDanger) 0x22FF0000 else tokens.accent
        canvas.drawRoundRect(rect, r, r, bgPaint)
        
        textPaint.color = if (isDanger) 0xFFFF4444.toInt() else tokens.font
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(text, rect.centerX(), rect.centerY() + 6f * density, textPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }

    override fun populateAccessibility(items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>, listBounds: RectF, absoluteTop: Float) {
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = (top.toInt() and 0xFFFF),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(absoluteTop + height, listBounds.bottom)),
                label = text,
                className = "android.widget.Button"
            )
        )
    }
}
