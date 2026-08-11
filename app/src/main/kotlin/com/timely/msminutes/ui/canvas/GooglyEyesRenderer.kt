package com.timely.msminutes.ui.canvas

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.animation.OvershootInterpolator
import com.timely.msminutes.util.ThemeTokens
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Ported GooglyEyes logic for CanvasRenderer.
 */
class GooglyEyesRenderer(private val density: Float) : CanvasRenderer {
    override val bounds = RectF()
    
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }

    private class EyeState {
        var curX: Float = 0f;  var curY: Float = 0f
        var fromX: Float = 0f; var fromY: Float = 0f
        var toX: Float = 0f;   var toY: Float = 0f
        var animator: ValueAnimator? = null
        var pendingRunnable: Runnable? = null
    }

    private val left = EyeState()
    private val right = EyeState()
    
    private var eyeRadius = 0f
    private var pupilRadius = 0f
    private var spacing = 0f
    private val maxOffsetDp = 22f

    init {
        scheduleNextMove(left, 0L)
        scheduleNextMove(right, Random.nextLong(300, 800))
    }

    private fun scheduleNextMove(eye: EyeState, delayMs: Long) {
        val r = Runnable { animateEye(eye) }
        eye.pendingRunnable = r
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(r, delayMs)
    }

    private fun animateEye(eye: EyeState) {
        eye.animator?.cancel()

        val angle = Random.nextDouble(0.0, Math.PI * 2).toFloat()
        val dist = Random.nextFloat() * maxOffsetDp
        eye.fromX = eye.curX;  eye.fromY = eye.curY
        eye.toX = cos(angle) * dist
        eye.toY = sin(angle) * dist

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = Random.nextLong(350, 650)
            interpolator = OvershootInterpolator(0.6f)
            addUpdateListener { va ->
                val f = va.animatedValue as Float
                eye.curX = eye.fromX + (eye.toX - eye.fromX) * f
                eye.curY = eye.fromY + (eye.toY - eye.fromY) * f
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    val d = Math.hypot(eye.curX.toDouble(), eye.curY.toDouble()).toFloat()
                    if (d > maxOffsetDp) {
                        val scale = maxOffsetDp / d
                        eye.curX *= scale; eye.toX *= scale
                        eye.curY *= scale; eye.toY *= scale
                    }
                    scheduleNextMove(eye, Random.nextLong(800, 2800))
                }
            })
        }
        eye.animator = anim
        anim.start()
    }

    override fun onLayout(left: Float, top: Float, right: Float, bottom: Float) {
        super.onLayout(left, top, right, bottom)
        eyeRadius = min(bounds.width(), bounds.height()) / 4.5f
        pupilRadius = eyeRadius / 2.5f
        spacing = eyeRadius * 1.25f
    }

    override fun draw(canvas: Canvas, tokens: ThemeTokens) {
        // Use standard cartoon look as in original: White eye, Black pupil, Black outline.
        // The user says "add the original googly eyes from XML".
        // Original XML code: whitePaint = Color.WHITE, pupilPaint = Color.BLACK, outlinePaint = Color.BLACK.
        
        val cx = bounds.centerX()
        val cy = bounds.centerY()
        
        drawEye(canvas, cx - spacing, cy, left)
        drawEye(canvas, cx + spacing, cy, right)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, eye: EyeState) {
        canvas.drawCircle(x, y, eyeRadius, whitePaint)
        canvas.drawCircle(x, y, eyeRadius, outlinePaint)
        canvas.drawCircle(x + eye.curX * density, y + eye.curY * density, pupilRadius, pupilPaint)
    }

    override fun isAnimating(): Boolean = true
}
