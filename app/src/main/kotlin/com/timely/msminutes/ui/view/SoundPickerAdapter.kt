package com.timely.msminutes.ui.view

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.RadioButton
import android.widget.TextView
import com.timely.msminutes.R
import com.timely.msminutes.util.ThemeUtil

class SoundPickerAdapter(
    private val context: Context,
    private val sounds: List<SoundItem>,
    private val isCustom: Boolean,
    private val fontColor: Int,
    private val accentColor: Int,
    private val getSelectedPosition: () -> Int
) : BaseAdapter() {
    override fun getCount() = sounds.size
    override fun getItem(pos: Int): Any? = sounds[pos]
    override fun getItemId(pos: Int) = pos.toLong()

    override fun getView(pos: Int, convert: View?, parent: ViewGroup?): View {
        val view = convert
            ?: LayoutInflater.from(context).inflate(R.layout.item_sound, parent, false)
        val item     = sounds[pos]
        val nameView = view.findViewById<TextView>(R.id.text_sound_name)
        val radio    = view.findViewById<RadioButton>(R.id.radio_selected)
        nameView.text   = item.name
        radio.isChecked = pos == getSelectedPosition()
        ThemeUtil.tintWidget(radio, accentColor)
        if (isCustom) {
            nameView.setTextColor(fontColor)
            radio.setTextColor(fontColor)
        }
        return view
    }
}
