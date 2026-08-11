package com.timely.msminutes.ui.canvas

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import com.timely.msminutes.util.ThemeTokens

class StopwatchHeaderRenderer(
    context: android.content.Context,
    private val onStartPause: () -> Unit,
    private val onLapReset: () -> Unit
) : CanvasRenderer {

    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 40f * density
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val btnPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val btnTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 14f * density
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val leftBtnBounds = RectF()
    private val rightBtnBounds = RectF()

    var timeText: String = "00:00.00"
    var leftBtnText: String = "Start"
    var rightBtnText: String = "Reset"

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        val centerX = bounds.centerX()
        val btnY = bottom - 40f * density
        val btnWidth = 80f * density
        val btnHeight = 36f * density
        leftBtnBounds.set(centerX - btnWidth - 8f * density, btnY, centerX - 8f * density, btnY + btnHeight)
        rightBtnBounds.set(centerX + 8f * density, btnY, centerX + btnWidth + 8f * density, btnY + btnHeight)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        // Draw Time
        timePaint.color = tokens.textPrimary
        canvas.drawText(timeText, bounds.centerX(), bounds.top + 60f * density, timePaint)

        // Draw Left Button
        btnPaint.color = tokens.accent
        canvas.drawRoundRect(leftBtnBounds, 24f * density, 24f * density, btnPaint)
        btnTextPaint.color = tokens.textPrimary
        canvas.drawText(leftBtnText, leftBtnBounds.centerX(), leftBtnBounds.centerY() + 6f * density, btnTextPaint)

        // Draw Right Button
        btnPaint.color = tokens.surface
        canvas.drawRoundRect(rightBtnBounds, 24f * density, 24f * density, btnPaint)
        btnTextPaint.color = tokens.textPrimary
        canvas.drawText(rightBtnText, rightBtnBounds.centerX(), rightBtnBounds.centerY() + 6f * density, btnTextPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return leftBtnBounds.contains(event.x, event.y) || rightBtnBounds.contains(event.x, event.y)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            if (leftBtnBounds.contains(event.x, event.y)) {
                onStartPause()
                return true
            }
            if (rightBtnBounds.contains(event.x, event.y)) {
                onLapReset()
                return true
            }
        }
        return false
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        // Time display
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 0,
                bounds = RectF(bounds.left, bounds.top, bounds.right, leftBtnBounds.top),
                label = "Elapsed time: $timeText",
                clickable = false
            )
        )
        // Left Button
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 1,
                bounds = leftBtnBounds,
                label = leftBtnText,
                className = "android.widget.Button"
            )
        )
        // Right Button
        items.add(
            CanvasRenderer.AccessibilityItem(
                id = 2,
                bounds = rightBtnBounds,
                label = rightBtnText,
                className = "android.widget.Button"
            )
        )
    }
}
