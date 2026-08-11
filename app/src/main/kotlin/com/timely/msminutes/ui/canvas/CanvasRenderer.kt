package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.RectF
import android.view.MotionEvent
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.timely.msminutes.util.ThemeTokens

/**
 * Interface for a component that renders directly to a Canvas.
 * Each renderer handles one specific "job" (e.g., Toolbar, TabBar).
 */
interface CanvasRenderer {
    /** The bounds of this renderer within the host view. */
    val bounds: RectF

    /** Draws the component onto the canvas using current theme tokens. */
    fun draw(canvas: Canvas, tokens: ThemeTokens)

    /** Handles touch events. Returns true if the event was consumed. */
    fun onTouchEvent(event: MotionEvent): Boolean = false

    /** Called when the host view size changes or layout is required. */
    fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        bounds.set(left, top, right, bottom)
    }

    /** Returns true if this renderer has active animations. */
    fun isAnimating(): Boolean = false

    /**
     * Optional: Populates accessibility nodes for this renderer.
     * If this renderer represents multiple interactive items (like a TabBar),
     * it can return multiple virtual items.
     */
    fun onPopulateAccessibilityItems(items: MutableList<AccessibilityItem>) {
        // Default implementation treats the whole renderer as one item if it has a label
    }

    data class AccessibilityItem(
        val id: Int,
        val bounds: RectF,
        val label: String,
        val className: String? = null,
        val clickable: Boolean = true,
        val focused: Boolean = false,
        val selected: Boolean = false,
        val actions: Int = AccessibilityNodeInfoCompat.ACTION_CLICK
    )
}
