package com.timely.msminutes.ui.canvas.items

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import com.timely.msminutes.util.TimeZoneUtil
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class WorldClockItemRenderer(
    private val context: android.content.Context,
    private val timeZoneId: String,
    private val is24Hour: Boolean,
    private val onDelete: () -> Unit
) : ItemRenderer {

    override val id: Long get() = timeZoneId.hashCode().toLong()
    private val density = context.resources.displayMetrics.density
    override var top: Float = 0f
    override var left: Float = 0f
    override var width: Float = 0f
    override var height: Float = 105f * density
    override var swipeX: Float = 0f
    override val isSwipeable: Boolean = true

    private val cardRect = RectF()
    private val deleteColor = Color.parseColor("#E53935")

    override fun drawBackground(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        if (swipeX > 0f) {
            BaseItemRenderer.resetPaints(density)
            val bgPaint = BaseItemRenderer.bgPaint
            val subTextPaint = BaseItemRenderer.subTextPaint

            val r = 24f * density
            val hMargin = 14f * density
            cardRect.set(hMargin, 8f * density, width - hMargin, height - 8f * density)
            
            bgPaint.color = deleteColor
            canvas.drawRoundRect(cardRect, r, r, bgPaint)
            
            subTextPaint.color = Color.WHITE
            subTextPaint.typeface = Typeface.DEFAULT_BOLD
            subTextPaint.textSize = 15f * density
            canvas.drawText("DELETE", hMargin + 18f * density, height / 2f + 6f * density, subTextPaint)
        }
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        BaseItemRenderer.resetPaints(density)
        val bgPaint = BaseItemRenderer.bgPaint
        val strokePaint = BaseItemRenderer.strokePaint
        val accentLinePaint = BaseItemRenderer.accentLinePaint
        val subTextPaint = BaseItemRenderer.subTextPaint
        val timePaint = BaseItemRenderer.timePaint
        val textPaint = BaseItemRenderer.textPaint // used for city name

        val r = 24f * density
        val hMargin = 14f * density
        cardRect.set(hMargin, 8f * density, width - hMargin, height - 8f * density)

        // 1. Background
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(cardRect, r, r, bgPaint)
        
        // 2. Premium outline & Accent strip
        strokePaint.color = (tokens.textPrimary and 0x00FFFFFF) or 0x1A000000
        canvas.drawRoundRect(cardRect, r, r, strokePaint)

        accentLinePaint.color = tokens.accent
        val stripWidth = 4f * density
        val stripX = hMargin + 12f * density
        canvas.drawRoundRect(stripX, cardRect.top + 15f * density, stripX + stripWidth, cardRect.bottom - 15f * density, stripWidth/2, stripWidth/2, accentLinePaint)

        val tz = TimeZone.getTimeZone(timeZoneId)
        val calendar = Calendar.getInstance(tz)
        val paddingX = hMargin + 22f * density

        // 3. City name
        textPaint.color = tokens.textPrimary
        textPaint.textSize = 19f * density
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val cityName = TimeZoneUtil.getCityName(timeZoneId)
        canvas.drawText(cityName, paddingX, 45f * density, textPaint)

        // 4. Country/Region & Offset
        subTextPaint.color = tokens.textSecondary
        subTextPaint.typeface = Typeface.DEFAULT_BOLD
        subTextPaint.textSize = 13f * density
        val offset = tz.getOffset(System.currentTimeMillis()) / 3600000f
        val offsetStr = "GMT ${if (offset >= 0) "+" else ""}${String.format(Locale.US, "%.1f", offset)}"
        val country = TimeZoneUtil.getCountryName(timeZoneId)
        canvas.drawText("$country • $offsetStr", paddingX, 75f * density, subTextPaint)

        // 5. Time
        timePaint.color = tokens.textPrimary
        timePaint.textSize = 36f * density
        timePaint.textAlign = android.graphics.Paint.Align.RIGHT
        val timeStr = TimeFormatUtil.formatClock(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), is24Hour)
        canvas.drawText(timeStr, width - hMargin - 20f * density, 65f * density, timePaint)
    }

    override fun onClick(x: Float, y: Float) {}

    override fun onDelete() {
        onDelete.invoke()
    }
}
