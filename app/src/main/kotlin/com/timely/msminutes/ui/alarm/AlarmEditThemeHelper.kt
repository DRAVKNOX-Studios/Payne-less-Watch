package com.timely.msminutes.ui.alarm

import android.graphics.Color
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.timely.msminutes.R
import com.timely.msminutes.ui.view.DurationPicker
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil

object AlarmEditThemeHelper {
    fun applyTheme(
        window: Window,
        root: View?,
        t: ThemeTokens,
        isCustomTheme: Boolean,
        hourPicker: NumberPicker?,
        minutePicker: NumberPicker?,
        amPmPicker: NumberPicker?,
        labelInput: EditText?,
        noteInput: EditText?,
        soundLabel: TextView?,
        vibrateSwitch: SwitchCompat?,
        gradualVolumeSwitch: SwitchCompat?,
        snoozePicker: DurationPicker?,
        daySelectionHelper: DaySelectionHelper?
    ) {
        ThemeUtil.applyColorsIterative(root, t.textPrimary)
        ThemeUtil.tintNumberPicker(hourPicker, t.textPrimary, t.accent)
        ThemeUtil.tintNumberPicker(minutePicker, t.textPrimary, t.accent)
        ThemeUtil.tintNumberPicker(amPmPicker, t.textPrimary, t.accent)
        ThemeApplier.applyWindow(window, t)
        root?.setBackgroundColor(t.background)
        
        val toolbar = root?.findViewById<Toolbar>(R.id.toolbar)
        toolbar?.setBackgroundColor(t.background)
        toolbar?.navigationIcon?.setTint(t.textPrimary)
        
        ThemeApplier.applyCard(labelInput, t, isCustomTheme)
        ThemeApplier.applyCard(noteInput, t, isCustomTheme)
        
        val hint = Color.argb(
            0x88,
            Color.red(t.textPrimary), Color.green(t.textPrimary), Color.blue(t.textPrimary)
        )
        labelInput?.setHintTextColor(hint)
        noteInput?.setHintTextColor(hint)
        soundLabel?.setTextColor(t.textSecondary)
        
        daySelectionHelper?.applyTheme(t)
        
        ThemeUtil.tintWidget(vibrateSwitch, t.accent, t.background)
        ThemeUtil.tintWidget(gradualVolumeSwitch, t.accent, t.background)
        snoozePicker?.applyTheme(t)
        
        val saveBtn = root?.findViewById<Button>(R.id.btn_save)
        saveBtn?.let { ThemeApplier.applyAccentButton(it, t) }

        val scroll = root?.findViewById<View>(R.id.alarm_edit_scroll)
        ThemeApplier.applyScrollbar(scroll, t)
    }
}
