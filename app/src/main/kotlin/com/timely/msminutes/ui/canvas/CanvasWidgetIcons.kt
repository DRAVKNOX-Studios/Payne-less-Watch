package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

fun CanvasIcons.drawWidgetPreview(canvas: Canvas, width: Float, height: Float, accentColor: Int) {
    val density = width / 250f
    
    // Background - Matching the new WidgetRenderer look
    val rect = RectF(2f * density, 2f * density, width - 2f * density, height - 2f * density)
    val radius = 28f * density
    
    paint.style = Paint.Style.FILL
    paint.color = 0xFFF5F5F5.toInt() // bgColor placeholder
    canvas.drawRoundRect(rect, radius, radius, paint)

    // Inner Highlight
    strokePaint.style = Paint.Style.STROKE
    strokePaint.strokeWidth = 1.2f * density
    strokePaint.color = 0x4DFFFFFF.toInt() // 30% alpha white
    val innerRect = RectF(rect.left + 1f * density, rect.top + 1f * density, rect.right - 1f * density, rect.bottom - 1f * density)
    canvas.drawRoundRect(innerRect, radius - 1f * density, radius - 1f * density, strokePaint)

    // Border
    strokePaint.strokeWidth = 1f * density
    strokePaint.color = (accentColor and 0x00FFFFFF) or (0x5A shl 24) // 35% alpha accent
    canvas.drawRoundRect(rect, radius, radius, strokePaint)

    // Clock digits placeholders (mimicking 10:45)
    paint.style = Paint.Style.FILL
    paint.color = 0xFF212121.toInt() // fontColor placeholder
    
    // '1'
    canvas.drawRect(60f * density, 25f * density, 66f * density, 69f * density, paint)
    // '0'
    val path0 = Path().apply {
        moveTo(78f * density, 25f * density)
        lineTo(98f * density, 25f * density)
        lineTo(98f * density, 69f * density)
        lineTo(78f * density, 69f * density)
        close()
        moveTo(84f * density, 31f * density)
        lineTo(92f * density, 31f * density)
        lineTo(92f * density, 63f * density)
        lineTo(84f * density, 63f * density)
        close()
        fillType = Path.FillType.EVEN_ODD
    }
    canvas.drawPath(path0, paint)
    
    // ':'
    canvas.drawRect(123f * density, 40f * density, 127f * density, 44f * density, paint)
    canvas.drawRect(123f * density, 54f * density, 127f * density, 58f * density, paint)
    
    // '4'
    val path4 = Path().apply {
        moveTo(164f * density, 25f * density)
        lineTo(170f * density, 25f * density)
        lineTo(170f * density, 69f * density)
        lineTo(164f * density, 69f * density)
        lineTo(164f * density, 51f * density)
        lineTo(140f * density, 51f * density)
        lineTo(140f * density, 45f * density)
        lineTo(158f * density, 25f * density)
        lineTo(164f * density, 25f * density)
        close()
    }
    canvas.drawPath(path4, paint)
    
    // '5'
    val path5 = Path().apply {
        moveTo(180f * density, 25f * density)
        lineTo(204f * density, 25f * density)
        lineTo(204f * density, 31f * density)
        lineTo(186f * density, 31f * density)
        lineTo(186f * density, 43f * density)
        lineTo(204f * density, 43f * density)
        lineTo(204f * density, 69f * density)
        lineTo(180f * density, 69f * density)
        lineTo(180f * density, 63f * density)
        lineTo(198f * density, 63f * density)
        lineTo(198f * density, 49f * density)
        lineTo(180f * density, 49f * density)
        lineTo(180f * density, 25f * density)
        close()
    }
    canvas.drawPath(path5, paint)

    // Bottom status bar placeholders
    paint.color = 0xFF9E9E9E.toInt()
    // Alarm icon
    canvas.drawCircle(20f * density, 78f * density, 5f * density, paint)
    // Alarm text
    canvas.drawRect(30f * density, 81f * density, 80f * density, 85f * density, paint)
    // Date text
    canvas.drawRect(170f * density, 81f * density, 235f * density, 85f * density, paint)
}
