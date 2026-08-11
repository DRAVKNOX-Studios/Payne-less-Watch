package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import com.timely.msminutes.util.ThemeTokens

interface ItemRenderer {
    val id: Long get() = -1L
    var top: Float
    var left: Float get() = 0f
        set(value) {}
    var width: Float get() = 0f
        set(value) {}
    var height: Float
    var swipeX: Float
    val isSwipeable: Boolean get() = true
    fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float)
    fun drawBackground(canvas: Canvas, tokens: ThemeTokens, width: Float) {}
    fun onClick(x: Float, y: Float)
    fun onTouchEvent(event: android.view.MotionEvent, x: Float, y: Float): Boolean = false
    fun onDelete() {}
    fun onCopy() {}

    fun onPopulateAccessibilityItems(
        items: MutableList<CanvasRenderer.AccessibilityItem>,
        listBounds: android.graphics.RectF,
        scrollY: Float
    ) { }
}
