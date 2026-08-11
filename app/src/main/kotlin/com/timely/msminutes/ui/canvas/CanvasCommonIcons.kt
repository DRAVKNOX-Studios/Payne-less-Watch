package com.timely.msminutes.ui.canvas

import android.graphics.Canvas

fun CanvasIcons.drawAdd(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19f, 13f)
        lineTo(13f, 13f)
        lineTo(13f, 19f)
        lineTo(11f, 19f)
        lineTo(11f, 13f)
        lineTo(5f, 13f)
        lineTo(5f, 11f)
        lineTo(11f, 11f)
        lineTo(11f, 5f)
        lineTo(13f, 5f)
        lineTo(13f, 11f)
        lineTo(19f, 11f)
        lineTo(19f, 13f)
        close()
    }
}

fun CanvasIcons.drawRemove(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19f, 13f)
        lineTo(5f, 13f)
        lineTo(5f, 11f)
        lineTo(19f, 11f)
        lineTo(19f, 13f)
        close()
    }
}

fun CanvasIcons.drawClose(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19f, 6.41f)
        lineTo(17.59f, 5f)
        lineTo(12f, 10.59f)
        lineTo(6.41f, 5f)
        lineTo(5f, 6.41f)
        lineTo(10.59f, 12f)
        lineTo(5f, 17.59f)
        lineTo(6.41f, 19f)
        lineTo(12f, 13.41f)
        lineTo(17.59f, 19f)
        lineTo(19f, 17.59f)
        lineTo(13.41f, 12f)
        lineTo(19f, 6.41f)
        close()
    }
}

fun CanvasIcons.drawBack(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(20f, 11f)
        lineTo(7.83f, 11f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        lineTo(20f, 13f)
        lineTo(20f, 11f)
        close()
    }
}

fun CanvasIcons.drawSearch(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(15.5f, 14f)
        lineTo(14.71f, 14f)
        lineTo(14.43f, 13.73f)
        cubicTo(15.41f, 12.59f, 16f, 11.11f, 16f, 9.5f)
        cubicTo(16f, 5.91f, 13.09f, 3f, 9.5f, 3f)
        cubicTo(5.91f, 3f, 3f, 5.91f, 3f, 9.5f)
        cubicTo(3f, 13.09f, 5.91f, 16f, 9.5f, 16f)
        cubicTo(11.11f, 16f, 12.59f, 15.41f, 13.73f, 14.43f)
        lineTo(14f, 14.71f)
        lineTo(14f, 15.5f)
        lineTo(19f, 20.49f)
        lineTo(20.49f, 19f)
        lineTo(15.5f, 14f)
        close()
        moveTo(9.5f, 14f)
        cubicTo(7.01f, 14f, 5f, 11.99f, 5f, 9.5f)
        cubicTo(5f, 7.01f, 7.01f, 5f, 9.5f, 5f)
        cubicTo(11.99f, 5f, 14f, 7.01f, 14f, 9.5f)
        cubicTo(14f, 11.99f, 11.99f, 14f, 9.5f, 14f)
        close()
    }
}
