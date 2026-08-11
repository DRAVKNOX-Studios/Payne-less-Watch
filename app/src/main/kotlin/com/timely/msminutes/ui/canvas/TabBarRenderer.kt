package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class TabBarRenderer(
    context: Context,
    private val onTabClick: (Int) -> Unit
) : CanvasRenderer {

    override val bounds = RectF()
    private var selectedIndex = 0
    private val density = context.resources.displayMetrics.density

    private val tabs = listOf(
        TabInfo("Alarm", IconType.ALARM),
        TabInfo("Timer", IconType.TIMER),
        TabInfo("Stop",  IconType.STOPWATCH),
        TabInfo("World", IconType.WORLD_CLOCK)
    )

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        val tabWidth = bounds.width() / tabs.size
        val iconSize = 24f * density
        val unselectedAlpha = 0x88
        val unselectedColor = (tokens.textPrimary and 0x00FFFFFF) or (unselectedAlpha shl 24)

        for (i in tabs.indices) {
            val isSelected = i == selectedIndex
            val color = if (isSelected) tokens.accent else unselectedColor
            
            val centerX = bounds.left + i * tabWidth + tabWidth / 2f
            
            // Draw Icon
            val iconX = centerX - iconSize / 2f
            val iconY = bounds.top + 8f * density
            when (tabs[i].iconType) {
                IconType.ALARM -> CanvasIcons.drawAlarm(canvas, iconX, iconY, iconSize, color)
                IconType.TIMER -> CanvasIcons.drawTimer(canvas, iconX, iconY, iconSize, color)
                IconType.STOPWATCH -> CanvasIcons.drawStopwatch(canvas, iconX, iconY, iconSize, color)
                IconType.WORLD_CLOCK -> CanvasIcons.drawWorldClock(canvas, iconX, iconY, iconSize, color)
            }

            // Draw Text
            textPaint.color = color
            textPaint.isFakeBoldText = isSelected
            canvas.drawText(tabs[i].label, centerX, bounds.top + iconSize + 20f * density, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return true
        }
        if (event.action == MotionEvent.ACTION_UP) {
            val tabWidth = bounds.width() / tabs.size
            val index = ((event.x - bounds.left) / tabWidth).toInt()
            if (index in tabs.indices) {
                selectedIndex = index
                onTabClick(index)
                return true
            }
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        val tabWidth = bounds.width() / tabs.size
        for (i in tabs.indices) {
            val itemBounds = RectF(
                bounds.left + i * tabWidth,
                bounds.top,
                bounds.left + (i + 1) * tabWidth,
                bounds.bottom
            )
            items.add(
                CanvasRenderer.AccessibilityItem(
                    id = i,
                    bounds = itemBounds,
                    label = tabs[i].label,
                    className = "android.widget.Button", // Or custom "Tab" role if supported
                    selected = i == selectedIndex
                )
            )
        }
    }

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
    }

    private data class TabInfo(val label: String, val iconType: IconType)
    private enum class IconType { ALARM, TIMER, STOPWATCH, WORLD_CLOCK }
}
