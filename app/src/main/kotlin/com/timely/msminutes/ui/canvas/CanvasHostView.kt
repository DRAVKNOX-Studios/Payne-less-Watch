package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.customview.widget.ExploreByTouchHelper
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens

/**
 * A host view that manages multiple [CanvasRenderer] components.
 * Extended to FrameLayout to allow hosting overlay views (like focused EditText).
 */
class CanvasHostView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr), ThemeStore.ThemeListener {

    private val renderers = mutableListOf<CanvasRenderer>()
    private var tokens: ThemeTokens? = ThemeStore.get().current()
    val density = resources.displayMetrics.density
    var drawBackground = true

    private val accessibilityHelper = CanvasAccessibilityHelper(this)

    init {
        setWillNotDraw(false)
        isClickable = true
        isFocusable = true
        ViewCompat.setAccessibilityDelegate(this, accessibilityHelper)
    }

    fun addRenderer(renderer: CanvasRenderer) {
        renderers.add(renderer)
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun removeRenderer(renderer: CanvasRenderer) {
        renderers.remove(renderer)
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    fun clearRenderers() {
        renderers.clear()
        accessibilityHelper.invalidateRoot()
        invalidate()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        ThemeStore.get().subscribe(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        ThemeStore.get().unsubscribe(this)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        tokens = t
        invalidate()
        requestLayout()
    }

    override fun dispatchDraw(canvas: Canvas) {
        val t = tokens ?: ThemeTokens.DEFAULT
        if (drawBackground) {
            canvas.drawColor(t.background)
        }
        
        for (renderer in renderers) {
            renderer.draw(canvas, t)
            if (renderer.isAnimating()) {
                val b = renderer.bounds
                postInvalidateOnAnimation(
                    b.left.toInt(),
                    b.top.toInt(),
                    b.right.toInt(),
                    b.bottom.toInt()
                )
            }
        }
        // Draw children (overlays) on top of canvas
        super.dispatchDraw(canvas)
    }

    override fun dispatchHoverEvent(event: MotionEvent): Boolean {
        return accessibilityHelper.dispatchHoverEvent(event) || super.dispatchHoverEvent(event)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Route touch events from top-most renderer down (reverse order)
        for (i in renderers.indices.reversed()) {
            val renderer = renderers[i]
            if (renderer.bounds.contains(event.x, event.y)) {
                if (renderer.onTouchEvent(event)) {
                    if (event.action == MotionEvent.ACTION_UP) {
                        performClick()
                    }
                    return true
                }
            }
        }
        return false
    }

    private inner class CanvasAccessibilityHelper(host: View) : ExploreByTouchHelper(host) {
        private val tempItems = mutableListOf<CanvasRenderer.AccessibilityItem>()
        private val rect = Rect()

        private fun getAllItems(): List<Pair<Int, CanvasRenderer.AccessibilityItem>> {
            val all = mutableListOf<Pair<Int, CanvasRenderer.AccessibilityItem>>()
            for (i in renderers.indices) {
                tempItems.clear()
                renderers[i].onPopulateAccessibilityItems(tempItems)
                for (item in tempItems) {
                    // Virtual ID: (rendererIndex << 16) | itemRelativeId
                    val virtualId = (i shl 16) or (item.id and 0xFFFF)
                    all.add(virtualId to item)
                }
            }
            return all
        }

        override fun getVirtualViewAt(x: Float, y: Float): Int {
            for (i in renderers.indices.reversed()) {
                tempItems.clear()
                renderers[i].onPopulateAccessibilityItems(tempItems)
                for (item in tempItems.asReversed()) {
                    if (item.bounds.contains(x, y)) {
                        return (i shl 16) or (item.id and 0xFFFF)
                    }
                }
            }
            return HOST_ID
        }

        override fun getVisibleVirtualViews(virtualViewIds: MutableList<Int>) {
            for (i in renderers.indices) {
                tempItems.clear()
                renderers[i].onPopulateAccessibilityItems(tempItems)
                for (item in tempItems) {
                    virtualViewIds.add((i shl 16) or (item.id and 0xFFFF))
                }
            }
        }

        override fun onPopulateNodeForVirtualView(virtualViewId: Int, node: AccessibilityNodeInfoCompat) {
            val rendererIndex = virtualViewId shr 16
            val itemId = virtualViewId and 0xFFFF
            
            if (rendererIndex !in renderers.indices) {
                node.text = ""
                node.setBoundsInParent(Rect(0, 0, 1, 1))
                return
            }

            tempItems.clear()
            renderers[rendererIndex].onPopulateAccessibilityItems(tempItems)
            val item = tempItems.find { it.id == itemId }

            if (item != null) {
                node.contentDescription = item.label
                item.className?.let { node.className = it }
                node.isClickable = item.clickable
                node.isFocusable = true
                node.isSelected = item.selected
                
                rect.set(
                    item.bounds.left.toInt(),
                    item.bounds.top.toInt(),
                    item.bounds.right.toInt(),
                    item.bounds.bottom.toInt()
                )
                node.setBoundsInParent(rect)
                
                if (item.actions and AccessibilityNodeInfoCompat.ACTION_CLICK != 0) {
                    node.addAction(AccessibilityNodeInfoCompat.ACTION_CLICK)
                }
                if (item.actions and AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD != 0) {
                    node.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)
                }
                if (item.actions and AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD != 0) {
                    node.addAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)
                }
            } else {
                node.text = ""
                node.setBoundsInParent(Rect(0, 0, 1, 1))
            }
        }

        override fun onPerformActionForVirtualView(virtualViewId: Int, action: Int, arguments: android.os.Bundle?): Boolean {
            val rendererIndex = virtualViewId shr 16
            val itemId = virtualViewId and 0xFFFF
            
            if (rendererIndex !in renderers.indices) return false

            if (action == AccessibilityNodeInfoCompat.ACTION_CLICK) {
                // Simulate a touch event for the click
                tempItems.clear()
                renderers[rendererIndex].onPopulateAccessibilityItems(tempItems)
                val item = tempItems.find { it.id == itemId }
                if (item != null) {
                    val centerX = item.bounds.centerX()
                    val centerY = item.bounds.centerY()
                    
                    val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
                    renderers[rendererIndex].onTouchEvent(down)
                    down.recycle()
                    
                    val up = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, centerX, centerY, 0)
                    val handled = renderers[rendererIndex].onTouchEvent(up)
                    up.recycle()
                    
                    if (handled) {
                        sendEventForVirtualView(virtualViewId, AccessibilityEvent.TYPE_VIEW_CLICKED)
                        return true
                    }
                }
            } else if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD || action == AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD) {
                // Delegate to renderer via specialized touch event or new method
                // For simplicity, let's just trigger a "scroll" by simulating vertical drag
                tempItems.clear()
                renderers[rendererIndex].onPopulateAccessibilityItems(tempItems)
                val item = tempItems.find { it.id == itemId }
                if (item != null) {
                    val centerX = item.bounds.centerX()
                    val centerY = item.bounds.centerY()
                    val dy = if (action == AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD) -50f * density else 50f * density
                    
                    val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, centerX, centerY, 0)
                    renderers[rendererIndex].onTouchEvent(down)
                    down.recycle()
                    
                    val move = MotionEvent.obtain(0, 0, MotionEvent.ACTION_MOVE, centerX, centerY + dy, 0)
                    renderers[rendererIndex].onTouchEvent(move)
                    move.recycle()
                    
                    val up = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, centerX, centerY + dy, 0)
                    renderers[rendererIndex].onTouchEvent(up)
                    up.recycle()
                    
                    invalidateVirtualView(virtualViewId)
                    return true
                }
            }
            return false
        }
    }
}
