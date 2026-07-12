package com.timely.msminutes.util

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.WindowCompat
import androidx.core.widget.TextViewCompat
import com.timely.msminutes.R

object ThemeApplier {

    fun isLight(color: Int): Boolean = ThemeUtil.isColorLight(color)

    private fun setSharedBackground(view: View, fillColor: Int, cornerDp: Int, strokeColor: Int = 0, strokeWidthDp: Int = 0) {
        val key = SharedDrawablePool.encodeKey(fillColor, cornerDp, strokeColor, strokeWidthDp)
        if (view.getTag(R.id.view_bg_color) == key && view.background != null) return

        val d = view.context.resources.displayMetrics.density
        val gd = SharedDrawablePool.get(fillColor, cornerDp, d, strokeColor, strokeWidthDp)

        val pL = view.paddingLeft
        val pT = view.paddingTop
        val pR = view.paddingRight
        val pB = view.paddingBottom
        val pS = view.paddingStart
        val pE = view.paddingEnd

        view.backgroundTintList = null
        view.background = gd

        view.setPaddingRelative(pS, pT, pE, pB)
        
        view.setTag(R.id.view_bg_color, key)
    }

    fun applyBackground(root: View?, t: ThemeTokens) {
        if (root == null) return
        setSharedBackground(root, t.background, SharedDrawablePool.CORNER_NONE)
    }

    fun applyRootBackground(root: View?, t: ThemeTokens) = applyBackground(root, t)

    fun applyTitleBar(view: View?, t: ThemeTokens) {
        if (view == null) return
        setSharedBackground(view, t.background, SharedDrawablePool.CORNER_TITLE)
    }

    fun applyCard(view: View?, t: ThemeTokens, customTheme: Boolean) {
        if (view == null) return
        if (!customTheme) {
            val resId = R.drawable.bg_card
            if (view.getTag(R.id.view_bg_color) != resId) {
                view.setBackgroundResource(resId)
                view.setTag(R.id.view_bg_color, resId)
            }
            return
        }
        setSharedBackground(view, cardOverlayColor(t.background), SharedDrawablePool.CORNER_CARD)
    }

    fun applyCardDelete(view: View?, t: ThemeTokens, customTheme: Boolean) {
        if (view == null) return
        if (!customTheme) {
            val resId = R.drawable.bg_card_delete
            if (view.getTag(R.id.view_bg_color) != resId) {
                view.setBackgroundResource(resId)
                view.setTag(R.id.view_bg_color, resId)
            }
            return
        }
        val deleteBg = if (isLight(t.background))
            Color.argb(40, 220, 50, 50)
        else
            Color.argb(50, 255, 80, 80)
        setSharedBackground(view, deleteBg, SharedDrawablePool.CORNER_CARD)
    }

    fun applyPillButton(view: View?, t: ThemeTokens, accentFill: Boolean = true) {
        if (view == null) return
        val fill = if (accentFill) t.accent else t.surface
        setSharedBackground(view, fill, SharedDrawablePool.CORNER_PILL)

        if (view is TextView) {
            val textColor = t.textPrimary
            if (view.getTag(R.id.view_text_color) != textColor) {
                view.setTextColor(flatCsl(textColor))
                view.setTag(R.id.view_text_color, textColor)
            }
        }
    }

    fun applyAccentButton(btn: Button?, t: ThemeTokens) = applyPillButton(btn, t, accentFill = true)

    fun applyFab(fab: View?, t: ThemeTokens) {
        if (fab == null) return
        setSharedBackground(fab, t.primary, SharedDrawablePool.CORNER_FAB)
    }

    fun applyFabDrawable(fab: View?, t: ThemeTokens) {
        if (fab == null) return
        setSharedBackground(fab, t.accent, SharedDrawablePool.CORNER_FAB)
        if (fab is TextView) {
            val textColor = t.textPrimary
            if (fab.getTag(R.id.view_text_color) != textColor) {
                fab.setTextColor(flatCsl(textColor))
                fab.setTag(R.id.view_text_color, textColor)
            }
        }
    }

    fun applyDialogRoot(root: View?, t: ThemeTokens, cornerDp: Int) {
        if (root == null) return
        setSharedBackground(root, t.surface, cornerDp)
    }

    fun applyTextPrimary(tv: TextView?, t: ThemeTokens) {
        if (tv == null) return
        if (tv.getTag(R.id.view_text_color) != t.textPrimary) {
            tv.setTextColor(t.textPrimary)
            tv.setTag(R.id.view_text_color, t.textPrimary)
        }
    }

    fun applyTextSecondary(tv: TextView?, t: ThemeTokens) {
        if (tv == null) return
        if (tv.getTag(R.id.view_text_color) != t.textSecondary) {
            tv.setTextColor(t.textSecondary)
            tv.setTag(R.id.view_text_color, t.textSecondary)
        }
    }

    fun applyDialogTextColor(root: View?, t: ThemeTokens) {
        if (root == null) return
        ThemeUtil.applyColorsRecursively(root, t.textPrimary)
    }

    fun applyTextColor(root: View?, t: ThemeTokens) = applyDialogTextColor(root, t)

    fun tintCompoundButton(cb: CompoundButton?, t: ThemeTokens) {
        ThemeUtil.tintWidget(cb, t.accent, t.background)
    }

    fun applyWindow(window: Window, t: ThemeTokens) {
        if (Build.VERSION.SDK_INT < 35) {
            @Suppress("DEPRECATION")
            window.statusBarColor = t.background
            @Suppress("DEPRECATION")
            window.navigationBarColor = t.background
        }
        val ctrl = WindowCompat.getInsetsController(window, window.decorView)
        ctrl.isAppearanceLightStatusBars     = isLight(t.background)
        ctrl.isAppearanceLightNavigationBars = isLight(t.background)
    }

    fun applyToolbarIcon(icon: ImageView?, t: ThemeTokens) {
        if (icon != null) icon.imageTintList = ColorStateList.valueOf(t.textPrimary)
    }

    fun applyScrollbar(view: View?, t: ThemeTokens) {
        ThemeUtil.applyScrollbar(view, t.accent)
    }

    fun applyTabBar(t: ThemeTokens, vararg tabs: TextView) {
        val unselected = Color.argb(
            0x88,
            Color.red(t.textPrimary),
            Color.green(t.textPrimary),
            Color.blue(t.textPrimary)
        )
        val csl = ColorStateList(
            arrayOf(intArrayOf(android.R.attr.state_selected), intArrayOf()),
            intArrayOf(t.accent, unselected)
        )
        for (tab in tabs) {
            tab.setTextColor(csl)
            TextViewCompat.setCompoundDrawableTintList(tab, csl)
        }
    }

    private fun cardOverlayColor(bg: Int): Int =
        if (isLight(bg)) Color.argb(20, 0, 0, 0)
        else             Color.argb(26, 255, 255, 255)

    private fun flatCsl(color: Int): ColorStateList =
        ColorStateList(
            arrayOf(
                intArrayOf( android.R.attr.state_pressed),
                intArrayOf( android.R.attr.state_focused),
                intArrayOf(-android.R.attr.state_enabled),
                intArrayOf()
            ),
            intArrayOf(color, color, color, color)
        )
}
