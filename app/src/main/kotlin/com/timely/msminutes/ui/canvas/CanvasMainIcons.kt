package com.timely.msminutes.ui.canvas

import android.graphics.Canvas

fun CanvasIcons.drawSettings(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(19.14f, 12.94f)
        cubicTo(19.18f, 12.64f, 19.2f, 12.33f, 19.2f, 12f)
        cubicTo(19.2f, 11.68f, 19.18f, 11.36f, 19.13f, 11.06f)
        lineTo(21.16f, 9.48f)
        cubicTo(21.34f, 9.34f, 21.39f, 9.07f, 21.28f, 8.87f)
        lineTo(19.36f, 5.55f)
        cubicTo(19.24f, 5.33f, 18.99f, 5.26f, 18.77f, 5.33f)
        lineTo(16.38f, 6.29f)
        cubicTo(15.88f, 5.91f, 15.35f, 5.59f, 14.76f, 5.35f)
        lineTo(14.4f, 2.81f)
        cubicTo(14.36f, 2.57f, 14.16f, 2.4f, 13.92f, 2.4f)
        lineTo(10.08f, 2.4f)
        cubicTo(9.84f, 2.4f, 9.65f, 2.57f, 9.61f, 2.81f)
        lineTo(9.25f, 5.35f)
        cubicTo(8.66f, 5.59f, 8.12f, 5.92f, 7.63f, 6.29f)
        lineTo(5.24f, 5.33f)
        cubicTo(5.02f, 5.25f, 4.77f, 5.33f, 4.65f, 5.55f)
        lineTo(2.73f, 8.87f)
        cubicTo(2.61f, 9.08f, 2.65f, 9.34f, 2.85f, 9.48f)
        lineTo(4.88f, 11.06f)
        cubicTo(4.84f, 11.36f, 4.81f, 11.69f, 4.81f, 12f)
        cubicTo(4.81f, 12.31f, 4.83f, 12.65f, 4.88f, 12.94f)
        lineTo(2.85f, 14.52f)
        cubicTo(2.67f, 14.66f, 2.62f, 14.93f, 2.73f, 15.13f)
        lineTo(4.65f, 18.45f)
        cubicTo(4.77f, 18.67f, 5.02f, 18.74f, 5.24f, 18.67f)
        lineTo(7.63f, 17.71f)
        cubicTo(8.13f, 18.09f, 8.66f, 18.41f, 9.25f, 18.65f)
        lineTo(9.61f, 21.19f)
        cubicTo(9.66f, 21.43f, 9.85f, 21.6f, 10.09f, 21.6f)
        lineTo(13.93f, 21.6f)
        cubicTo(14.17f, 21.6f, 14.37f, 21.43f, 14.4f, 21.19f)
        lineTo(14.76f, 18.65f)
        cubicTo(15.35f, 18.41f, 15.89f, 18.08f, 16.38f, 17.71f)
        lineTo(18.77f, 18.67f)
        cubicTo(18.99f, 18.75f, 19.24f, 18.67f, 19.36f, 18.45f)
        lineTo(21.28f, 15.13f)
        cubicTo(21.4f, 14.92f, 21.35f, 14.66f, 21.16f, 14.52f)
        lineTo(19.14f, 12.94f)
        close()
        moveTo(12f, 15.6f)
        cubicTo(10.02f, 15.6f, 8.4f, 13.98f, 8.4f, 12f)
        cubicTo(8.4f, 10.02f, 10.02f, 8.4f, 12f, 8.4f)
        cubicTo(13.98f, 8.4f, 15.6f, 10.02f, 15.6f, 12f)
        cubicTo(15.6f, 13.98f, 13.98f, 15.6f, 12f, 15.6f)
        close()
    }
}

fun CanvasIcons.drawAlarm(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(22f, 5.72f)
        lineTo(17.4f, 1.86f)
        lineTo(16.11f, 3.39f)
        lineTo(20.71f, 7.25f)
        close()
        moveTo(7.88f, 3.39f)
        lineTo(6.59f, 1.86f)
        lineTo(2f, 5.72f)
        lineTo(3.29f, 7.25f)
        close()
        moveTo(12.5f, 8f)
        lineTo(11f, 8f)
        lineTo(11f, 14f)
        lineTo(15.75f, 16.85f)
        lineTo(16.5f, 15.62f)
        lineTo(12.5f, 13.25f)
        lineTo(12.5f, 8f)
        close()
        moveTo(12f, 4f)
        cubicTo(7.03f, 4f, 3f, 8.03f, 3f, 13f)
        cubicTo(3f, 17.97f, 7.03f, 22f, 12f, 22f)
        cubicTo(16.97f, 22f, 21f, 17.97f, 21f, 13f)
        cubicTo(21f, 8.03f, 16.97f, 4f, 12f, 4f)
        close()
        moveTo(12f, 20f)
        cubicTo(8.13f, 20f, 5f, 16.87f, 5f, 13f)
        cubicTo(5f, 9.13f, 8.13f, 6f, 12f, 6f)
        cubicTo(15.87f, 6f, 19f, 9.13f, 19f, 13f)
        cubicTo(19f, 16.87f, 15.87f, 20f, 12f, 20f)
        close()
    }
}

fun CanvasIcons.drawTimer(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(6f, 2f)
        lineTo(18f, 2f)
        lineTo(18f, 7f)
        lineTo(13f, 12f)
        lineTo(18f, 17f)
        lineTo(18f, 22f)
        lineTo(6f, 22f)
        lineTo(6f, 17f)
        lineTo(11f, 12f)
        lineTo(6f, 7f)
        lineTo(6f, 2f)
        close()
        moveTo(16f, 4f)
        lineTo(8f, 4f)
        lineTo(8f, 7.5f)
        lineTo(12f, 11.5f)
        lineTo(16f, 7.5f)
        lineTo(16f, 4f)
        close()
        moveTo(12f, 12.5f)
        lineTo(8f, 16.5f)
        lineTo(8f, 20f)
        lineTo(16f, 20f)
        lineTo(16f, 16.5f)
        lineTo(12f, 12.5f)
        close()
    }
}

fun CanvasIcons.drawStopwatch(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(15f, 1f)
        lineTo(9f, 1f)
        lineTo(9f, 3f)
        lineTo(15f, 3f)
        lineTo(15f, 1f)
        close()
        moveTo(19.03f, 7.39f)
        lineTo(20.45f, 5.97f)
        cubicTo(20.02f, 5.46f, 19.55f, 4.98f, 19.04f, 4.56f)
        lineTo(17.62f, 5.98f)
        cubicTo(16.07f, 4.74f, 14.12f, 4f, 12f, 4f)
        cubicTo(7.03f, 4f, 3f, 8.03f, 3f, 13f)
        cubicTo(3f, 17.97f, 7.02f, 22f, 12f, 22f)
        cubicTo(16.98f, 22f, 21f, 17.97f, 21f, 13f)
        cubicTo(21f, 10.88f, 20.26f, 8.93f, 19.03f, 7.39f)
        close()
        moveTo(12f, 20f)
        cubicTo(8.13f, 20f, 5f, 16.87f, 5f, 13f)
        cubicTo(5f, 9.13f, 8.13f, 6f, 12f, 6f)
        cubicTo(15.87f, 6f, 19f, 9.13f, 19f, 13f)
        cubicTo(19f, 16.87f, 15.87f, 20f, 12f, 20f)
        close()
        moveTo(12.5f, 8f)
        lineTo(12.5f, 13.25f)
        lineTo(15.5f, 15f)
        lineTo(14.75f, 16.25f)
        lineTo(11f, 14f)
        lineTo(11f, 8f)
        lineTo(12.5f, 8f)
        close()
    }
}

fun CanvasIcons.drawPlay(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(8f, 5f)
        lineTo(8f, 19f)
        lineTo(19f, 12f)
        lineTo(8f, 5f)
        close()
    }
}

fun CanvasIcons.drawPause(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(6f, 19f)
        lineTo(10f, 19f)
        lineTo(10f, 5f)
        lineTo(6f, 5f)
        lineTo(6f, 19f)
        close()
        moveTo(14f, 5f)
        lineTo(14f, 19f)
        lineTo(18f, 19f)
        lineTo(18f, 5f)
        lineTo(14f, 5f)
        close()
    }
}

fun CanvasIcons.drawStop(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(6f, 6f)
        lineTo(18f, 6f)
        lineTo(18f, 18f)
        lineTo(6f, 18f)
        close()
    }
}

fun CanvasIcons.drawSave(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(20f, 12f)
        lineTo(18.59f, 10.59f)
        lineTo(13f, 16.17f)
        lineTo(13f, 2f)
        lineTo(11f, 2f)
        lineTo(11f, 16.17f)
        lineTo(5.41f, 10.59f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(20f, 12f)
        close()
        moveTo(5f, 18f)
        lineTo(5f, 20f)
        lineTo(19f, 20f)
        lineTo(19f, 18f)
        lineTo(5f, 18f)
        close()
    }
}

fun CanvasIcons.drawHistory(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color) {
        moveTo(13f, 3f)
        cubicTo(8.03f, 3f, 4f, 7.03f, 4f, 12f)
        cubicTo(4f, 13.5f, 4.38f, 14.91f, 5.04f, 16.14f)
        lineTo(3.64f, 17.54f)
        cubicTo(2.61f, 15.95f, 2f, 14.05f, 2f, 12f)
        cubicTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
        cubicTo(15.28f, 2f, 18.15f, 3.59f, 19.91f, 6.03f)
        lineTo(22f, 4f)
        lineTo(22f, 10f)
        lineTo(16f, 10f)
        lineTo(18.29f, 7.71f)
        cubicTo(16.83f, 5.43f, 14.28f, 4f, 11.41f, 4f)
        cubicTo(11.95f, 4f, 12.48f, 4.05f, 13f, 4.14f)
        moveTo(12.5f, 7f)
        lineTo(12.5f, 13f)
        lineTo(17f, 16f)
        lineTo(16.2f, 17.2f)
        lineTo(11f, 13.8f)
        lineTo(11f, 7f)
        lineTo(12.5f, 7f)
        close()
    }
}

fun CanvasIcons.drawWorldClock(canvas: Canvas, x: Float, y: Float, size: Float, color: Int) {
    drawIcon(canvas, x, y, size, color, stroke = true) {
        addCircle(12f, 12f, 10f, android.graphics.Path.Direction.CW)
        moveTo(2f, 12f)
        lineTo(22f, 12f)
        moveTo(12f, 2f)
        cubicTo(8f, 6f, 8f, 18f, 12f, 22f)
        moveTo(12f, 2f)
        cubicTo(16f, 6f, 16f, 18f, 12f, 22f)
        moveTo(4.9f, 7f)
        lineTo(19.1f, 7f)
        moveTo(4.9f, 17f)
        lineTo(19.1f, 17f)
    }
}
