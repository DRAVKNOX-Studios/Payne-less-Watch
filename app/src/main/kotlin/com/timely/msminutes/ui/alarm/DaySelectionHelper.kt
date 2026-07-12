package com.timely.msminutes.ui.alarm

import android.widget.TextView
import com.timely.msminutes.util.SharedDrawablePool
import com.timely.msminutes.util.ThemeTokens

class DaySelectionHelper(
    private val dayToggles: Array<TextView?>,
    private val daySelected: BooleanArray,
    private val onSelectionChanged: () -> Unit
) {

    init {
        dayToggles.forEachIndexed { index, textView ->
            textView?.setOnClickListener {
                daySelected[index] = !daySelected[index]
                textView.isSelected = daySelected[index]
                onSelectionChanged()
            }
        }
    }

    fun populate(repeatDays: Int) {
        for (i in 0..6) {
            daySelected[i] = (repeatDays and (1 shl i)) != 0
            dayToggles[i]?.isSelected = daySelected[i]
        }
    }

    fun getRepeatDays(): Int {
        var days = 0
        for (i in 0..6) {
            if (daySelected[i]) days = days or (1 shl i)
        }
        return days
    }

    fun applyTheme(tokens: ThemeTokens?) {
        if (tokens == null) return
        val density = dayToggles.firstOrNull { it != null }?.context?.resources?.displayMetrics?.density ?: 1f
        
        for (day in dayToggles) {
            if (day == null) continue
            val isSel = day.isSelected
            
            val bgColor = if (isSel) tokens.accent else tokens.surface
            val textColor = tokens.textPrimary
            
            day.background = SharedDrawablePool.get(bgColor, 100, density)
            day.setTextColor(textColor)
            day.backgroundTintList = null
            day.alpha = if (isSel) 1.0f else 0.7f
        }
    }
}
