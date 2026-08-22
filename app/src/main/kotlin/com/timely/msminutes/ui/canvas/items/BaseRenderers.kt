package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer

abstract class BaseItemRenderer(context: Context) : ItemRenderer {
    protected val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var left: Float = 0f
    override var width: Float = 0f
    override var height: Float = 56f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    protected val paddingStart = 16f * density
    protected val paddingEnd = 16f * density

    override fun onPopulateAccessibilityItems(
        items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>,
        listBounds: RectF,
        scrollY: Float
    ) {
        val absoluteTop = listBounds.top + top - scrollY
        val absoluteBottom = absoluteTop + height
        if (absoluteBottom < listBounds.top || absoluteTop > listBounds.bottom) return

        populateAccessibility(items, listBounds, absoluteTop)
    }

    open fun populateAccessibility(
        items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>,
        listBounds: RectF,
        absoluteTop: Float
    ) {
        val absoluteBottom = absoluteTop + height
        val actualWidth = if (width > 0) width else listBounds.width()
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = (top.toInt() xor (left.toInt() shl 16)),
                bounds = RectF(listBounds.left + left, Math.max(listBounds.top, absoluteTop), listBounds.left + left + actualWidth, Math.min(listBounds.bottom, absoluteBottom)),
                label = "", 
                clickable = true
            )
        )
    }

    companion object {
        internal val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val subTextPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val accentLinePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        internal val timePaint = Paint(Paint.ANTI_ALIAS_FLAG)

        fun resetPaints(density: Float) {
            textPaint.apply {
                isAntiAlias = true
                textSize = 16f * density
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.LEFT
                style = Paint.Style.FILL
                color = 0xFF000000.toInt()
                alpha = 255
                clearShadowLayer()
                textScaleX = 1f
                shader = null
            }

            subTextPaint.apply {
                isAntiAlias = true
                textSize = 14f * density
                typeface = Typeface.DEFAULT
                textAlign = Paint.Align.LEFT
                style = Paint.Style.FILL
                color = 0xFF000000.toInt()
                alpha = 255
                clearShadowLayer()
                shader = null
            }

            bgPaint.apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xFF000000.toInt()
                alpha = 255
                clearShadowLayer()
                shader = null
                strokeWidth = 0f
            }

            strokePaint.apply {
                isAntiAlias = true
                style = Paint.Style.STROKE
                strokeWidth = 1f * density
                strokeCap = Paint.Cap.BUTT
                strokeJoin = Paint.Join.MITER
                color = 0xFF000000.toInt()
                alpha = 255
            }

            accentLinePaint.apply {
                isAntiAlias = true
                style = Paint.Style.FILL
                color = 0xFF000000.toInt()
                alpha = 255
            }

            timePaint.apply {
                isAntiAlias = true
                textSize = 54f * density
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.LEFT
                color = 0xFF000000.toInt()
                alpha = 255
            }
        }
    }
}
