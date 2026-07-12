package com.timely.msminutes.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.util.LruCache
import android.view.View
import android.widget.RemoteViews
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sign

object GooglyEyesController {
    private val DIGIT_TEXT_IDS = intArrayOf(
        R.id.digit_text_1, R.id.digit_text_2, R.id.digit_text_3, R.id.digit_text_4
    )
    private val DIGIT_IMG_IDS = intArrayOf(
        R.id.digit_img_1, R.id.digit_img_2, R.id.digit_img_3, R.id.digit_img_4
    )
    private val DIGIT_PUPIL_IDS = intArrayOf(
        R.id.digit_pupil_1, R.id.digit_pupil_2, R.id.digit_pupil_3, R.id.digit_pupil_4
    )
    private val DIGIT_PUPIL_CONTAINER_IDS = intArrayOf(
        R.id.digit_pupil_container_1, R.id.digit_pupil_container_2,
        R.id.digit_pupil_container_3, R.id.digit_pupil_container_4
    )

    private val EYE_POSITIONS = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(-6f, 0f),
        floatArrayOf(6f, 0f),
        floatArrayOf(0f, -6f),
        floatArrayOf(0f, 6f),
        floatArrayOf(-4f, -4f),
        floatArrayOf(4f, -4f),
        floatArrayOf(-4f, 4f),
        floatArrayOf(4f, 4f),
    )

    private val currentPos = Array(4) { floatArrayOf(0f, 0f) }
    private val targetIdx = IntArray(4) { 0 }
    private val targetChangeTime = LongArray(4) { 0L }
    private val holdDurations = longArrayOf(2800L, 3500L, 2200L, 4000L)
    private val phaseOffsets = longArrayOf(0L, 1100L, 600L, 1800L)
    private const val INTERP_SPEED_DP_PER_MS = 0.025f
    private var initialised = false

    private val bitmapCache = LruCache<String, Bitmap>(20)

    private fun getCacheKey(text: String, textColor: Int, outlineColor: Int, wideEye: Boolean): String {
        return "$text|$textColor|$outlineColor|$wideEye"
    }

    private fun pickNextTarget(eyeIdx: Int): Int {
        var next: Int
        do {
            next = ((System.currentTimeMillis() / 1L + eyeIdx * 31L + phaseOffsets[eyeIdx]) % EYE_POSITIONS.size).toInt()
        } while (next == targetIdx[eyeIdx])
        return next
    }

    fun tick(now: Long) {
        if (!initialised) {
            for (i in 0..3) {
                targetIdx[i] = (i * 2 + 1) % EYE_POSITIONS.size
                targetChangeTime[i] = now - phaseOffsets[i]
            }
            initialised = true
        }

        for (i in 0..3) {
            val elapsed = now - targetChangeTime[i]
            if (elapsed >= holdDurations[i]) {
                val next = (targetIdx[i] + i + 3) % EYE_POSITIONS.size
                targetIdx[i] = if (next == targetIdx[i]) (next + 1) % EYE_POSITIONS.size else next
                targetChangeTime[i] = now
            }

            val target = EYE_POSITIONS[targetIdx[i]]
            val cur = currentPos[i]
            val maxStep = INTERP_SPEED_DP_PER_MS * 100f
            cur[0] = lerpStep(cur[0], target[0], maxStep)
            cur[1] = lerpStep(cur[1], target[1], maxStep)
        }
    }

    private fun lerpStep(current: Float, target: Float, maxStep: Float): Float {
        val diff = target - current
        return if (abs(diff) <= maxStep) target
        else current + sign(diff) * maxStep
    }

    fun reset() {
        initialised = false
        for (i in 0..3) {
            currentPos[i][0] = 0f
            currentPos[i][1] = 0f
            targetIdx[i] = 0
        }
        clearCache()
    }

    fun clearCache() {
        val snapshot = bitmapCache.snapshot()
        snapshot.values.forEach { if (!it.isRecycled) it.recycle() }
        bitmapCache.evictAll()
    }

    fun updateTime(
        context: Context,
        views: RemoteViews,
        is24h: Boolean,
        fontColor: Int,
        accentColor: Int,
        isTransparent: Boolean
    ) {
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val isEasterEggTime = is24h && hour == 0 && minute == 0

        val timeStr = if (is24h) {
            String.format("%02d%02d", hour, minute)
        } else {
            val hour12 = hour % 12
            val displayHour = if (hour12 == 0) 12 else hour12
            String.format("%2d%02d", displayHour, minute)
        }

        // AM/PM badge — only visible in 12h mode
        if (!is24h) {
            val amPm = if (hour < 12) "AM" else "PM"
            views.setViewVisibility(R.id.widget_am_pm, View.VISIBLE)
            views.setTextViewText(R.id.widget_am_pm, amPm)
            views.setTextColor(R.id.widget_am_pm, fontColor)
            if (isTransparent) {
                // Re-use the same shadow helper via reflection that ClockWidgetProvider uses
                // Instead, just set a slightly translucent version of fontColor as shadow via bitmap path.
                // We keep it simple: transparency mode already has the outline bitmap digits,
                // so just tint the AM/PM text with accentColor to match.
                views.setTextColor(R.id.widget_am_pm, accentColor)
            }
        } else {
            views.setViewVisibility(R.id.widget_am_pm, View.GONE)
        }

        var eyeIdx = 0
        for (i in 0..3) {
            val c = timeStr[i]

            if (isTransparent) {
                views.setViewVisibility(DIGIT_TEXT_IDS[i], View.GONE)
                views.setViewVisibility(DIGIT_IMG_IDS[i], View.VISIBLE)
                
                val key = getCacheKey(c.toString(), fontColor, accentColor, isEasterEggTime && c == '0')
                val cached = bitmapCache.get(key)
                val bitmap = if (cached != null && !cached.isRecycled) {
                    cached
                } else {
                    val newBmp = createDigitBitmap(context, c.toString(), fontColor, accentColor, isEasterEggTime && c == '0')
                    bitmapCache.put(key, newBmp)
                    newBmp
                }
                
                views.setImageViewBitmap(DIGIT_IMG_IDS[i], bitmap)
            } else {
                views.setViewVisibility(DIGIT_TEXT_IDS[i], View.VISIBLE)
                views.setViewVisibility(DIGIT_IMG_IDS[i], View.GONE)
                views.setTextViewText(DIGIT_TEXT_IDS[i], c.toString())
                views.setTextColor(DIGIT_TEXT_IDS[i], fontColor)
            }

            if (c == '0') {
                views.setViewVisibility(DIGIT_PUPIL_CONTAINER_IDS[i], View.VISIBLE)
                views.setInt(DIGIT_PUPIL_IDS[i], "setColorFilter", accentColor)

                if (isEasterEggTime) {
                    val density = context.resources.displayMetrics.density
                    val pos = currentPos[eyeIdx]
                    val BASE_PAD = 6f

                    val padL = ((BASE_PAD + pos[0]) * density).toInt().coerceAtLeast(0)
                    val padT = ((BASE_PAD + pos[1]) * density).toInt().coerceAtLeast(0)
                    val padR = ((BASE_PAD - pos[0]) * density).toInt().coerceAtLeast(0)
                    val padB = ((BASE_PAD - pos[1]) * density).toInt().coerceAtLeast(0)

                    views.setViewPadding(DIGIT_PUPIL_CONTAINER_IDS[i], padL, padT, padR, padB)
                    eyeIdx++
                } else {
                    views.setViewPadding(DIGIT_PUPIL_CONTAINER_IDS[i], 0, 0, 0, 0)
                }
            } else {
                views.setViewVisibility(DIGIT_PUPIL_CONTAINER_IDS[i], View.GONE)
            }
        }

        if (isTransparent) {
            views.setViewVisibility(R.id.widget_colon, View.GONE)
            views.setViewVisibility(R.id.widget_colon_img, View.VISIBLE)
            
            val key = getCacheKey(":", fontColor, accentColor, false)
            val cached = bitmapCache.get(key)
            val bitmap = if (cached != null && !cached.isRecycled) {
                cached
            } else {
                val newBmp = createDigitBitmap(context, ":", fontColor, accentColor, false)
                bitmapCache.put(key, newBmp)
                newBmp
            }
            
            views.setImageViewBitmap(R.id.widget_colon_img, bitmap)
        } else {
            views.setViewVisibility(R.id.widget_colon, View.VISIBLE)
            views.setViewVisibility(R.id.widget_colon_img, View.GONE)
            views.setTextColor(R.id.widget_colon, fontColor)
        }
    }

    private fun createDigitBitmap(
        context: Context,
        text: String,
        textColor: Int,
        outlineColor: Int,
        wideEye: Boolean
    ): Bitmap {
        val density = context.resources.displayMetrics.density
        val textSize = 72 * density

        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.textSize = textSize
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

        val scaleX = if (wideEye) 1.35f else 1.0f
        paint.textScaleX = scaleX

        val bounds = Rect()
        paint.getTextBounds(text, 0, text.length, bounds)

        val width = (paint.measureText(text) + 28 * density).toInt()
        val height = (textSize + 24 * density).toInt()

        val bitmap = Bitmap.createBitmap(max(1, width), max(1, height), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val x = (width - paint.measureText(text)) / 2f
        val y = (height + bounds.height()) / 2f - bounds.bottom

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 4 * density
        paint.color = outlineColor
        paint.setShadowLayer(10 * density, 0f, 0f, outlineColor)
        canvas.drawText(text, x, y, paint)

        paint.clearShadowLayer()
        paint.strokeWidth = 6 * density
        canvas.drawText(text, x, y, paint)

        paint.style = Paint.Style.FILL
        paint.color = textColor
        canvas.drawText(text, x, y, paint)

        return bitmap
    }

    fun isEasterEggMinute(context: Context): Boolean {
        val prefs = Prefs(context)
        val cal = Calendar.getInstance()
        return prefs.is24Hour() && cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0
    }
}
