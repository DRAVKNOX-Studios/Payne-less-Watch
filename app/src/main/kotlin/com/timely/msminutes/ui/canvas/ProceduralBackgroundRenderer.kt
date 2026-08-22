package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.timely.msminutes.ui.canvas.items.BaseItemRenderer
import com.timely.msminutes.util.ThemeTokens
import java.util.Random

/**
 * Renders procedural background formations (Expanding "Sacred Timeline" rings) from the center.
 * Features a "Loki-esque" aesthetic with branching temporal lines and a faint Miss Minutes silhouette.
 */
class ProceduralBackgroundRenderer(context: Context) : CanvasRenderer {
    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    private val startTime = System.currentTimeMillis()
    private val ringCount = 3
    private val duration = 8000L // Slower, more "temporal" feel
    private val random = Random(42) // Stable seed for procedural look
    
    private val branchPath = Path()

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (bounds.isEmpty) return

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        val maxRadius = Math.max(bounds.width(), bounds.height()) * 0.8f
        
        val currentTime = System.currentTimeMillis()
        val elapsed = (currentTime - startTime) % duration
        
        BaseItemRenderer.resetPaints(density)
        val paint = BaseItemRenderer.strokePaint
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        
        // 1. Draw "Sacred Timeline" Rings & Branches
        for (i in 0 until ringCount) {
            val ringOffset = (i.toFloat() / ringCount) * duration
            val progress = ((elapsed + ringOffset) % duration) / duration
            
            val radius = maxRadius * progress
            val alpha = (255 * (1f - progress) * 0.4f).toInt() // Increased to 40% max
            
            if (alpha > 0) {
                paint.color = tokens.accent
                paint.alpha = alpha
                paint.strokeWidth = 1.5f * density // Slightly thicker
                paint.setShadowLayer(10f * density, 0f, 0f, tokens.accent) // Add neon glow
                
                // Main Timeline Ring
                canvas.drawCircle(centerX, centerY, radius, paint)
                
                // Branching "Nexus" events
                drawBranches(canvas, centerX, centerY, radius, alpha, tokens.accent)
                paint.clearShadowLayer()
            }
        }

        // 2. Draw Faint Miss Minutes Silhouette (Easter Egg)
        drawMissMinutes(canvas, centerX, centerY, tokens)
    }

    private fun drawBranches(canvas: Canvas, cx: Float, cy: Float, radius: Float, alpha: Int, color: Int) {
        val paint = BaseItemRenderer.strokePaint
        paint.color = color
        paint.alpha = alpha / 2
        paint.strokeWidth = 0.8f * density
        
        // Add 4-6 random branches per ring
        val branchCount = 5
        for (b in 0 until branchCount) {
            val angle = (b * (360f / branchCount) + (radius / 10f)) % 360f
            val angleRad = Math.toRadians(angle.toDouble())
            
            val startX = cx + Math.cos(angleRad).toFloat() * radius
            val startY = cy + Math.sin(angleRad).toFloat() * radius
            
            branchPath.reset()
            branchPath.moveTo(startX, startY)
            
            // Curve outwards and sideways
            val branchLen = 40f * density
            val cp1X = startX + Math.cos(angleRad + 0.2).toFloat() * (branchLen * 0.5f)
            val cp1Y = startY + Math.sin(angleRad + 0.2).toFloat() * (branchLen * 0.5f)
            val endX = startX + Math.cos(angleRad + 0.5).toFloat() * branchLen
            val endY = startY + Math.sin(angleRad + 0.5).toFloat() * branchLen
            
            branchPath.quadTo(cp1X, cp1Y, endX, endY)
            canvas.drawPath(branchPath, paint)
        }
    }

    private fun drawMissMinutes(canvas: Canvas, cx: Float, cy: Float, tokens: ThemeTokens) {
        val paint = BaseItemRenderer.strokePaint
        paint.style = Paint.Style.STROKE
        paint.color = tokens.accent
        
        // Pulsing glow for Miss Minutes
        val pulse = (Math.sin(System.currentTimeMillis() / 2000.0) * 0.5 + 0.5).toFloat()
        paint.alpha = (255 * (0.2f + 0.2f * pulse)).toInt() // Range 20-40%
        paint.setShadowLayer(12f * density * pulse, 0f, 0f, tokens.accent)
        
        val faceRadius = 60f * density
        canvas.drawCircle(cx, cy, faceRadius, paint)
        
        // Eyes
        val eyeOffset = 20f * density
        val eyeRadius = 6f * density
        paint.style = Paint.Style.FILL
        canvas.drawCircle(cx - eyeOffset, cy - eyeOffset, eyeRadius, paint)
        canvas.drawCircle(cx + eyeOffset, cy - eyeOffset, eyeRadius, paint)
        
        // Smile
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.5f * density // Thicker smile
        val smileRect = RectF(cx - 30f * density, cy - 10f * density, cx + 30f * density, cy + 30f * density)
        canvas.drawArc(smileRect, 20f, 140f, false, paint)
        paint.clearShadowLayer()
    }

    override fun isAnimating(): Boolean = true

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        bounds.set(left, top, right, bottom)
    }
}
