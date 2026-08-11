package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens

class TimeZoneItemRenderer(
    context: Context,
    val city: String,
    val region: String,
    private val onClickAction: () -> Unit
) : ItemRenderer {
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 64f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    private val paddingStart = 16f * density

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        BaseItemRenderer.resetPaints(density)
        val textPaint = BaseItemRenderer.textPaint
        val subTextPaint = BaseItemRenderer.subTextPaint

        textPaint.color = tokens.textPrimary
        textPaint.textSize = 18f * density
        textPaint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText(city, paddingStart, 30f * density, textPaint)

        subTextPaint.color = tokens.textSecondary
        subTextPaint.textSize = 14f * density
        canvas.drawText(region, paddingStart, 52f * density, subTextPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }
}
