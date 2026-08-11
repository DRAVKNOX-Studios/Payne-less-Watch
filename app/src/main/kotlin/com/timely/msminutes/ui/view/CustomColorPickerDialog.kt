package com.timely.msminutes.ui.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Window
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.ColorPickerItemRenderer
import com.timely.msminutes.ui.canvas.items.HeaderItemRenderer
import com.timely.msminutes.ui.canvas.items.InlineEditItemRenderer
import com.timely.msminutes.ui.canvas.items.SliderItemRenderer
import com.timely.msminutes.util.ThemeStore

class CustomColorPickerDialog(
    context: Context,
    private var currentColor: Int,
    private val listener: OnColorSelectedListener
) : Dialog(context) {
    fun interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private var hexItem: InlineEditItemRenderer? = null
    private var redItem: SliderItemRenderer? = null
    private var greenItem: SliderItemRenderer? = null
    private var blueItem: SliderItemRenderer? = null
    private var previewItem: ColorPickerItemRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels

        hostView = CanvasHostView(context)
        hostView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        listView = CanvasListView(context, hostView) {}
        hostView.addRenderer(listView)

        // Initialize items immediately so they are ready for the first frame
        setupItems()

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = (right - left).toFloat()
            val h = (bottom - top).toFloat()
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            
            listView.onLayout(0f, 0f, w, h)
            
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
        
        // Initial window size
        window?.setLayout((screenW * 0.85f).toInt(), (screenH * 0.7f).toInt())

        val t = ThemeStore.get().current()
        if (t != null) {
            val shape = GradientDrawable().apply {
                setColor(t.surface)
                cornerRadius = 28f * context.resources.displayMetrics.density
            }
            window?.setBackgroundDrawable(shape)
        }
    }

    private fun setupItems() {
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()

        items.add(HeaderItemRenderer(context, "Custom Color"))
        
        previewItem = ColorPickerItemRenderer(context, "Preview", currentColor) {}
        items.add(previewItem!!)

        hexItem = InlineEditItemRenderer(context, hostView, listView, "Hex", String.format("#%06X", (0xFFFFFF and currentColor))) { hex ->
            try {
                if (hex.startsWith("#") && hex.length == 7) {
                    val newColor = Color.parseColor(hex)
                    if (newColor != currentColor) {
                        currentColor = newColor
                        updateComponents()
                    }
                }
            } catch (ignored: Exception) {}
        }
        items.add(hexItem!!)

        redItem = SliderItemRenderer(context, "Red", Color.red(currentColor)) { r ->
            currentColor = Color.rgb(r, Color.green(currentColor), Color.blue(currentColor))
            updateComponents()
        }
        items.add(redItem!!)

        greenItem = SliderItemRenderer(context, "Green", Color.green(currentColor)) { g ->
            currentColor = Color.rgb(Color.red(currentColor), g, Color.blue(currentColor))
            updateComponents()
        }
        items.add(greenItem!!)

        blueItem = SliderItemRenderer(context, "Blue", Color.blue(currentColor)) { b ->
            currentColor = Color.rgb(Color.red(currentColor), Color.green(currentColor), b)
            updateComponents()
        }
        items.add(blueItem!!)

        items.add(ButtonItemRenderer(context, "Select") {
            listener.onColorSelected(currentColor)
            dismiss()
        })

        items.add(ButtonItemRenderer(context, "Cancel", isDanger = true) {
            dismiss()
        })

        listView.setItems(items)
    }

    private fun updateComponents() {
        previewItem?.color = currentColor
        redItem?.value = Color.red(currentColor)
        greenItem?.value = Color.green(currentColor)
        blueItem?.value = Color.blue(currentColor)
        hostView.invalidate()
    }
}
