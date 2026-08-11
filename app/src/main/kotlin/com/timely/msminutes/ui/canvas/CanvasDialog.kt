package com.timely.msminutes.ui.canvas

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Window
import com.timely.msminutes.util.ThemeStore

/**
 * A custom Dialog that uses CanvasHostView to render all its content.
 */
class CanvasDialog(
    context: Context,
    private val contentBuilder: (CanvasDialog, CanvasListView) -> Unit
) : Dialog(context) {

    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        hostView = CanvasHostView(context)
        // Initial layout params to ensure it's not 0x0
        hostView.layoutParams = android.view.ViewGroup.LayoutParams(
            (screenW * 0.85f).toInt(),
            (screenH * 0.8f).toInt()
        )

        listView = CanvasListView(context, hostView) {}
        hostView.addRenderer(listView)

        // Pre-build content so it's ready for the first layout/draw
        contentBuilder(this, listView)

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = (right - left).toFloat()
            val h = (bottom - top).toFloat()
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            
            listView.onLayout(0f, 0f, w, h)
            
            // Re-run builder in case it depends on layout bounds
            contentBuilder(this, listView)
            
            val contentH = listView.getContentHeight()
            if (contentH > 0) {
                val maxAllowedH = screenH * 0.85f
                val finalH = (contentH + 24f * context.resources.displayMetrics.density).coerceAtMost(maxAllowedH)
                
                val currentH = window?.attributes?.height ?: 0
                if (Math.abs(currentH - finalH.toInt()) > 10) {
                    window?.setLayout((screenW * 0.85f).toInt(), finalH.toInt())
                }
            }
            hostView.invalidate()
        }

        setContentView(hostView)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        // Force initial window size so it appears immediately
        window?.setLayout((screenW * 0.85f).toInt(), (screenH * 0.8f).toInt())
        
        applyTheme()
    }

    private fun applyTheme() {
        val t = ThemeStore.get().current() ?: return
        val shape = GradientDrawable().apply {
            setColor(t.surface)
            cornerRadius = 28f * context.resources.displayMetrics.density
        }
        window?.setBackgroundDrawable(shape)
    }

    fun reload() {
        contentBuilder(this, listView)
        hostView.invalidate()
    }
}
