package com.timely.msminutes.ui.view

import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import com.timely.msminutes.R
import com.timely.msminutes.util.ThemeTokens

/**
 * Shared duration picker view.
 */
class DurationPicker @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val valueText: TextView
    private val btnMinus: ImageButton
    private val btnPlus: ImageButton

    private var value: Int = 0
    private var listener: ((Int) -> Unit)? = null

    init {
        orientation = HORIZONTAL
        LayoutInflater.from(context).inflate(R.layout.view_duration_picker, this, true)
        
        valueText = findViewById(R.id.text_duration_value)
        btnMinus = findViewById(R.id.btn_duration_minus)
        btnPlus = findViewById(R.id.btn_duration_plus)

        btnMinus.setOnClickListener { 
            if (value > 1) {
                value--
                updateUI()
                listener?.invoke(value)
            }
        }
        btnPlus.setOnClickListener {
            value++
            updateUI()
            listener?.invoke(value)
        }
    }

    fun setValue(v: Int) {
        value = v
        updateUI()
    }

    fun getValue(): Int = value

    fun setOnValueChangeListener(l: (Int) -> Unit) {
        listener = l
    }

    private fun updateUI() {
        valueText.text = "$value min"
    }

    fun applyTheme(t: ThemeTokens) {
        valueText.setTextColor(t.textPrimary)
        val csl = ColorStateList.valueOf(t.accent)
        btnMinus.imageTintList = csl
        btnPlus.imageTintList = csl
    }
}
