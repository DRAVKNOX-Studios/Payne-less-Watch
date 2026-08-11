package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.ui.canvas.WheelPickerRenderer
import com.timely.msminutes.util.ThemeTokens

class TimerPickerItemRenderer(
    context: Context,
    private val host: android.view.View,
    private val list: com.timely.msminutes.ui.canvas.CanvasListView,
    var hour: Int,
    var minute: Int,
    var second: Int,
    private val onTimeChange: (Int, Int, Int) -> Unit
) : ItemRenderer {
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 200f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    private val hourPicker: WheelPickerRenderer = WheelPickerRenderer(context, host, list, 0, 23, hour) { h ->
        hour = h
        onTimeChange(hour, minute, second)
    }
    private val minutePicker: WheelPickerRenderer = WheelPickerRenderer(context, host, list, 0, 59, minute) { m ->
        minute = m
        onTimeChange(hour, minute, second)
    }
    private val secondPicker: WheelPickerRenderer = WheelPickerRenderer(context, host, list, 0, 59, second) { s ->
        second = s
        onTimeChange(hour, minute, second)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        val pickerWidth = width / 3f
        
        hourPicker.onLayout(0f, 0f, pickerWidth, height)
        hourPicker.draw(canvas, tokens)
        
        minutePicker.onLayout(pickerWidth, 0f, pickerWidth * 2f, height)
        minutePicker.draw(canvas, tokens)
        
        secondPicker.onLayout(pickerWidth * 2f, 0f, width, height)
        secondPicker.draw(canvas, tokens)
    }

    override fun onClick(x: Float, y: Float) {}

    override fun onTouchEvent(event: MotionEvent, x: Float, y: Float): Boolean {
        if (hourPicker.onTouchEvent(event, x, y)) return true
        if (minutePicker.onTouchEvent(event, x, y)) return true
        if (secondPicker.onTouchEvent(event, x, y)) return true
        return false
    }

    override fun onPopulateAccessibilityItems(
        items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>,
        listBounds: android.graphics.RectF,
        scrollY: Float
    ) {
        val absoluteTop = listBounds.top + top - scrollY
        val absoluteBottom = absoluteTop + height
        if (absoluteBottom < listBounds.top || absoluteTop > listBounds.bottom) return

        val pickerWidth = listBounds.width() / 3f
        val commonActions = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD or 
                          androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD

        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = android.graphics.RectF(listBounds.left, absoluteTop, listBounds.left + pickerWidth, absoluteBottom).apply { intersect(listBounds) },
                label = "Hour: ${hourPicker.value}",
                className = "android.widget.NumberPicker",
                actions = commonActions
            )
        )

        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = 1,
                bounds = android.graphics.RectF(listBounds.left + pickerWidth, absoluteTop, listBounds.left + pickerWidth * 2f, absoluteBottom).apply { intersect(listBounds) },
                label = "Minute: ${minutePicker.value}",
                className = "android.widget.NumberPicker",
                actions = commonActions
            )
        )

        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = 2,
                bounds = android.graphics.RectF(listBounds.left + pickerWidth * 2f, absoluteTop, listBounds.right, absoluteBottom).apply { intersect(listBounds) },
                label = "Second: ${secondPicker.value}",
                className = "android.widget.NumberPicker",
                actions = commonActions
            )
        )
    }
}
