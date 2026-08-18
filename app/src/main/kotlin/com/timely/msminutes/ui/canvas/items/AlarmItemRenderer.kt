package com.timely.msminutes.ui.canvas.items

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.AlarmTimeUtil
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil

class AlarmItemRenderer(
    private val androidContext: android.content.Context,
    private val alarm: Alarm,
    private val onToggle: (Boolean) -> Unit,
    private val onClickAction: () -> Unit,
    private val onDeleteAction: () -> Unit,
    private val onCopyAction: () -> Unit
) : ItemRenderer {

    override val id: Long get() = alarm.id
    private val density = androidContext.resources.displayMetrics.density
    override var top: Float = 0f
    override var left: Float = 0f
    override var width: Float = 0f
    override var height: Float = 155f * density
    override var swipeX: Float = 0f
    
    private val cardRect = RectF()
    private val toggleRect = RectF()
    private var is24h = false
    private val deleteColor = Color.parseColor("#E53935")

    override fun drawBackground(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        if (swipeX > 0f) {
            BaseItemRenderer.resetPaints(density)
            val bgPaint = BaseItemRenderer.bgPaint
            val subTextPaint = BaseItemRenderer.subTextPaint

            val r = 24f * density
            val hMargin = 14f * density
            cardRect.set(hMargin, 2f * density, width - hMargin, height - 2f * density)
            
            bgPaint.color = deleteColor
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
            cardRect.set(hMargin, 2f * density, width - hMargin, height - 2f * density)
            
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

        val r = 24f * density
        val hMargin = 14f * density
        cardRect.set(hMargin, 2f * density, width - hMargin, height - 2f * density)

        // 1. Draw Card Background
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)
        
        // 2. Premium outline & Accent strip
        strokePaint.color = (tokens.textPrimary and 0x00FFFFFF) or 0x1A000000
        canvas.drawRoundRect(cardRect, r, r, strokePaint)

        val isEnabled = alarm.isEnabled
        if (isEnabled) {
            accentLinePaint.color = tokens.accent
            val stripWidth = 4f * density
            val stripX = hMargin + 12f * density
            canvas.drawRoundRect(stripX, cardRect.top + 21f * density, stripX + stripWidth, cardRect.bottom - 21f * density, stripWidth/2, stripWidth/2, accentLinePaint)
        }

        val mainColor = if (isEnabled) tokens.textPrimary else tokens.textSecondary
        val paddingX = hMargin + 22f * density

        // 3. Status / Remaining Time (Top Left)
        val remaining = AlarmTimeUtil.getRemainingTimeText(alarm)
        if (remaining != null && isEnabled) {
            subTextPaint.color = tokens.accent
            subTextPaint.typeface = Typeface.DEFAULT_BOLD
            subTextPaint.textSize = 11f * density
            canvas.drawText(remaining.uppercase(), paddingX, 30f * density, subTextPaint)
        } else if (!isEnabled) {
            subTextPaint.color = (tokens.textSecondary and 0x00FFFFFF) or (0x88 shl 24)
            subTextPaint.typeface = Typeface.DEFAULT
            subTextPaint.textSize = 11f * density
            canvas.drawText("DISABLED", paddingX, 30f * density, subTextPaint)
        }

        // 4. Main Time (Center Left)
        timePaint.color = mainColor
        timePaint.textSize = 54f * density
        is24h = android.text.format.DateFormat.is24HourFormat(androidContext)
        val timeStr = TimeFormatUtil.formatClock(alarm.hour, alarm.minute, is24h)
        canvas.drawText(timeStr, paddingX, 86f * density, timePaint)

        // 5. Label & Days (Bottom Left)
        val daysText = buildDaysText()
        val label = alarm.label ?: "Alarm"
        val combinedText = "$label  •  $daysText"
        
        subTextPaint.color = if (isEnabled) tokens.textPrimary else tokens.textSecondary
        subTextPaint.typeface = Typeface.DEFAULT_BOLD
        subTextPaint.textSize = 13f * density
        canvas.drawText(combinedText, paddingX, 119f * density, subTextPaint)

        // 6. Toggle (Right side)
        val toggleX = width - hMargin - 30f * density
        val toggleY = height / 2f
        val tw = 44f * density
        val th = 24f * density
        
        bgPaint.color = if (isEnabled) tokens.accent else (tokens.textSecondary and 0x00FFFFFF) or 0x44000000
        canvas.drawRoundRect(toggleX - tw/2, toggleY - th/2, toggleX + tw/2, toggleY + th/2, th/2, th/2, bgPaint)
        
        bgPaint.color = tokens.background
        val thumbR = 9f * density
        val thumbX = toggleX + (if (isEnabled) (tw/2 - thumbR - 3f * density) else -(tw/2 - thumbR - 3f * density))
        canvas.drawCircle(thumbX, toggleY, thumbR, bgPaint)
        
        // Touch hitbox for toggle
        toggleRect.set(width - hMargin - 60f * density, 0f, width, height)
    }

    private fun buildDaysText(): String {
        if (!alarm.isRepeating) return "One time"
        if (alarm.repeatDays == 127) return "Everyday"
        if (alarm.repeatDays == 31)  return "Weekdays"
        if (alarm.repeatDays == 96)  return "Weekends"
        
        val sb = StringBuilder()
        val labels = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        for (i in 0..6) {
            if (alarm.isDayEnabled(i)) {
                if (sb.isNotEmpty()) sb.append(", ")
                sb.append(labels[i])
            }
        }
        return sb.toString()
    }

    override fun onClick(x: Float, y: Float) {
        if (toggleRect.contains(x, y)) {
            onToggle(!alarm.isEnabled)
        } else {
            onClickAction()
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
        
        val timeStr = TimeFormatUtil.formatClock(alarm.hour, alarm.minute, is24h)
        val daysText = buildDaysText()
        val label = alarm.label ?: "Alarm"
        val status = if (alarm.isEnabled) "Enabled" else "Disabled"
        val fullDescription = "$label, $timeStr, $daysText, $status"

        // Entire card
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = ((id % 1000).toInt() shl 1),
                bounds = RectF(listBounds.left, Math.max(listBounds.top, absoluteTop), listBounds.right, Math.min(listBounds.bottom, absoluteBottom)),
                label = fullDescription,
                className = "android.widget.FrameLayout"
            )
        )
        
        // Toggle area
        val toggleLeft = listBounds.right - 80f * density
        val toggleAbsRect = RectF(
            toggleLeft,
            Math.max(listBounds.top, absoluteTop),
            listBounds.right,
            Math.min(listBounds.bottom, absoluteBottom)
        )
        
        items.add(
            com.timely.msminutes.ui.canvas.CanvasRenderer.AccessibilityItem(
                id = ((id % 1000).toInt() shl 1) or 1,
                bounds = toggleAbsRect,
                label = if (alarm.isEnabled) "Turn Off" else "Turn On",
                className = "android.widget.Switch",
                selected = alarm.isEnabled
            )
        )
    }
}
