package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.ui.canvas.WheelPickerRenderer
import com.timely.msminutes.util.ThemeTokens

class TimePickerItemRenderer(
    context: Context,
    private val host: android.view.View,
    private val list: com.timely.msminutes.ui.canvas.CanvasListView,
    var hour: Int,
    var minute: Int,
    var is24h: Boolean,
    private val onTimeChange: (Int, Int) -> Unit
) : ItemRenderer {
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var height: Float = 200f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = false

    private val hourPicker: WheelPickerRenderer
    private val minutePicker: WheelPickerRenderer
    private var amPmPicker: WheelPickerRenderer? = null

    init {
        if (is24h) {
            hourPicker = WheelPickerRenderer(context, host, list, 0, 23, hour) { h ->
                hour = h
                onTimeChange(hour, minute)
            }
        } else {
            val displayHour = if (hour == 0) 12 else if (hour > 12) hour - 12 else hour
            hourPicker = WheelPickerRenderer(context, host, list, 1, 12, displayHour) { h ->
                updateHourFromAmPm(h, amPmPicker?.value ?: 0)
            }
            amPmPicker = WheelPickerRenderer(context, host, list, 0, 1, if (hour >= 12) 1 else 0, isLooping = false) { ap ->
                updateHourFromAmPm(hourPicker.value, ap)
            }.apply {
                labels = arrayOf("AM", "PM")
            }
        }
        
        minutePicker = WheelPickerRenderer(context, host, list, 0, 59, minute) { m ->
            minute = m
            onTimeChange(hour, minute)
        }
    }

    private fun updateHourFromAmPm(h: Int, ap: Int) {
        hour = if (ap == 0) { // AM
            if (h == 12) 0 else h
        } else { // PM
            if (h == 12) 12 else h + 12
        }
        onTimeChange(hour, minute)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        val pickerWidth = if (amPmPicker != null) width / 3f else width / 2f
        
        hourPicker.itemTop = top
        hourPicker.onLayout(0f, 0f, pickerWidth, height)
        hourPicker.draw(canvas, tokens)
        
        minutePicker.itemTop = top
        minutePicker.onLayout(pickerWidth, 0f, pickerWidth * 2f, height)
        minutePicker.draw(canvas, tokens)
        
        amPmPicker?.let {
            it.itemTop = top
            it.onLayout(pickerWidth * 2f, 0f, width, height)
            it.draw(canvas, tokens)
        }
    }

    override fun onClick(x: Float, y: Float) {}

    override fun onTouchEvent(event: MotionEvent, x: Float, y: Float): Boolean {
        if (hourPicker.onTouchEvent(event, x, y)) return true
        if (minutePicker.onTouchEvent(event, x, y)) return true
        if (amPmPicker?.onTouchEvent(event, x, y) == true) return true
        
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

        val pickerWidth = if (amPmPicker != null) listBounds.width() / 3f else listBounds.width() / 2f
        val commonActions = androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD or 
                          androidx.core.view.accessibility.AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD

        // Hour Picker
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = android.graphics.RectF(listBounds.left, absoluteTop, listBounds.left + pickerWidth, absoluteBottom).apply { intersect(listBounds) },
                label = "Hour: ${hourPicker.value}",
                className = "android.widget.NumberPicker",
                actions = commonActions
            )
        )

        // Minute Picker
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = 1,
                bounds = android.graphics.RectF(listBounds.left + pickerWidth, absoluteTop, listBounds.left + pickerWidth * 2f, absoluteBottom).apply { intersect(listBounds) },
                label = "Minute: ${minutePicker.value}",
                className = "android.widget.NumberPicker",
                actions = commonActions
            )
        )

        // AM/PM Picker
        amPmPicker?.let {
            items.add(
                com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                    id = 2,
                    bounds = android.graphics.RectF(listBounds.left + pickerWidth * 2f, absoluteTop, listBounds.right, absoluteBottom).apply { intersect(listBounds) },
                    label = if (it.value == 0) "AM" else "PM",
                    className = "android.widget.NumberPicker",
                    actions = commonActions
                )
            )
        }
    }
}
