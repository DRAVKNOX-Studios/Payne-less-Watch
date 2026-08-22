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

    private val mainTabs = listOf(
        TabInfo("Alarm", IconType.ALARM),
        TabInfo("Timer", IconType.TIMER),
        TabInfo("Stopwatch", IconType.STOPWATCH),
        TabInfo("World", IconType.WORLD_CLOCK)
    )

    var showAddButton = false
    var onAddClick: (() -> Unit)? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 12f * density
        textAlign = Paint.Align.CENTER
    }

    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        // Draw pill background with shadow
        backgroundPaint.color = tokens.surface
        backgroundPaint.setShadowLayer(12f * density, 0f, 6f * density, 0x66000000)
        val cornerRadius = bounds.height() / 2f
        canvas.drawRoundRect(bounds, cornerRadius, cornerRadius, backgroundPaint)
        backgroundPaint.clearShadowLayer()

        val allTabs = if (showAddButton) {
            mainTabs + TabInfo("Add", IconType.ADD)
        } else {
            mainTabs
        }
        val tabWidth = bounds.width() / allTabs.size
        
        // Adjust text size if tabs are narrow
        textPaint.textSize = if (tabWidth < 72f * density) 10f * density else 12f * density
        
        val iconSize = 24f * density
        val unselectedAlpha = 0x88
        val unselectedColor = (tokens.textPrimary and 0x00FFFFFF) or (unselectedAlpha shl 24)
        
        // Unified vertical layout for all icons and text
        // Pill height is 64dp, so 32dp is the vertical center
        // We push icons up slightly (to 28dp) to make room for labels at the bottom
        val iconCenterY = bounds.top + 28f * density
        val textBaselineY = bounds.top + 54f * density

        for (i in allTabs.indices) {
            val tab = allTabs[i]
            val centerX = bounds.left + i * tabWidth + tabWidth / 2f
            
            val isAddTab = tab.iconType == IconType.ADD
            val isSelected = !isAddTab && mainTabs.indexOf(tab) == selectedIndex
            val color = if (isSelected || isAddTab) tokens.accent else unselectedColor

            if (isAddTab) {
                // Draw Add button background - centered vertically in the pill
                val circleRadius = 26f * density
                backgroundPaint.color = tokens.accent
                backgroundPaint.setShadowLayer(8f * density, 0f, 4f * density, 0x44000000)
                canvas.drawCircle(centerX, bounds.centerY(), circleRadius, backgroundPaint)
                backgroundPaint.clearShadowLayer()
                
                val addIconSize = 36f * density
                CanvasIcons.drawAdd(canvas, centerX - addIconSize / 2f, bounds.centerY() - addIconSize / 2f, addIconSize, tokens.font)
            } else {
                // Draw Selected Highlight (only for main tabs)
                if (isSelected) {
                    backgroundPaint.color = tokens.accent
                    backgroundPaint.alpha = 0x1A // ~10% opacity
                    val rectSize = 48f * density
                    val rect = RectF(
                        centerX - rectSize / 2f,
                        bounds.top + 8f * density,
                        centerX + rectSize / 2f,
                        bounds.bottom - 8f * density
                    )
                    canvas.drawRoundRect(rect, 16f * density, 16f * density, backgroundPaint)
                    backgroundPaint.alpha = 0xFF
                }

                // Draw Icon
                val iconX = centerX - iconSize / 2f
                // Optical vertical alignment adjustments based on icon path centers
                val iconY = when (tab.iconType) {
                    IconType.ALARM -> iconCenterY - 13f * density
                    IconType.STOPWATCH -> iconCenterY - 11.5f * density
                    else -> iconCenterY - 12f * density
                }
                
                when (tab.iconType) {
                    IconType.ALARM -> CanvasIcons.drawAlarm(canvas, iconX, iconY, iconSize, color)
                    IconType.TIMER -> CanvasIcons.drawTimer(canvas, iconX, iconY, iconSize, color)
                    IconType.STOPWATCH -> CanvasIcons.drawStopwatch(canvas, iconX, iconY, iconSize, color)
                    IconType.WORLD_CLOCK -> CanvasIcons.drawWorldClock(canvas, iconX, iconY, iconSize, color)
                    else -> {}
                }

                // Draw Text Label for main tabs
                textPaint.color = color
                textPaint.isFakeBoldText = isSelected
                canvas.drawText(tab.label, centerX, textBaselineY, textPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) return true
        if (event.action == MotionEvent.ACTION_UP) {
            val allTabs = if (showAddButton) {
                mainTabs + TabInfo("Add", IconType.ADD)
            } else {
                mainTabs
            }
            val tabWidth = bounds.width() / allTabs.size
            val index = ((event.x - bounds.left) / tabWidth).toInt()
            if (index in allTabs.indices) {
                val tab = allTabs[index]
                if (tab.iconType == IconType.ADD) {
                    onAddClick?.invoke()
                } else {
                    val mainIndex = mainTabs.indexOf(tab)
                    if (mainIndex != -1) {
                        selectedIndex = mainIndex
                        onTabClick(mainIndex)
                    }
                }
                return true
            }
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        val allTabs = if (showAddButton) {
            mainTabs + TabInfo("Add", IconType.ADD)
        } else {
            mainTabs
        }
        val tabWidth = bounds.width() / allTabs.size
        for (i in allTabs.indices) {
            val tab = allTabs[i]
            val itemBounds = RectF(
                bounds.left + i * tabWidth,
                bounds.top,
                bounds.left + (i + 1) * tabWidth,
                bounds.bottom
            )
            val isSelected = tab.iconType != IconType.ADD && mainTabs.indexOf(tab) == selectedIndex
            items.add(
                CanvasRenderer.AccessibilityItem(
                    id = i,
                    bounds = itemBounds,
                    label = tab.label,
                    className = "android.widget.Button",
                    selected = isSelected
                )
            )
        }
    }

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
    }

    private data class TabInfo(val label: String, val iconType: IconType)
    private enum class IconType { ALARM, TIMER, STOPWATCH, WORLD_CLOCK, ADD }
}
