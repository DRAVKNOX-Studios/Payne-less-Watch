package com.timely.msminutes.ui.canvas

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import com.timely.msminutes.util.ThemeTokens

/**
 * A minimalist glowing comic-book style speech bubble.
 * Transparent background, only border and text with neon effect.
 */
class ComicBubbleRenderer(
    context: Context,
    private val text: String
) : CanvasRenderer {
    override val bounds = RectF()
    private val density = context.resources.displayMetrics.density
    
    var isVisible = false

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create("sans-serif-condensed", Typeface.BOLD)
        letterSpacing = 0.05f
    }
    
    private val bubblePath = Path()

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        if (!isVisible || bounds.isEmpty) return

        val centerX = bounds.centerX()
        val centerY = bounds.centerY()
        
        // Target point for Miss Minutes face (offset by Toolbar height)
        // Toolbar is 56dp, face is at screen center.
        val faceY = centerY - 38f * density 

        textPaint.textSize = 20f * density
        
        val paddingH = 32f * density
        val textWidth = textPaint.measureText(text.uppercase())
        val bubbleWidth = (textWidth + paddingH * 2).coerceAtLeast(160f * density)
        val bubbleHeight = 60f * density
        
        val bLeft = centerX - bubbleWidth / 2f
        val bTop = faceY - 200f * density
        val bRight = bLeft + bubbleWidth
        val bBottom = bTop + bubbleHeight
        
        val r = 20f * density
        val tailW = 16f * density
        val tailBaseX = centerX - tailW / 2f
        val targetX = centerX 
        val targetY = faceY - 65f * density // Stops exactly above the face radius (60dp)

        // 1. Build a Unified Path
        bubblePath.reset()
        bubblePath.moveTo(bLeft + r, bTop)
        bubblePath.lineTo(bRight - r, bTop)
        bubblePath.quadTo(bRight, bTop, bRight, bTop + r)
        bubblePath.lineTo(bRight, bBottom - r)
        bubblePath.quadTo(bRight, bBottom, bRight - r, bBottom)
        
        bubblePath.lineTo(tailBaseX + tailW, bBottom)
        bubblePath.lineTo(targetX, targetY)
        bubblePath.lineTo(tailBaseX, bBottom)
        
        bubblePath.lineTo(bLeft + r, bBottom)
        bubblePath.quadTo(bLeft, bBottom, bLeft, bBottom - r)
        bubblePath.lineTo(bLeft, bTop + r)
        bubblePath.quadTo(bLeft, bTop, bLeft + r, bTop)
        bubblePath.close()

        // 2. Draw Neon Outline (Pulsing - Sync with Background)
        val pulse = (Math.sin(System.currentTimeMillis() / 2000.0) * 0.5 + 0.5).toFloat()
        val effectAlpha = (0.3f + 0.3f * pulse) // Match background intensity
        
        borderPaint.color = tokens.accent
        borderPaint.alpha = (255 * effectAlpha).toInt()
        borderPaint.strokeWidth = 1.5f * density
        borderPaint.setShadowLayer(
            (12f * pulse) * density, 
            0f, 0f, 
            tokens.accent
        )
        canvas.drawPath(bubblePath, borderPaint)
        borderPaint.clearShadowLayer()
        
        // 3. Draw Text (White with a matching glow)
        textPaint.color = Color.WHITE
        textPaint.alpha = 255
        textPaint.setShadowLayer(8f * density * pulse, 0f, 0f, tokens.accent)
        val textY = bTop + bubbleHeight / 2f - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(text.uppercase(), centerX, textY, textPaint)
        textPaint.clearShadowLayer()
    }

    override fun onPopulateAccessibilityItems(items: MutableList<CanvasRenderer.AccessibilityItem>) {
        if (isVisible && !bounds.isEmpty) {
            items.add(
                CanvasRenderer.AccessibilityItem(
                    id = 0,
                    bounds = bounds,
                    label = text,
                    className = "android.widget.TextView"
                )
            )
        }
    }
}
