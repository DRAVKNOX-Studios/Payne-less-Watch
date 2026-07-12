package com.timely.msminutes.ui.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.NumberPicker
import kotlin.math.abs

/**
 * Paints the center (selected) wheel item at 100 % alpha and all others at
 * 45 % alpha — every frame, including mid-scroll.
 * 
 * Strategy: wrap the Canvas given to super.onDraw() with a proxy that
 * intercepts every drawText() call. The y-coordinate passed by AOSP already
 * includes the picker’s live scroll offset, so animation is unaffected.
 * Items whose text midpoint falls inside the centre band get fullColor;
 * everything else gets fadedColor.
 */
class ThemedNumberPicker : NumberPicker {
    private var fullColor = Color.BLACK
    private var fadedColor = Color.argb(115, 0, 0, 0)
    private var halfBand = 0f
    private var interceptor: InterceptCanvas? = null

    constructor(context: Context?) : super(context)

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)

    fun setDigitColor(color: Int) {
        fullColor = color
        fadedColor = Color.argb(
            Math.round(Color.alpha(color) * 0.45f),
            Color.red(color),
            Color.green(color),
            Color.blue(color)
        )
        interceptor?.updateColors(fullColor, fadedColor)
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        halfBand = h / 4f
        interceptor = null // recreate on next draw to update dimensions
    }

    override fun onDraw(canvas: Canvas) {
        val i = interceptor ?: InterceptCanvas(
            height / 2f, halfBand,
            fullColor, fadedColor
        ).also { interceptor = it }
        
        i.setTarget(canvas)
        super.onDraw(i)
        i.clearTarget()
    }

    // ------------------------------------------------------------------
    private class InterceptCanvas(
        private val cy: Float, private val halfBand: Float,
        private var fullColor: Int, private var fadedColor: Int
    ) : Canvas() {
        private var target: Canvas? = null

        fun setTarget(c: Canvas) { target = c }
        fun clearTarget() { target = null }
        
        fun updateColors(full: Int, faded: Int) {
            fullColor = full
            fadedColor = faded
        }
        
        // --- minimal delegation for non-text draw calls ---
        override fun save(): Int = target?.save() ?: 0
        override fun restore() { target?.restore() }
        override fun restoreToCount(s: Int) { target?.restoreToCount(s) }
        override fun getSaveCount(): Int = target?.getSaveCount() ?: 0
        override fun translate(dx: Float, dy: Float) { target?.translate(dx, dy) }
        override fun scale(sx: Float, sy: Float) { target?.scale(sx, sy) }
        override fun drawLine(x0: Float, y0: Float, x1: Float, y1: Float, p: Paint) { target?.drawLine(x0, y0, x1, y1, p) }
        override fun drawRect(l: Float, t: Float, r: Float, b: Float, p: Paint) { target?.drawRect(l, t, r, b, p) }
        override fun drawBitmap(bm: Bitmap, l: Float, t: Float, p: Paint?) { target?.drawBitmap(bm, l, t, p) }

        // --- text intercepts (color swap) ---
        override fun drawText(text: String, x: Float, y: Float, paint: Paint) {
            val t = target ?: return
            paint.setColor(if (isCenter(y, paint)) fullColor else fadedColor)
            t.drawText(text, x, y, paint)
        }

        override fun drawText(
            text: CharSequence, start: Int, end: Int,
            x: Float, y: Float, paint: Paint
        ) {
            val t = target ?: return
            paint.setColor(if (isCenter(y, paint)) fullColor else fadedColor)
            t.drawText(text, start, end, x, y, paint)
        }

        override fun drawText(
            text: CharArray, index: Int, count: Int,
            x: Float, y: Float, paint: Paint
        ) {
            val t = target ?: return
            paint.setColor(if (isCenter(y, paint)) fullColor else fadedColor)
            t.drawText(text, index, count, x, y, paint)
        }

        override fun drawText(
            text: String, start: Int, end: Int,
            x: Float, y: Float, paint: Paint
        ) {
            val t = target ?: return
            paint.setColor(if (isCenter(y, paint)) fullColor else fadedColor)
            t.drawText(text, start, end, x, y, paint)
        }

        private fun isCenter(y: Float, paint: Paint): Boolean {
            if (halfBand <= 0f) return false
            val fm = paint.getFontMetrics()
            val mid = y - (fm.ascent + fm.descent) / 2f
            return abs(mid - cy) <= halfBand
        }
    }
}
