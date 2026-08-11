package com.timely.msminutes.service

import android.graphics.Canvas
import android.graphics.RectF
import android.service.dreams.DreamService
import android.view.ViewGroup
import com.timely.msminutes.ui.canvas.AnalogClockRenderer
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasRenderer
import com.timely.msminutes.util.ThemeTokens

class AlarmDreamService : DreamService() {

    private lateinit var hostView: CanvasHostView
    private lateinit var clockRenderer: DreamClockRenderer

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        
        isInteractive = false
        isFullscreen = true
        
        hostView = CanvasHostView(this)
        hostView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        clockRenderer = DreamClockRenderer(resources.displayMetrics.density)
        hostView.addRenderer(clockRenderer)
        
        setContentView(hostView)
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        clockRenderer.startAnimating()
    }

    override fun onDreamingStopped() {
        super.onDreamingStopped()
        clockRenderer.stopAnimating()
    }

    private class DreamClockRenderer(private val density: Float) : CanvasRenderer {
        override val bounds = RectF()
        private val renderer = AnalogClockRenderer(density)
        private var isAnimating = false

        fun startAnimating() {
            isAnimating = true
        }

        fun stopAnimating() {
            isAnimating = false
        }

        override fun isAnimating(): Boolean = isAnimating

        override fun draw(canvas: Canvas, tokens: ThemeTokens) {
            val size = Math.min(bounds.width(), bounds.height()) * 0.8f
            val x = bounds.centerX() - size / 2f
            val y = bounds.centerY() - size / 2f
            renderer.draw(canvas, x, y, size, tokens)
        }
    }
}
