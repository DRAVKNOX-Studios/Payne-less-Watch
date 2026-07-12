package com.timely.msminutes.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.NumberPicker
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.WindowCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.view.ThemedNumberPicker
import com.timely.msminutes.util.ThemeUtil.applyColorsIterative

object ThemeUtil {
    fun applyTheme(mode: String?) {
        val m = mode ?: "system"
        when (m) {
            "light" -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            "dark"  -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else     -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    fun isColorLight(color: Int): Boolean {
        val darkness =
            1 - (0.299 * Color.red(color) + 0.587 * Color.green(color) + 0.114 * Color.blue(color)) / 255.0
        return darkness < 0.5
    }

    @SuppressLint("SoonBlockedPrivateApi")
    fun tintNumberPicker(picker: NumberPicker?, digitColor: Int, dividerColor: Int) {
        if (picker == null) return

        if (Build.VERSION.SDK_INT < 37) {
            try {
                val divField = NumberPicker::class.java.getDeclaredField("mSelectionDivider")
                divField.isAccessible = true
                divField.set(picker, ColorDrawable(dividerColor))
            } catch (e: Exception) {
                Log.w("ThemeUtil", "tintNumberPicker: mSelectionDivider unavailable — ${e.message}")
            }
        }

        if (picker is ThemedNumberPicker) {
            picker.setDigitColor(digitColor)
            picker.invalidate()
            return
        }

        applyColorsIterative(picker, digitColor)
        if (Build.VERSION.SDK_INT < 37) {
            try {
                val f = NumberPicker::class.java.getDeclaredField("mSelectorWheelPaint")
                f.isAccessible = true
                val p = f.get(picker) as Paint?
                p?.color = digitColor
            } catch (ignored: Exception) {}
        }
        picker.invalidate()
    }

    fun tintNumberPicker(picker: NumberPicker?, color: Int) {
        tintNumberPicker(picker, color, color)
    }

    @JvmOverloads
    fun tintWidget(view: CompoundButton?, accentColor: Int, bgColor: Int = Color.TRANSPARENT) {
        if (view == null) return
        val unchecked = if (isColorLight(bgColor)) -0x616162 else -0x424243
        val csl = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()),
            intArrayOf(accentColor, unchecked)
        )
        view.buttonTintList = csl
        if (view is SwitchCompat) {
            view.thumbTintList = csl
            view.trackTintList = csl
        }
    }

    fun tintWidget(view: View?, accentColor: Int) {
        if (view is CompoundButton) tintWidget(view, accentColor)
    }

    fun tintRadioGroup(group: RadioGroup?, accentColor: Int) {
        if (group == null) return
        for (i in 0 until group.childCount) {
            val v = group.getChildAt(i)
            if (v is RadioButton) tintWidget(v as CompoundButton, accentColor)
        }
    }

    fun tintSeekBar(seekBar: SeekBar?, accentColor: Int) {
        if (seekBar == null) return
        val csl = ColorStateList.valueOf(accentColor)
        seekBar.thumbTintList = csl
        seekBar.progressTintList = csl
    }

    fun applyScrollbar(view: View?, accentColor: Int) {
        if (view == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val d = view.context.resources.displayMetrics.density
            val thumb = SharedDrawablePool.get(accentColor, 4, d)
            view.verticalScrollbarThumbDrawable = thumb
            view.horizontalScrollbarThumbDrawable = thumb
        }
    }

    fun applyCustomColors(activity: Activity, prefs: Prefs) {
        if (!prefs.isCustomTheme) return
        val bg = prefs.backgroundColor
        val font = prefs.fontColor
        val window = activity.window
        val ctrl = WindowCompat.getInsetsController(window, window.decorView)

        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = bg
            @Suppress("DEPRECATION")
            window.navigationBarColor = bg
        }

        ctrl.isAppearanceLightStatusBars = isColorLight(bg)
        ctrl.isAppearanceLightNavigationBars = isColorLight(bg)
        val root = activity.findViewById<View>(android.R.id.content)
        if (root != null) applyColorsIterative(root, font)
    }

    fun applyCardStyle(card: View?, prefs: Prefs) {
        if (card == null) return
        if (!prefs.isCustomTheme) {
            card.setBackgroundResource(R.drawable.bg_card)
            return
        }
        val bg = prefs.backgroundColor
        val cardBg = if (isColorLight(bg))
            Color.argb(20, 0, 0, 0)
        else
            Color.argb(26, 255, 255, 255)
        val gd = GradientDrawable()
        gd.setColor(cardBg)
        gd.cornerRadius = dpToPx(card.context, 24).toFloat()
        card.background = gd
        applyColorsIterative(card, prefs.fontColor)
    }

    fun applyColorsIterative(root: View?, fontColor: Int) {
        if (root == null) return
        val stack = ArrayDeque<View>()
        stack.addLast(root)
        while (stack.isNotEmpty()) {
            val view = stack.removeLast()
            if (view is TextView) view.setTextColor(fontColor)
            if (view is ViewGroup) {
                for (i in 0 until view.childCount) {
                    stack.addLast(view.getChildAt(i))
                }
            }
        }
    }

    fun applyColorsRecursively(view: View?, fontColor: Int) =
        applyColorsIterative(view, fontColor)

    fun dpToPx(context: Context, dp: Int): Int =
        Math.round(dp * context.resources.displayMetrics.density)
}