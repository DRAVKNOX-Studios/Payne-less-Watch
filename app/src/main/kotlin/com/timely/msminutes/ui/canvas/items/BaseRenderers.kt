package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens

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
            textPaint.reset()
            textPaint.isAntiAlias = true
            textPaint.textSize = 16f * density
            textPaint.typeface = Typeface.DEFAULT
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.style = Paint.Style.FILL
            textPaint.clearShadowLayer()
            textPaint.textScaleX = 1f
            textPaint.shader = null

            subTextPaint.reset()
            subTextPaint.isAntiAlias = true
            subTextPaint.textSize = 14f * density
            subTextPaint.typeface = Typeface.DEFAULT
            subTextPaint.textAlign = Paint.Align.LEFT
            subTextPaint.style = Paint.Style.FILL
            subTextPaint.clearShadowLayer()
            subTextPaint.shader = null

            bgPaint.reset()
            bgPaint.isAntiAlias = true
            bgPaint.style = Paint.Style.FILL
            bgPaint.clearShadowLayer()
            bgPaint.shader = null
            bgPaint.strokeWidth = 0f

            strokePaint.reset()
            strokePaint.isAntiAlias = true
            strokePaint.style = Paint.Style.STROKE
            strokePaint.strokeWidth = 1f * density
            strokePaint.strokeCap = Paint.Cap.BUTT
            strokePaint.strokeJoin = Paint.Join.MITER

            accentLinePaint.reset()
            accentLinePaint.isAntiAlias = true
            accentLinePaint.style = Paint.Style.FILL

            timePaint.reset()
            timePaint.isAntiAlias = true
            timePaint.textSize = 54f * density
            timePaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            timePaint.textAlign = Paint.Align.LEFT
        }
    }
}

class HeaderItemRenderer(context: Context, private val title: String) : BaseItemRenderer(context) {
    override var height: Float = 72f * density
    
    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textPaint.textSize = 13f * density
        textPaint.color = tokens.accent
        val y = height - 20f * density
        canvas.drawText(title.uppercase(), paddingStart, y, textPaint)
    }

    override fun onClick(x: Float, y: Float) {}

    override fun populateAccessibility(items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>, listBounds: RectF, absoluteTop: Float) {
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = (top.toInt() and 0xFFFF),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(absoluteTop + height, listBounds.bottom)),
                label = title,
                clickable = false
            )
        )
    }
}

class ToggleItemRenderer(
    context: Context,
    private val label: String,
    var isChecked: Boolean,
    private val onToggle: (Boolean) -> Unit
) : BaseItemRenderer(context) {

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.color = tokens.textPrimary
        val textY = height / 2f + 6f * density
        canvas.drawText(label, paddingStart, textY, textPaint)

        // Draw Switch
        val swWidth = 44f * density
        val swHeight = 24f * density
        val swX = width - swWidth - paddingEnd
        val swY = (height - swHeight) / 2f
        
        bgPaint.color = if (isChecked) tokens.accent else tokens.textSecondary.withAlpha(0x44)
        canvas.drawRoundRect(swX, swY, swX + swWidth, swY + swHeight, swHeight / 2f, swHeight / 2f, bgPaint)
        
        bgPaint.color = tokens.background
        val thumbR = 9f * density
        val thumbPadding = 3f * density
        val thumbX = if (isChecked) swX + swWidth - thumbR - thumbPadding else swX + thumbR + thumbPadding
        canvas.drawCircle(thumbX, swY + swHeight / 2f, thumbR, bgPaint)
    }

    override fun onClick(x: Float, y: Float) {
        isChecked = !isChecked
        onToggle(isChecked)
    }
    
    private fun Int.withAlpha(alpha: Int): Int {
        return (this and 0x00FFFFFF) or (alpha shl 24)
    }

    override fun populateAccessibility(items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>, listBounds: RectF, absoluteTop: Float) {
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = (top.toInt() and 0xFFFF),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(absoluteTop + height, listBounds.bottom)),
                label = label,
                className = "android.widget.Switch",
                selected = isChecked
            )
        )
    }
}

class EditItemRenderer(
    context: Context,
    private val label: String,
    var value: String,
    private val onClickAction: () -> Unit
) : BaseItemRenderer(context) {

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.color = tokens.textPrimary
        val labelY = height / 2f - 4f * density
        canvas.drawText(label, paddingStart, labelY, textPaint)

        subTextPaint.color = tokens.textSecondary
        val valueY = height / 2f + 16f * density
        canvas.drawText(value.ifEmpty { "None" }, paddingStart, valueY, subTextPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }

    override fun populateAccessibility(items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>, listBounds: RectF, absoluteTop: Float) {
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = (top.toInt() and 0xFFFF),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(absoluteTop + height, listBounds.bottom)),
                label = "$label: ${value.ifEmpty { "None" }}",
                className = "android.widget.Button"
            )
        )
    }
}

class SelectorItemRenderer(
    context: Context,
    private val label: String,
    var selectedValue: String,
    private val onClickAction: () -> Unit
) : BaseItemRenderer(context) {

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.color = tokens.textPrimary
        val labelY = height / 2f + 6f * density
        canvas.drawText(label, paddingStart, labelY, textPaint)

        subTextPaint.color = tokens.accent
        val valueWidth = subTextPaint.measureText(selectedValue)
        val valueY = height / 2f + 6f * density
        canvas.drawText(selectedValue, width - valueWidth - paddingEnd, valueY, subTextPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }
}

class ColorPickerItemRenderer(
    context: Context,
    private val label: String,
    var color: Int,
    private val onClickAction: () -> Unit
) : BaseItemRenderer(context) {

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        textPaint.color = tokens.textPrimary
        val labelY = height / 2f + 6f * density
        canvas.drawText(label, paddingStart, labelY, textPaint)

        val circleR = 12f * density
        val circleX = width - circleR - paddingEnd
        val circleY = height / 2f
        bgPaint.color = color
        canvas.drawCircle(circleX, circleY, circleR, bgPaint)
    }

    override fun onClick(x: Float, y: Float) {
        onClickAction()
    }
}
