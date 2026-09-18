package com.timely.msminutes.ui.canvas

import android.graphics.Canvas

fun CanvasIcons.drawAdd(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19f, 13f)
        lineTo(13f, 13f)
        lineTo(13f, 19f)
        cubicTo(13f, 19.55f, 12.55f, 20f, 12f, 20f)
        cubicTo(11.45f, 20f, 11f, 19.55f, 11f, 19f)
        lineTo(11f, 13f)
        lineTo(5f, 13f)
        cubicTo(4.45f, 13f, 4f, 12.55f, 4f, 12f)
        cubicTo(4f, 11.45f, 4.45f, 11f, 5f, 11f)
        lineTo(11f, 11f)
        lineTo(11f, 5f)
        cubicTo(11f, 4.45f, 11.45f, 4f, 12f, 4f)
        cubicTo(12.55f, 4f, 13f, 4.45f, 13f, 5f)
        lineTo(13f, 11f)
        lineTo(19f, 11f)
        cubicTo(19.55f, 11f, 20f, 11.45f, 20f, 12f)
        cubicTo(20f, 12.55f, 19.55f, 13f, 19f, 13f)
        close()
    }
}

fun CanvasIcons.drawRemove(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19f, 13f)
        lineTo(5f, 13f)
        cubicTo(4.45f, 13f, 4f, 12.55f, 4f, 12f)
        cubicTo(4f, 11.45f, 4.45f, 11f, 5f, 11f)
        lineTo(19f, 11f)
        cubicTo(19.55f, 11f, 20f, 11.45f, 20f, 12f)
        cubicTo(20f, 12.55f, 19.55f, 13f, 19f, 13f)
        close()
    }
}

fun CanvasIcons.drawClose(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(18.3f, 5.71f)
        cubicTo(17.91f, 5.32f, 17.28f, 5.32f, 16.89f, 5.71f)
        lineTo(12f, 10.59f)
        lineTo(7.11f, 5.7f)
        cubicTo(6.72f, 5.31f, 6.09f, 5.31f, 5.7f, 5.7f)
        cubicTo(5.31f, 6.09f, 5.31f, 6.72f, 5.7f, 7.11f)
        lineTo(10.59f, 12f)
        lineTo(5.7f, 16.89f)
        cubicTo(5.31f, 17.28f, 5.31f, 17.91f, 5.7f, 18.3f)
        cubicTo(6.09f, 18.69f, 6.72f, 18.69f, 7.11f, 18.3f)
        lineTo(12f, 13.41f)
        lineTo(16.89f, 18.3f)
        cubicTo(17.28f, 18.69f, 17.91f, 18.69f, 18.3f, 18.3f)
        cubicTo(18.69f, 17.91f, 18.69f, 17.28f, 18.3f, 16.89f)
        lineTo(13.41f, 12f)
        lineTo(18.3f, 7.11f)
        cubicTo(18.69f, 6.72f, 18.69f, 6.09f, 18.3f, 5.71f)
        close()
    }
}

fun CanvasIcons.drawBack(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(20f, 11f)
        lineTo(7.83f, 11f)
        lineTo(12.7f, 6.12f)
        cubicTo(13.09f, 5.73f, 13.09f, 5.09f, 12.7f, 4.7f)
        cubicTo(12.31f, 4.31f, 11.67f, 4.31f, 11.28f, 4.7f)
        lineTo(4.7f, 11.29f)
        cubicTo(4.31f, 11.68f, 4.31f, 12.31f, 4.7f, 12.7f)
        lineTo(11.28f, 19.29f)
        cubicTo(11.67f, 19.68f, 12.31f, 19.68f, 12.7f, 19.29f)
        cubicTo(13.09f, 18.9f, 13.09f, 18.26f, 12.7f, 17.87f)
        lineTo(7.83f, 13f)
        lineTo(20f, 13f)
        cubicTo(20.55f, 13f, 21f, 12.55f, 21f, 12f)
        cubicTo(21f, 11.45f, 20.55f, 11f, 20f, 11f)
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
        lineTo(20.29f, 21.79f)
        cubicTo(20.68f, 22.18f, 21.31f, 22.18f, 21.7f, 21.79f)
        cubicTo(22.09f, 21.4f, 22.09f, 20.77f, 21.7f, 20.38f)
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
