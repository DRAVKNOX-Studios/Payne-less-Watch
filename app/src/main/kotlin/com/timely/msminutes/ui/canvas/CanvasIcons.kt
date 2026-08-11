package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.core.graphics.withScale
import androidx.core.graphics.withTranslation

/**
 * Utility to draw vector icons directly on Canvas using Path data.
 * All icons are designed for a 24x24 viewport.
 */
object CanvasIcons {

    internal val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    internal val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    internal val path = Path()

    internal fun drawIcon(
        canvas: Canvas,
        x: Float,
        y: Float,
        size: Float,
        color: Int,
        stroke: Boolean = false,
        pathData: Path.() -> Unit
    ) {
        path.reset()
        path.pathData()
        
        val scale = size / 24f
        canvas.withTranslation(x, y) {
            withScale(scale, scale) {
                if (stroke) {
                    strokePaint.color = color
                    strokePaint.strokeWidth = 2f
                    canvas.drawPath(path, strokePaint)
                } else {
                    paint.color = color
                    canvas.drawPath(path, paint)
                }
            }
        }
    }
}
