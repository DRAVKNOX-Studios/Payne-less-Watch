package com.timely.msminutes.ui.timer

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil

object TimerCreateThemeHelper {

    fun applyTheme(
        context: Context,
        root: View,
        vibrateSwitch: SwitchCompat?,
        labelInput: EditText,
        hours: NumberPicker?,
        minutes: NumberPicker?,
        seconds: NumberPicker?
    ) {
        val t: ThemeTokens = ThemeStore.get().current() ?: return
        val p = Prefs(context)

        ThemeApplier.applyDialogRoot(root, t, 24)
        ThemeApplier.applyDialogTextColor(root, t)
        ThemeUtil.tintNumberPicker(hours, t.textPrimary, t.accent)
        ThemeUtil.tintNumberPicker(minutes, t.textPrimary, t.accent)
        ThemeUtil.tintNumberPicker(seconds, t.textPrimary, t.accent)

        // Start button — accent-filled pill
        ThemeApplier.applyAccentButton(root.findViewById<Button>(R.id.btn_timer_start), t)

        // Cancel button — surface-filled pill
        ThemeApplier.applyPillButton(root.findViewById<Button>(R.id.btn_timer_cancel), t, accentFill = false)

        ThemeApplier.applyCard(labelInput, t, p.isCustomTheme)
        labelInput.setHintTextColor(
            Color.argb(0x88,
                Color.red(t.textPrimary), Color.green(t.textPrimary), Color.blue(t.textPrimary))
        )
        ThemeApplier.tintCompoundButton(vibrateSwitch, t)
        root.findViewById<TextView>(R.id.text_timer_sound).setTextColor(
            Color.argb(0xBB,
                Color.red(t.textPrimary), Color.green(t.textPrimary), Color.blue(t.textPrimary))
        )
    }
}
