package com.timely.msminutes.ui.canvas.items

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import kotlin.math.max

class TimerItemRenderer(
    private val androidContext: android.content.Context,
    private val item: TimerItem,
    private val onToggle: () -> Unit,
    private val onReset: () -> Unit,
    private val onDeleteAction: () -> Unit,
    private val onCopyAction: () -> Unit,
    private val onEnd: () -> Unit
) : ItemRenderer {

    override val id: Long get() = item.id
    private val density = androidContext.resources.displayMetrics.density
    override var top: Float = 0f
    override var left: Float = 0f
    override var width: Float = 0f
    override var height: Float = 135f * density
    override var swipeX: Float = 0f

    private val cardRect = RectF()
    private val pauseBounds = RectF()
    private val resetBounds = RectF()
    private val playPath = Path()

    override fun drawBackground(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        if (swipeX > 0f) {
            BaseItemRenderer.resetPaints(density)
            val bgPaint = BaseItemRenderer.bgPaint
            val subTextPaint = BaseItemRenderer.subTextPaint

            val r = 24f * density
            val hMargin = 14f * density
            cardRect.set(hMargin, 8f * density, width - hMargin, height - 8f * density)
            bgPaint.color = Color.parseColor("#E53935")
            canvas.drawRoundRect(cardRect, r, r, bgPaint)
            subTextPaint.color = Color.WHITE
            subTextPaint.typeface = Typeface.DEFAULT_BOLD
            canvas.drawText("DELETE", hMargin + 18f * density, height / 2f + 6f * density, subTextPaint)
        } else if (swipeX < 0f) {
            BaseItemRenderer.resetPaints(density)
            val bgPaint = BaseItemRenderer.bgPaint
            val subTextPaint = BaseItemRenderer.subTextPaint

            val r = 24f * density
            val hMargin = 14f * density
            cardRect.set(hMargin, 8f * density, width - hMargin, height - 8f * density)
            bgPaint.color = tokens.accent
            canvas.drawRoundRect(cardRect, r, r, bgPaint)
            subTextPaint.color = Color.WHITE
            subTextPaint.typeface = Typeface.DEFAULT_BOLD
            val text = "COPY"
            val textW = subTextPaint.measureText(text)
            canvas.drawText(text, width - hMargin - 18f * density - textW, height / 2f + 6f * density, subTextPaint)
        }
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        BaseItemRenderer.resetPaints(density)
        val bgPaint = BaseItemRenderer.bgPaint
        val strokePaint = BaseItemRenderer.strokePaint
        val accentLinePaint = BaseItemRenderer.accentLinePaint
        val subTextPaint = BaseItemRenderer.subTextPaint
        val timePaint = BaseItemRenderer.timePaint
        val textPaint = BaseItemRenderer.textPaint // used for pause/resume icons

        val r = 24f * density
        val hMargin = 14f * density
        cardRect.set(hMargin, 8f * density, width - hMargin, height - 8f * density)

        // 1. Draw Card
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)
        
        // 2. Premium outline & Accent strip
        strokePaint.color = (tokens.textPrimary and 0x00FFFFFF) or 0x1A000000
        canvas.drawRoundRect(cardRect, r, r, strokePaint)

        // Calculate live display time
        val displayMillis = if (item.state == TimerItem.STATE_RUNNING) {
            max(0L, item.endTimestamp - System.currentTimeMillis())
        } else {
            item.remainingMillis
        }

        val isRinging = item.state == TimerItem.STATE_RINGING || 
                       (item.state == TimerItem.STATE_RUNNING && displayMillis <= 0)

        if (item.state == TimerItem.STATE_RUNNING || isRinging) {
            accentLinePaint.color = if (isRinging) Color.parseColor("#E53935") else tokens.accent
            val stripWidth = 4f * density
            val stripX = hMargin + 12f * density
            canvas.drawRoundRect(stripX, cardRect.top + 20f * density, stripX + stripWidth, cardRect.bottom - 20f * density, stripWidth/2, stripWidth/2, accentLinePaint)
        }

        val paddingX = hMargin + 22f * density

        // 3. Draw Time
        timePaint.color = if (isRinging) Color.parseColor("#E53935") else tokens.textPrimary
        timePaint.textSize = 46f * density
        val timeStr = TimeFormatUtil.formatTimer(displayMillis)
        canvas.drawText(timeStr, paddingX, 68f * density, timePaint)

        // 4. Draw Progress Bar Track
        val progress = if (item.totalMillis > 0) displayMillis.toFloat() / item.totalMillis else 0f
        bgPaint.color = (tokens.accent and 0x00FFFFFF) or 0x22000000
        val trackY = 86f * density
        val trackHeight = 6f * density
        val trackWidth = width - paddingX - hMargin - 110f * density
        canvas.drawRoundRect(paddingX, trackY, paddingX + trackWidth, trackY + trackHeight, 3f * density, 3f * density, bgPaint)
        
        // Draw Progress Bar Fill
        bgPaint.color = tokens.accent
        canvas.drawRoundRect(paddingX, trackY, paddingX + (trackWidth * progress), trackY + trackHeight, 3f * density, 3f * density, bgPaint)

        // 5. Draw Label
        subTextPaint.color = tokens.textSecondary
        subTextPaint.typeface = Typeface.DEFAULT_BOLD
        subTextPaint.textSize = 15f * density
        canvas.drawText(item.label ?: "Timer", paddingX, 115f * density, subTextPaint)
        
        // 6. Draw Buttons (Right side)
        // 1. Pause/Resume/End Button
        pauseBounds.set(width - hMargin - 56f * density, 25f * density, width - hMargin - 16f * density, 65f * density)
        
        if (isRinging) {
            bgPaint.color = Color.parseColor("#E53935") // Red for End/Stop
        } else {
            bgPaint.color = tokens.accent
        }
        canvas.drawCircle(pauseBounds.centerX(), pauseBounds.centerY(), 20f * density, bgPaint)
        
        bgPaint.color = tokens.background
        if (isRinging) {
            // Stop icon
            canvas.drawRect(pauseBounds.centerX() - 7f * density, pauseBounds.centerY() - 7f * density, pauseBounds.centerX() + 7f * density, pauseBounds.centerY() + 7f * density, bgPaint)
        } else if (item.state == TimerItem.STATE_RUNNING) {
            // Pause icon
            canvas.drawRect(pauseBounds.centerX() - 6f * density, pauseBounds.centerY() - 8f * density, pauseBounds.centerX() - 2f * density, pauseBounds.centerY() + 8f * density, bgPaint)
            canvas.drawRect(pauseBounds.centerX() + 2f * density, pauseBounds.centerY() - 8f * density, pauseBounds.centerX() + 6f * density, pauseBounds.centerY() + 8f * density, bgPaint)
        } else {
            // Play icon
            playPath.reset()
            playPath.moveTo(pauseBounds.centerX() - 5f * density, pauseBounds.centerY() - 9f * density)
            playPath.lineTo(pauseBounds.centerX() + 9f * density, pauseBounds.centerY())
            playPath.lineTo(pauseBounds.centerX() - 5f * density, pauseBounds.centerY() + 9f * density)
            playPath.close()
            canvas.drawPath(playPath, bgPaint)
        }

        // 2. Restart/Reset Button
        resetBounds.set(width - hMargin - 104f * density, 25f * density, width - hMargin - 64f * density, 65f * density)
        bgPaint.color = (tokens.textPrimary and 0x00FFFFFF) or 0x22000000
        canvas.drawCircle(resetBounds.centerX(), resetBounds.centerY(), 18f * density, bgPaint)

        strokePaint.color = tokens.textPrimary
        strokePaint.strokeWidth = 3f * density
        canvas.drawArc(resetBounds.centerX() - 11f * density, resetBounds.centerY() - 11f * density, resetBounds.centerX() + 11f * density, resetBounds.centerY() + 11f * density, 45f, 270f, false, strokePaint)
        
        // Arrow head
        accentLinePaint.color = tokens.textPrimary
        playPath.reset()
        val ax = resetBounds.centerX() + 8f * density
        val ay = resetBounds.centerY() + 8f * density
        playPath.moveTo(ax - 5f * density, ay)
        playPath.lineTo(ax + 2f * density, ay + 7f * density)
        playPath.lineTo(ax + 2f * density, ay - 7f * density)
        playPath.close()
        canvas.drawPath(playPath, accentLinePaint)
    }

    override fun onClick(x: Float, y: Float) {
        // Calculate isRinging for click consistency
        val displayMillis = if (item.state == TimerItem.STATE_RUNNING) {
            max(0L, item.endTimestamp - System.currentTimeMillis())
        } else {
            item.remainingMillis
        }
        val isRinging = item.state == TimerItem.STATE_RINGING || 
                       (item.state == TimerItem.STATE_RUNNING && displayMillis <= 0)

        if (pauseBounds.contains(x, y)) {
            if (isRinging) {
                onEnd()
            } else {
                onToggle()
            }
        } else if (resetBounds.contains(x, y)) {
            onReset()
        }
    }

    override fun onDelete() {
        onDeleteAction()
    }

    override fun onCopy() {
        onCopyAction()
    }

    override fun onPopulateAccessibilityItems(
        items: MutableList<com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem>,
        listBounds: RectF,
        scrollY: Float
    ) {
        val absoluteTop = listBounds.top + top - scrollY
        val absoluteBottom = absoluteTop + height
        
        if (absoluteBottom < listBounds.top || absoluteTop > listBounds.bottom) return

        val displayMillis = if (item.state == TimerItem.STATE_RUNNING) {
            max(0L, item.endTimestamp - System.currentTimeMillis())
        } else {
            item.remainingMillis
        }
        val timeStr = TimeFormatUtil.formatTimer(displayMillis)
        val label = item.label ?: "Timer"
        val isRinging = item.state == TimerItem.STATE_RINGING || 
                       (item.state == TimerItem.STATE_RUNNING && displayMillis <= 0)
        
        val state = when {
            isRinging -> "Ringing"
            item.state == TimerItem.STATE_RUNNING -> "Running"
            item.state == TimerItem.STATE_PAUSED -> "Paused"
            else -> "Finished"
        }
        val fullDescription = "$label, $timeStr, $state"

        // Main card
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = ((id % 1000).toInt() * 3),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(listBounds.bottom, absoluteBottom)),
                label = fullDescription,
                clickable = false
            )
        )
        
        // Reset button
        val resetAbs = RectF(
            listBounds.left + resetBounds.left,
            absoluteTop + (resetBounds.top - top),
            listBounds.left + resetBounds.right,
            absoluteTop + (resetBounds.bottom - top)
        )
        resetAbs.intersect(listBounds)
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = ((id % 1000).toInt() * 3) + 1,
                bounds = resetAbs,
                label = "Reset",
                className = "android.widget.Button"
            )
        )

        // Toggle button
        val pauseAbs = RectF(
            listBounds.left + pauseBounds.left,
            absoluteTop + (pauseBounds.top - top),
            listBounds.left + pauseBounds.right,
            absoluteTop + (pauseBounds.bottom - top)
        )
        pauseAbs.intersect(listBounds)

        val isRingingAcc = item.state == TimerItem.STATE_RINGING || 
                          (item.state == TimerItem.STATE_RUNNING && displayMillis <= 0)

        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = ((id % 1000).toInt() * 3) + 2,
                bounds = pauseAbs,
                label = when {
                    isRingingAcc -> "End"
                    item.state == TimerItem.STATE_RUNNING -> "Pause"
                    else -> "Start"
                },
                className = "android.widget.Button"
            )
        )
    }
}
