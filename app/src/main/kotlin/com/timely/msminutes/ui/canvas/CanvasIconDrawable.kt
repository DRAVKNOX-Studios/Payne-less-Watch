package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/**
 * A Drawable that wraps CanvasIcons drawing logic.
 */
class CanvasIconDrawable(
    private val iconType: IconType,
    private var color: Int,
    private val sizeDp: Float,
    private val density: Float
) : Drawable() {

    enum class IconType { SETTINGS, ALARM, TIMER, STOPWATCH, WORLD_CLOCK, ADD, REMOVE }

    override fun draw(canvas: Canvas) {
        val b = bounds
        val size = sizeDp * density
        val x = b.left + (b.width() - size) / 2f
        val y = b.top + (b.height() - size) / 2f

        when (iconType) {
            IconType.SETTINGS -> CanvasIcons.drawSettings(canvas, x, y, size, color)
            IconType.ALARM -> CanvasIcons.drawAlarm(canvas, x, y, size, color)
            IconType.TIMER -> CanvasIcons.drawTimer(canvas, x, y, size, color)
            IconType.STOPWATCH -> CanvasIcons.drawStopwatch(canvas, x, y, size, color)
            IconType.WORLD_CLOCK -> CanvasIcons.drawWorldClock(canvas, x, y, size, color)
            IconType.ADD -> CanvasIcons.drawAdd(canvas, x, y, size, color)
            IconType.REMOVE -> CanvasIcons.drawRemove(canvas, x, y, size, color)
        }
    }

    override fun setAlpha(alpha: Int) {
        // Not implemented for simplicity
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        // Not implemented
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setTint(color: Int) {
        this.color = color
        invalidateSelf()
    }
}
