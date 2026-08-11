package com.timely.msminutes.ui.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.util.LruCache
import com.timely.msminutes.ui.canvas.items.BaseItemRenderer
import com.timely.msminutes.util.ThemeTokens
import java.util.Calendar
import java.util.Random
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class AnalogClockRenderer(private val density: Float) {

    companion object {
        // Shared cache for clock faces across all activities/instances
        private val faceCache = LruCache<String, Bitmap>(4)
    }

    private val random = Random()
    private var lastEyeUpdate = 0L
    
    // Smooth pupil movement
    private var targetPupilX1 = 0f
    private var targetPupilY1 = 0f
    private var targetPupilX2 = 0f
    private var targetPupilY2 = 0f
    private var currentPupilX1 = 0f
    private var currentPupilY1 = 0f
    private var currentPupilX2 = 0f
    private var currentPupilY2 = 0f

    fun draw(canvas: Canvas, x: Float, y: Float, size: Float, tokens: ThemeTokens) {
        val radius = size / 2f
        val centerX = x + radius
        val centerY = y + radius

        // 1-3. Static Face Caching (LRU)
        val cacheKey = "${size.toInt()}_${tokens.accent}_${tokens.surface}"
        var cachedFace = faceCache.get(cacheKey)
        if (cachedFace == null || cachedFace.isRecycled) {
            cachedFace = createFaceBitmap(size, tokens)
            faceCache.put(cacheKey, cachedFace)
        }
        canvas.drawBitmap(cachedFace, x, y, null)

        // 4. Draw Hands
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR)
        val minute = calendar.get(Calendar.MINUTE)

        BaseItemRenderer.resetPaints(density)
        val strokePaint = BaseItemRenderer.strokePaint
        val bgPaint = BaseItemRenderer.bgPaint

        // Hour hand
        val hourAngle = (hour * 30 + minute / 2f - 90).toDouble().toRadians()
        val hourLength = radius * 0.55f
        strokePaint.color = tokens.textPrimary
        strokePaint.strokeWidth = 3.5f * density
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawLine(
            centerX, centerY,
            centerX + cos(hourAngle).toFloat() * hourLength,
            centerY + sin(hourAngle).toFloat() * hourLength,
            strokePaint
        )

        // Minute hand
        val minAngle = (minute * 6 - 90).toDouble().toRadians()
        val minLength = radius * 0.75f
        strokePaint.strokeWidth = 2f * density
        canvas.drawLine(
            centerX, centerY,
            centerX + cos(minAngle).toFloat() * minLength,
            centerY + sin(minAngle).toFloat() * minLength,
            strokePaint
        )
        
        // Center hub
        bgPaint.color = tokens.accent
        canvas.drawCircle(centerX, centerY, 3f * density, bgPaint)

        // 5. Draw Googly Eyes
        val eyeRadius = size / 10f
        val eyeSpacing = size / 4f
        val eyeY = centerY - size / 4.5f
        
        // Update target eye positions occasionally
        val now = System.currentTimeMillis()
        if (now - lastEyeUpdate > 1500 + random.nextInt(3000)) {
            val maxOffset = size / 15f
            targetPupilX1 = (random.nextFloat() * 2f - 1f) * maxOffset
            targetPupilY1 = -random.nextFloat() * maxOffset
            targetPupilX2 = (random.nextFloat() * 2f - 1f) * maxOffset
            targetPupilY2 = -random.nextFloat() * maxOffset
            lastEyeUpdate = now
        }

        // Interpolate pupil positions for smoothness
        val lerpFactor = 0.05f
        currentPupilX1 += (targetPupilX1 - currentPupilX1) * lerpFactor
        currentPupilY1 += (targetPupilY1 - currentPupilY1) * lerpFactor
        currentPupilX2 += (targetPupilX2 - currentPupilX2) * lerpFactor
        currentPupilY2 += (targetPupilY2 - currentPupilY2) * lerpFactor

        drawEye(canvas, centerX - eyeSpacing / 2f, eyeY, eyeRadius, currentPupilX1, currentPupilY1, tokens)
        drawEye(canvas, centerX + eyeSpacing / 2f, eyeY, eyeRadius, currentPupilX2, currentPupilY2, tokens)
    }

    private fun drawEye(canvas: Canvas, cx: Float, cy: Float, radius: Float, px: Float, py: Float, tokens: ThemeTokens) {
        BaseItemRenderer.resetPaints(density)
        val strokePaint = BaseItemRenderer.strokePaint
        val bgPaint = BaseItemRenderer.bgPaint

        // Bolder cartoonish border
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 1.5f * density
        strokePaint.color = tokens.accent
        canvas.drawCircle(cx, cy, radius, strokePaint)

        // Cartoonish iris (now accent colored and smaller)
        bgPaint.color = tokens.accent
        val pupilRadius = radius * 0.4f
        canvas.drawCircle(cx + px, cy + py, pupilRadius, bgPaint)
    }

    private fun createFaceBitmap(size: Float, tokens: ThemeTokens): Bitmap {
        val radius = size / 2f
        val centerX = radius
        val centerY = radius
        
        val bitmap = Bitmap.createBitmap(size.toInt().coerceAtLeast(1), size.toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
        val faceCanvas = Canvas(bitmap)

        BaseItemRenderer.resetPaints(density)
        val bgPaint = BaseItemRenderer.bgPaint
        val strokePaint = BaseItemRenderer.strokePaint

        // 1. Face Background (Subtle)
        bgPaint.color = (tokens.accent and 0x00FFFFFF) or (0x1A shl 24) // ~10% opacity
        faceCanvas.drawCircle(centerX, centerY, radius, bgPaint)

        // 2. Hour Ticks
        strokePaint.color = tokens.accent
        strokePaint.strokeWidth = 1.5f * density
        strokePaint.strokeCap = Paint.Cap.ROUND
        for (i in 0 until 12) {
            val angle = (i * 30).toDouble().toRadians()
            val startR = radius * 0.85f
            val endR = radius * 0.95f
            faceCanvas.drawLine(
                centerX + cos(angle).toFloat() * startR,
                centerY + sin(angle).toFloat() * startR,
                centerX + cos(angle).toFloat() * endR,
                centerY + sin(angle).toFloat() * endR,
                strokePaint
            )
        }

        // 3. Circular border
        strokePaint.style = Paint.Style.STROKE
        strokePaint.strokeWidth = 2.5f * density
        faceCanvas.drawCircle(centerX, centerY, radius - strokePaint.strokeWidth / 2f, strokePaint)

        return bitmap
    }

    private fun Double.toRadians() = this * PI / 180.0
}
