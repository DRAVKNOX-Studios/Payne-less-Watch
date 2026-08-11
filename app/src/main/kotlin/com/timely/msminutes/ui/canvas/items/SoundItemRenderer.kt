package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens

class SoundItemRenderer(
    context: Context,
    private val name: String,
    private val isSelected: Boolean,
    private val onClickAction: () -> Unit
) : ItemRenderer {
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 48f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false
    
    private val paddingStart = 16f * density

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        BaseItemRenderer.resetPaints(density)
        val textPaint = BaseItemRenderer.textPaint
        val bgPaint = BaseItemRenderer.bgPaint

        if (isSelected) {
            bgPaint.color = (tokens.accent and 0x00FFFFFF) or (0x33 shl 24)
            canvas.drawRoundRect(8f * density, 4f * density, width - 8f * density, height - 4f * density, 8f * density, 8f * density, bgPaint)
            textPaint.typeface = Typeface.DEFAULT_BOLD
            textPaint.color = tokens.accent
        } else {
            textPaint.typeface = Typeface.DEFAULT
            textPaint.color = tokens.textPrimary
        }

        canvas.drawText(name, paddingStart, height / 2f + 6f * density, textPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }
}
