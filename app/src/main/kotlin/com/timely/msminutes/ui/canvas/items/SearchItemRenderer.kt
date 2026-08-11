package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasIcons
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.drawSearch
import com.timely.msminutes.util.ThemeTokens

class SearchItemRenderer(
    private val context: Context,
    private val host: CanvasHostView,
    private val list: CanvasListView,
    private val hint: String,
    private var query: String,
    private val onQueryChanged: (String) -> Unit
) : BaseItemRenderer(context) {
    override var height: Float = 64f * density
    
    private var editText: EditText? = null
    private var isEditing = false
    private var scrollListener: ((Float) -> Unit)? = null

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        val r = 12f * density
        val rect = android.graphics.RectF(paddingStart, 8f * density, width - paddingEnd, height - 8f * density)
        
        bgPaint.color = tokens.surface
        canvas.drawRoundRect(rect, r, r, bgPaint)
        
        val iconSize = 20f * density
        val iconX = rect.left + 12f * density
        val iconY = rect.centerY() - iconSize / 2f
        CanvasIcons.drawSearch(canvas, iconX, iconY, iconSize, tokens.textSecondary)

        if (isEditing) return // Hide drawing while editing

        textPaint.color = if (query.isEmpty()) (tokens.textSecondary and 0x00FFFFFF) or (0x88 shl 24) else tokens.textPrimary
        val displayText = query.ifEmpty { hint }
        canvas.drawText(displayText, iconX + iconSize + 12f * density, rect.centerY() + 6f * density, textPaint)
    }

    override fun onClick(x: Float, y: Float) {
        startEditing()
    }

    private fun startEditing() {
        if (isEditing) return
        isEditing = true

        val etHeight = height - 20f * density
        
        val et = EditText(context).apply {
            setText(query)
            setSelection(query.length)
            
            val t = com.timely.msminutes.util.ThemeStore.get().current()
            // Blend with item background (surface)
            background = GradientDrawable().apply {
                setColor(t?.surface ?: 0xFF333333.toInt())
                cornerRadius = 12f * density
            }
            
            setTextColor(t?.textPrimary ?: 0xFFFFFFFF.toInt())
            this.hint = hint
            setHintTextColor((t?.textSecondary ?: 0xFF888888.toInt()) and 0x88FFFFFF.toInt())
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_SEARCH
            setPadding((8f * density).toInt(), 0, (8f * density).toInt(), 0)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 16f * density)
            
            updatePosition(this, etHeight)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newVal = s?.toString() ?: ""
                    if (newVal != query) {
                        query = newVal
                        onQueryChanged(query)
                    }
                }
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_SEARCH || actionId == EditorInfo.IME_ACTION_DONE) {
                    stopEditing()
                    true
                } else false
            }
        }

        editText = et
        host.addView(et)
        et.requestFocus()
        
        val l = { _: Float ->
            updatePosition(et, etHeight)
        }
        scrollListener = l
        list.addOnScrollListener(l)
        
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(et, 0)
        host.invalidate()
    }

    private fun updatePosition(et: EditText, etHeight: Float) {
        val hostTop = list.bounds.top + top - list.scrollY + 10f * density
        val lp = et.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(0, 0)
        
        val etX = paddingStart + 44f * density
        lp.width = (list.bounds.width() - 44f * density - paddingStart - paddingEnd + 12f * density).toInt()
        lp.height = etHeight.toInt()
        lp.topMargin = hostTop.toInt()
        lp.leftMargin = (list.bounds.left + etX - 6f * density).toInt()
        et.layoutParams = lp
        
        et.visibility = if (hostTop + etHeight < list.bounds.top || hostTop > list.bounds.bottom) {
            android.view.View.GONE
        } else {
            android.view.View.VISIBLE
        }
    }

    fun stopEditing() {
        if (!isEditing) return
        isEditing = false
        scrollListener?.let { list.removeOnScrollListener(it) }
        scrollListener = null
        
        editText?.let {
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(it.windowToken, 0)
            host.removeView(it)
        }
        editText = null
        host.invalidate()
    }
}
