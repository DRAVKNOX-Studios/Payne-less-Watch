package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import com.timely.msminutes.util.ThemeTokens

class CanvasListView(
    private val context: Context,
    private val host: View,
    private val onEmptyChange: (Boolean) -> Unit
) : CanvasRenderer {
    val d = context.resources.displayMetrics.density

    override val bounds = RectF()
    private val scroller = OverScroller(context).apply { setFriction(0.005f) }
    private val scrollListeners = mutableListOf<(Float) -> Unit>()
    internal var scrollY = 0f
        set(value) {
            field = value
            scrollListeners.forEach { it(value) }
        }

    fun addOnScrollListener(l: (Float) -> Unit) { scrollListeners.add(l) }
    fun removeOnScrollListener(l: (Float) -> Unit) { scrollListeners.remove(l) }

    internal val items = mutableListOf<ItemRenderer>()
    private val touchHandler = CanvasListTouchHandler(context, host, this, scroller)

    fun setItems(newItems: List<ItemRenderer>) {
        if (items.size == newItems.size) {
            var allSame = true
            for (i in items.indices) {
                if (items[i] !== newItems[i]) { allSame = false; break }
            }
            if (allSame) return
        }
        items.clear()
        items.addAll(newItems)
        layoutItems()
        clampScroll()
        onEmptyChange(items.isEmpty())
    }

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        val widthChanged = bounds.width() != (right - left)
        super.onLayout(left, top, right, bottom)
        if (widthChanged) {
            layoutItems()
        } else {
            clampScroll()
        }
    }

    private fun layoutItems() {
        var currentTop = 0f
        val w = if (bounds.width() > 0) bounds.width() else context.resources.displayMetrics.widthPixels.toFloat()
        val currentIds = items.map { it.id }.toSet()
        touchHandler.swipeHandler.swipeStates.keys.retainAll { currentIds.contains(it) }
        for (item in items) {
            item.top = currentTop
            item.left = 0f
            item.width = w
            item.swipeX = touchHandler.swipeHandler.swipeStates.getOrDefault(item.id, 0f)
            currentTop += item.height + 12f * d
        }
        clampScroll()
    }

    internal fun clampScroll() { scrollY = scrollY.coerceIn(0f, maxScroll()) }

    internal fun maxScroll(): Float {
        val totalHeight = items.lastOrNull()?.let { it.top + it.height } ?: 0f
        return (totalHeight - bounds.height() + 100f).coerceAtLeast(0f)
    }

    fun getContentHeight(): Float = items.lastOrNull()?.let { it.top + it.height } ?: 0f

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.currY.toFloat()
            host.postInvalidateOnAnimation()
        }
        val drawingWidth = bounds.width()
        canvas.save()
        canvas.clipRect(bounds)
        canvas.translate(bounds.left, bounds.top - scrollY)
        for (item in items) {
            if (item.top + item.height >= scrollY && item.top <= scrollY + bounds.height()) {
                canvas.save()
                canvas.translate(item.left, item.top)
                if (item.swipeX != 0f) item.drawBackground(canvas, tokens, drawingWidth)
                canvas.translate(item.swipeX, 0f)
                item.draw(canvas, tokens, drawingWidth)
                canvas.restore()
            }
        }
        canvas.restore()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHandler.onTouchEvent(event)
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        for (item in this.items) {
            item.onPopulateAccessibilityItems(items, bounds, scrollY)
        }
    }

    override fun isAnimating(): Boolean =
        !scroller.isFinished || touchHandler.swipeHandler.isAnimating()
}
