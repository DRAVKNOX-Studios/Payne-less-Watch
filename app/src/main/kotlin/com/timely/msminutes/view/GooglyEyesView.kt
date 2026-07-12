package com.timely.msminutes.view

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Two googly eyes with fully independent, randomly-timed pupil movement.
 * Each eye picks its own random direction and duration so they never look
 * like they are controlled by the same signal.  Pupils interpolate smoothly
 * with a slight overshoot instead of teleporting.
 */
class GooglyEyesView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ── paints ────────────────────────────────────────────────────────────
    private val whitePaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val pupilPaint   = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK }
    private val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
    }

    // ── per-eye state ──────────────────────────────────────────────────────
    private inner class EyeState {
        var curX: Float = 0f;  var curY: Float = 0f
        var fromX: Float = 0f; var fromY: Float = 0f
        var toX: Float = 0f;   var toY: Float = 0f
        var animator: ValueAnimator? = null

        // Runnable reference so we can cancel it individually via View.removeCallbacks()
        var pendingRunnable: Runnable? = null
    }

    private val left  = EyeState()
    private val right = EyeState()

    // ── layout helpers ────────────────────────────────────────────────────
    private var eyeRadius   = 0f
    private var pupilRadius = 0f
    private var spacing     = 0f
    private val d get() = resources.displayMetrics.density  // dp → px

    private val maxOffsetDp = 22f

    init {
        post {
            scheduleNextMove(left,  delayMs = 0L)
            scheduleNextMove(right, delayMs = Random.nextLong(300, 800))
        }
    }

    // ── movement scheduling ───────────────────────────────────────────────

    private fun scheduleNextMove(eye: EyeState, delayMs: Long) {
        // Cancel any previously queued runnable for this eye
        eye.pendingRunnable?.let { removeCallbacks(it) }
        val r = Runnable { animateEye(eye) }
        eye.pendingRunnable = r
        postDelayed(r, delayMs)
    }

    private fun animateEye(eye: EyeState) {
        eye.animator?.cancel()

        val angle = Random.nextDouble(0.0, Math.PI * 2).toFloat()
        val dist  = Random.nextFloat() * maxOffsetDp
        eye.fromX = eye.curX;  eye.fromY = eye.curY
        eye.toX   = cos(angle) * dist
        eye.toY   = sin(angle) * dist

        val duration = Random.nextLong(350, 650)

        val anim = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator  = OvershootInterpolator(0.6f)
            addUpdateListener { va ->
                val f     = va.animatedValue as Float
                eye.curX  = eye.fromX + (eye.toX - eye.fromX) * f
                eye.curY  = eye.fromY + (eye.toY - eye.fromY) * f
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    clampToBorder(eye)
                    scheduleNextMove(eye, Random.nextLong(800, 2800))
                }
            })
        }
        eye.animator = anim
        anim.start()
    }

    /** Keep the pupil centre inside the eye orbit (dp units). */
    private fun clampToBorder(eye: EyeState) {
        val dist = Math.hypot(eye.curX.toDouble(), eye.curY.toDouble()).toFloat()
        if (dist > maxOffsetDp) {
            val scale = maxOffsetDp / dist
            eye.curX *= scale;  eye.toX *= scale
            eye.curY *= scale;  eye.toY *= scale
        }
    }

    // ── drawing ────────────────────────────────────────────────────────────

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        eyeRadius   = min(w, h) / 4.5f
        pupilRadius = eyeRadius / 2.5f
        spacing     = eyeRadius * 1.25f
        outlinePaint.strokeWidth = 2f * d
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cx = width  / 2f
        val cy = height / 2f
        drawEye(canvas, cx - spacing, cy, left)
        drawEye(canvas, cx + spacing, cy, right)
    }

    private fun drawEye(canvas: Canvas, x: Float, y: Float, eye: EyeState) {
        canvas.drawCircle(x, y, eyeRadius, whitePaint)
        canvas.drawCircle(x, y, eyeRadius, outlinePaint)
        canvas.drawCircle(x + eye.curX * d, y + eye.curY * d, pupilRadius, pupilPaint)
    }

    // ── cleanup ────────────────────────────────────────────────────────────

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // Cancel animators
        left.animator?.cancel()
        right.animator?.cancel()
        // Cancel pending postDelayed runnables — View.removeCallbacks() is the correct API
        left.pendingRunnable?.let  { removeCallbacks(it) }
        right.pendingRunnable?.let { removeCallbacks(it) }
    }
}
