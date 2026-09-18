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
    private val onLapReset: () -> Unit,
    private val onSave: () -> Unit,
    private val onHistory: () -> Unit
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
    private val saveBtnBounds = RectF()
    private val historyBtnBounds = RectF()

    var timeText: String = "00:00.00"
    var leftBtnText: String = "Start"
    var rightBtnText: String = "Reset"

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        val centerX = bounds.centerX()
        val btnSize = 48f * density
        val margin = 16f * density
        
        val rowY = top + 80f * density
        
        // Arrange 4 buttons in a row
        val totalWidth = btnSize * 4 + margin * 3
        var startX = centerX - totalWidth / 2f
        
        leftBtnBounds.set(startX, rowY, startX + btnSize, rowY + btnSize)
        startX += btnSize + margin
        rightBtnBounds.set(startX, rowY, startX + btnSize, rowY + btnSize)
        startX += btnSize + margin
        saveBtnBounds.set(startX, rowY, startX + btnSize, rowY + btnSize)
        startX += btnSize + margin
        historyBtnBounds.set(startX, rowY, startX + btnSize, rowY + btnSize)
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        // Draw Time
        timePaint.color = tokens.textPrimary
        timePaint.setShadowLayer(8f * density, 0f, 0f, tokens.accent)
        canvas.drawText(timeText, bounds.centerX(), bounds.top + 60f * density, timePaint)

        val iconSize = 24f * density

        // Draw Left Button (Play/Pause)
        btnPaint.color = tokens.accent
        canvas.drawRoundRect(leftBtnBounds, 12f * density, 12f * density, btnPaint)
        if (leftBtnText == "Pause") {
            CanvasIcons.drawPause(canvas, leftBtnBounds.centerX() - iconSize/2, leftBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)
        } else {
            CanvasIcons.drawPlay(canvas, leftBtnBounds.centerX() - iconSize/2, leftBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)
        }

        // Draw Right Button (Lap/Stop/Reset)
        btnPaint.color = tokens.surface
        canvas.drawRoundRect(rightBtnBounds, 12f * density, 12f * density, btnPaint)
        if (rightBtnText == "Reset" || rightBtnText == "Stop") {
            CanvasIcons.drawStop(canvas, rightBtnBounds.centerX() - iconSize/2, rightBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)
        } else {
            CanvasIcons.drawAdd(canvas, rightBtnBounds.centerX() - iconSize/2, rightBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)
        }

        // Draw Save Button
        canvas.drawRoundRect(saveBtnBounds, 12f * density, 12f * density, btnPaint)
        CanvasIcons.drawSave(canvas, saveBtnBounds.centerX() - iconSize/2, saveBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)

        // Draw History Button
        canvas.drawRoundRect(historyBtnBounds, 12f * density, 12f * density, btnPaint)
        CanvasIcons.drawHistory(canvas, historyBtnBounds.centerX() - iconSize/2, historyBtnBounds.centerY() - iconSize/2, iconSize, tokens.textPrimary)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            return leftBtnBounds.contains(event.x, event.y) || 
                   rightBtnBounds.contains(event.x, event.y) ||
                   saveBtnBounds.contains(event.x, event.y) ||
                   historyBtnBounds.contains(event.x, event.y)
        }
        if (event.action == MotionEvent.ACTION_UP) {
            when {
                leftBtnBounds.contains(event.x, event.y) -> onStartPause()
                rightBtnBounds.contains(event.x, event.y) -> onLapReset()
                saveBtnBounds.contains(event.x, event.y) -> onSave()
                historyBtnBounds.contains(event.x, event.y) -> onHistory()
                else -> return false
            }
            return true
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
        // Buttons
        items.add(CanvasRenderer.AccessibilityItem(id = 1, bounds = leftBtnBounds, label = leftBtnText, className = "android.widget.Button"))
        items.add(CanvasRenderer.AccessibilityItem(id = 2, bounds = rightBtnBounds, label = rightBtnText, className = "android.widget.Button"))
        items.add(CanvasRenderer.AccessibilityItem(id = 3, bounds = saveBtnBounds, label = "Save", className = "android.widget.Button"))
        items.add(CanvasRenderer.AccessibilityItem(id = 4, bounds = historyBtnBounds, label = "History", className = "android.widget.Button"))
    }
}
