package com.timely.msminutes.ui.settings

import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.ui.view.CustomColorPickerDialog
import com.timely.msminutes.ui.view.DurationPicker
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil

class SettingsAdapter(private val items: List<SettingItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    sealed class SettingItem {
        data class Header(val title: String) : SettingItem()
        data class Toggle(val id: Int, val title: String, var isChecked: Boolean, val onChecked: (Boolean) -> Unit) : SettingItem()
        data class Edit(val id: Int, val title: String, var value: String, val onChanged: (String) -> Unit) : SettingItem()
        data class Duration(val id: Int, val title: String, var value: Int, val onChanged: (Int) -> Unit) : SettingItem()
        data class ColorPicker(val id: Int, val title: String, var color: Int, val onSelected: (Int) -> Unit) : SettingItem()
    }

    private var tokens: ThemeTokens? = ThemeStore.get().current()

    fun setTokens(t: ThemeTokens?) {
        tokens = t
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is SettingItem.Header   -> 0
        is SettingItem.Toggle   -> 1
        is SettingItem.Edit     -> 2
        is SettingItem.Duration -> 3
        is SettingItem.ColorPicker -> 4
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> HeaderVH(inflater.inflate(R.layout.item_settings_header, parent, false))
            1 -> ToggleVH(inflater.inflate(R.layout.item_settings_toggle, parent, false))
            2 -> EditVH(inflater.inflate(R.layout.item_settings_edit, parent, false))
            3 -> DurationVH(DurationPicker(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                setPadding(0, 16, 0, 16)
            })
            else -> ColorVH(inflater.inflate(R.layout.item_settings_color, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        val t = tokens ?: return

        when (holder) {
            is HeaderVH -> {
                val h = item as SettingItem.Header
                holder.title.text = h.title
                holder.title.setTextColor(t.textPrimary)
            }
            is ToggleVH -> {
                val toggle = item as SettingItem.Toggle
                holder.title.text = toggle.title
                holder.title.setTextColor(t.textPrimary)
                holder.switch.setOnCheckedChangeListener(null)
                holder.switch.isChecked = toggle.isChecked
                holder.switch.setOnCheckedChangeListener { _, c -> 
                    toggle.isChecked = c
                    toggle.onChecked(c) 
                }
                ThemeUtil.tintWidget(holder.switch, t.accent, t.background)
            }
            is EditVH -> {
                val edit = item as SettingItem.Edit
                holder.label.text = edit.title
                holder.label.setTextColor(t.textPrimary)
                holder.input.removeTextChangedListener(holder.watcher)
                holder.input.setText(edit.value)
                holder.input.setTextColor(t.textPrimary)
                holder.input.setHintTextColor(t.textSecondary)
                holder.watcher = object : TextWatcher {
                    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                    override fun afterTextChanged(s: Editable?) {
                        val str = s?.toString() ?: ""
                        edit.value = str
                        edit.onChanged(str)
                    }
                }
                holder.input.addTextChangedListener(holder.watcher)
            }
            is DurationVH -> {
                val dur = item as SettingItem.Duration
                holder.picker.setValue(dur.value)
                holder.picker.setOnValueChangeListener {
                    dur.value = it
                    dur.onChanged(it)
                }
                holder.picker.applyTheme(t)
            }
            is ColorVH -> {
                val cp = item as SettingItem.ColorPicker
                holder.title.text = cp.title
                holder.title.setTextColor(t.textPrimary)
                
                val gd = holder.preview.background as android.graphics.drawable.GradientDrawable
                gd.setColor(cp.color)
                
                holder.row.setOnClickListener {
                    CustomColorPickerDialog(it.context, cp.color) { newColor ->
                        cp.color = newColor
                        cp.onSelected(newColor)
                        notifyItemChanged(holder.bindingAdapterPosition)
                    }.show()
                }
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class HeaderVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.settings_header)
    }

    class ToggleVH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.settings_text)
        val switch: SwitchCompat = view.findViewById(R.id.settings_switch)
    }

    class EditVH(view: View) : RecyclerView.ViewHolder(view) {
        val label: TextView = view.findViewById(R.id.edit_label)
        val input: EditText = view.findViewById(R.id.edit_input)
        var watcher: TextWatcher? = null
    }

    class DurationVH(val picker: DurationPicker) : RecyclerView.ViewHolder(picker)

    class ColorVH(view: View) : RecyclerView.ViewHolder(view) {
        val row: View = view.findViewById(R.id.settings_row)
        val title: TextView = view.findViewById(R.id.settings_text)
        val preview: View = view.findViewById(R.id.color_preview)
    }
}
