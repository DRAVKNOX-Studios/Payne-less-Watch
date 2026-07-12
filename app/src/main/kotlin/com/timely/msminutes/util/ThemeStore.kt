package com.timely.msminutes.util

import android.graphics.Color
import com.timely.msminutes.data.Prefs
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

class ThemeStore private constructor() {
    interface ThemeListener {
        fun onThemeChanged(t: ThemeTokens?)
    }

    private val mListeners: MutableList<WeakReference<ThemeListener>> = ArrayList()
    private var mCurrent: ThemeTokens? = null
    private var mPrefs: Prefs? = null

    fun init(prefs: Prefs) {
        mPrefs = prefs
        setTokens(buildFrom(prefs))
    }

    fun prefs(): Prefs? = mPrefs

    fun refresh() {
        mPrefs?.let { setTokens(buildFrom(it)) }
    }

    fun setTokens(t: ThemeTokens?) {
        SharedDrawablePool.invalidate()
        mCurrent = t
        val it = mListeners.iterator()
        while (it.hasNext()) {
            val l = it.next().get()
            if (l == null) it.remove() else l.onThemeChanged(t)
        }
    }

    fun current(): ThemeTokens? = mCurrent

    fun subscribe(l: ThemeListener) {
        var found = false
        val it = mListeners.iterator()
        while (it.hasNext()) {
            val ref = it.next().get()
            if (ref == null) it.remove()
            else if (ref === l) found = true
        }
        if (!found) mListeners.add(WeakReference(l))
        if (mCurrent != null) l.onThemeChanged(mCurrent)
    }

    fun unsubscribe(l: ThemeListener?) {
        val it = mListeners.iterator()
        while (it.hasNext()) {
            val ref = it.next().get()
            if (ref == null || ref === l) it.remove()
        }
    }

    companion object {
        private var sInstance: ThemeStore? = null

        @Synchronized
        fun get(): ThemeStore {
            if (sInstance == null) sInstance = ThemeStore()
            return sInstance!!
        }

        fun buildFrom(p: Prefs): ThemeTokens {
            val bg     = p.backgroundColor
            val accent = p.accentColor
            val font   = p.fontColor

            val surface: Int = if (ThemeApplier.isLight(bg))
                blendWith(bg, Color.BLACK,   0.08f)
            else
                blendWith(bg, Color.WHITE,   0.10f)

            val textSecondary: Int = blendColor(font, bg, 0.55f)
            val onAccent = if (ThemeApplier.isLight(accent)) Color.BLACK else Color.WHITE

            return ThemeTokens(
                primary       = accent,
                background    = bg,
                surface       = surface,
                textPrimary   = font,
                textSecondary = textSecondary,
                accent        = accent,
                font          = onAccent,
                isCustom      = p.isCustomTheme
            )
        }

        private fun blendWith(base: Int, overlay: Int, t: Float): Int {
            val r = clamp(Math.round(Color.red(base)   * (1 - t) + Color.red(overlay)   * t))
            val g = clamp(Math.round(Color.green(base) * (1 - t) + Color.green(overlay) * t))
            val b = clamp(Math.round(Color.blue(base)  * (1 - t) + Color.blue(overlay)  * t))
            return Color.rgb(r, g, b)
        }

        private fun blendColor(fg: Int, bg: Int, alpha: Float): Int {
            val r = clamp(Math.round(Color.red(fg)   * alpha + Color.red(bg)   * (1 - alpha)))
            val g = clamp(Math.round(Color.green(fg) * alpha + Color.green(bg) * (1 - alpha)))
            val b = clamp(Math.round(Color.blue(fg)  * alpha + Color.blue(bg)  * (1 - alpha)))
            return Color.rgb(r, g, b)
        }

        private fun clamp(v: Int): Int = max(0, min(255, v))
    }
}
