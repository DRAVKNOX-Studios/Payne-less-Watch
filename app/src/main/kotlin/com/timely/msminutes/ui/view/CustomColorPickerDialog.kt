package com.timely.msminutes.ui.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.util.ThemeUtil

class CustomColorPickerDialog(
    context: Context,
    private var currentColor: Int,
    private val listener: OnColorSelectedListener
) : Dialog(context) {
    fun interface OnColorSelectedListener {
        fun onColorSelected(color: Int)
    }

    private val prefs: Prefs = Prefs(context)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_custom_color_picker)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val hex = findViewById<EditText>(R.id.edit_hex)
        val r = findViewById<SeekBar>(R.id.seek_red)
        val g = findViewById<SeekBar>(R.id.seek_green)
        val b = findViewById<SeekBar>(R.id.seek_blue)
        val preview = findViewById<View>(R.id.color_preview)
        val sliders = findViewById<View>(R.id.layout_sliders)
        val selectBtn = findViewById<Button>(R.id.btn_select)
        val rgbToggle = findViewById<Button>(R.id.btn_rgb_toggle)

        applyCurrentTheme()
        updateUI(hex, r, g, b, preview)

        val sl = object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    currentColor = Color.rgb(r.progress, g.progress, b.progress)
                    hex.setText(String.format("#%06X", (0xFFFFFF and currentColor)))
                    updatePreview(preview, currentColor)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        }

        r.setOnSeekBarChangeListener(sl)
        g.setOnSeekBarChangeListener(sl)
        b.setOnSeekBarChangeListener(sl)

        hex.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                try {
                    currentColor = Color.parseColor(s.toString())
                    r.progress = Color.red(currentColor)
                    g.progress = Color.green(currentColor)
                    b.progress = Color.blue(currentColor)
                    updatePreview(preview, currentColor)
                } catch (ignored: Exception) {
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        rgbToggle.setOnClickListener { sliders.visibility = if (sliders.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        selectBtn.setOnClickListener {
            listener.onColorSelected(currentColor)
            dismiss()
        }
    }

    private fun applyCurrentTheme() {
        val accent = prefs.accentColor
        val bg = if (prefs.isCustomTheme) prefs.backgroundColor else -0x1
        val font = if (prefs.isCustomTheme) prefs.fontColor else -0x1000000

        val root = findViewById<View>(R.id.picker_root)
        val gd = GradientDrawable()
        gd.setColor(bg)
        gd.setCornerRadius(ThemeUtil.dpToPx(getContext(), 28).toFloat())
        root.setBackground(gd)

        (findViewById<View?>(R.id.picker_title) as TextView).setTextColor(font)
        (findViewById<View?>(R.id.txt_red) as TextView).setTextColor(font)
        (findViewById<View?>(R.id.txt_green) as TextView).setTextColor(font)
        (findViewById<View?>(R.id.txt_blue) as TextView).setTextColor(font)
        val hexEdit = findViewById<EditText>(R.id.edit_hex)
        hexEdit.setTextColor(font)
        hexEdit.setHintTextColor((font and 0x00FFFFFF) or -0x78000000)

        val rgbToggle = findViewById<Button>(R.id.btn_rgb_toggle)
        rgbToggle.setTextColor(accent)

        val selectBtn = findViewById<Button>(R.id.btn_select)
        val btnBg = selectBtn.getBackground().mutate() as GradientDrawable
        btnBg.setColor(accent)
        selectBtn.setTextColor(if (ThemeUtil.isColorLight(accent)) Color.BLACK else Color.WHITE)

        ThemeUtil.tintSeekBar(findViewById<SeekBar?>(R.id.seek_red), accent)
        ThemeUtil.tintSeekBar(findViewById<SeekBar?>(R.id.seek_green), accent)
        ThemeUtil.tintSeekBar(findViewById<SeekBar?>(R.id.seek_blue), accent)
        ThemeUtil.applyScrollbar(findViewById(R.id.picker_scroll), accent)
    }

    private fun updateUI(hex: EditText, r: SeekBar, g: SeekBar, b: SeekBar, p: View) {
        hex.setText(String.format("#%06X", (0xFFFFFF and currentColor)))
        r.setProgress(Color.red(currentColor))
        g.setProgress(Color.green(currentColor))
        b.setProgress(Color.blue(currentColor))
        updatePreview(p, currentColor)
    }

    private fun updatePreview(v: View, color: Int) {
        val gd = v.getBackground() as GradientDrawable
        gd.setColor(color)
        gd.setStroke(ThemeUtil.dpToPx(getContext(), 2), 0x33888888)
    }
}
