package com.timely.msminutes.ui.canvas.items

import android.content.Context
import android.graphics.Canvas
import android.graphics.drawable.GradientDrawable
import android.text.Editable
import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.util.ThemeTokens

class InlineEditItemRenderer(
    private val context: Context,
    private val host: CanvasHostView,
    private val list: CanvasListView,
    private val label: String,
    private var value: String,
    private val hint: String = "",
    private val onValueChanged: (String) -> Unit
) : BaseItemRenderer(context) {

    private var editText: EditText? = null
    private var isEditing = false
    private var scrollListener: ((Float) -> Unit)? = null

    override fun draw(canvas: Canvas, tokens: ThemeTokens, width: Float) {
        resetPaints(density)
        val textPaint = textPaint
        val subTextPaint = subTextPaint

        // Draw label always
        textPaint.color = if (isEditing) tokens.accent else tokens.textPrimary
        val labelY = (height / 2f) - (4f * density)
        canvas.drawText(label, paddingStart, labelY, textPaint)

        if (isEditing) return // Hide the value drawing, EditText is on top

        subTextPaint.color = (tokens.textSecondary and 0x00FFFFFF) or (0x88 shl 24)
        val valueY = (height / 2f) + (16f * density)
        val displayValue = value.ifEmpty { hint }
        canvas.drawText(displayValue, paddingStart, valueY, subTextPaint)
    }

    override fun onClick(x: Float, y: Float) {
        startEditing()
    }

    private fun startEditing() {
        if (isEditing) return
        isEditing = true

        val etHeight = 32f * density
        
        val et = EditText(context).apply {
            setText(value)
            setSelection(value.length)
            
            val t = com.timely.msminutes.util.ThemeStore.get().current()
            val bgColor = t?.background ?: 0xFF000000.toInt()
            
            background = GradientDrawable().apply {
                setColor(bgColor)
                cornerRadius = 8f * density
            }
            
            setTextColor(t?.textPrimary ?: 0xFFFFFFFF.toInt())
            this.hint = hint
            setHintTextColor((t?.textSecondary ?: 0xFF888888.toInt()) and 0x88FFFFFF.toInt())
            isSingleLine = true
            imeOptions = EditorInfo.IME_ACTION_DONE
            setPadding((12f * density).toInt(), 0, (12f * density).toInt(), 0)
            
            setTextSize(TypedValue.COMPLEX_UNIT_PX, 14f * density)
            
            updatePosition(this, etHeight)

            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    val newVal = s?.toString() ?: ""
                    if (newVal != value) {
                        value = newVal
                        onValueChanged(value)
                    }
                }
            })

            setOnEditorActionListener { _, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    stopEditing()
                    true
                } else false
            }

            setOnKeyListener { _, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
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
        val valueYRelativeToItem = (height / 2f) + (16f * density)
        val hostTop = list.bounds.top + top - list.scrollY + valueYRelativeToItem - (etHeight / 2f) - (2f * density)
        
        val lp = et.layoutParams as? FrameLayout.LayoutParams ?: FrameLayout.LayoutParams(0, 0)
        lp.width = (list.bounds.width() - paddingStart - paddingEnd + 16f * density).toInt()
        lp.height = etHeight.toInt()
        lp.topMargin = hostTop.toInt()
        lp.leftMargin = (list.bounds.left + paddingStart - 8f * density).toInt()
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
