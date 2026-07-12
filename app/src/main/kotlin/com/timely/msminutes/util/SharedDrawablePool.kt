package com.timely.msminutes.util

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import java.lang.ref.WeakReference

object SharedDrawablePool {

    private val pool = HashMap<Long, WeakReference<Drawable.ConstantState>>(16)

    const val CORNER_NONE:   Int = 0
    const val CORNER_TITLE:  Int = 0
    const val CORNER_CARD:   Int = 24
    const val CORNER_FAB:    Int = 28
    const val CORNER_PILL:   Int = 50
    const val CORNER_DIALOG: Int = 20

    fun get(
        fillColor: Int,
        cornerDp: Int,
        density: Float,
        strokeColor: Int = 0,
        strokeWidthDp: Int = 0
    ): Drawable {
        if (cornerDp <= 0 && strokeColor == 0) {
            return ColorDrawable(fillColor)
        }

        val key = encodeKey(fillColor, cornerDp, strokeColor, strokeWidthDp)
        
        val cachedState = pool[key]?.get()
        if (cachedState != null) {
            return cachedState.newDrawable()
        }

        val template = GradientDrawable().apply {
            setColor(fillColor)
            if (cornerDp > 0) setCornerRadius(cornerDp * density)
            if (strokeColor != 0 && strokeWidthDp > 0) {
                setStroke((strokeWidthDp * density).toInt(), strokeColor)
            }
        }
        
        val state = template.constantState!!
        pool[key] = WeakReference(state)
        
        return state.newDrawable()
    }

    fun invalidate() {
        pool.clear()
    }

    fun invalidate(fillColor: Int, cornerDp: Int, strokeColor: Int = 0, strokeWidthDp: Int = 0) {
        pool.remove(encodeKey(fillColor, cornerDp, strokeColor, strokeWidthDp))
    }

    fun encodeKey(fill: Int, cornerDp: Int, stroke: Int, strokeWidth: Int): Long {
        val fillL    = (fill.toLong() and 0xFFFFFFFFL) shl 32
        val strokeL  = (stroke.toLong() and 0xFFFFL) shl 16
        val cornerL  = (cornerDp.coerceIn(0, 255).toLong()) shl 8
        val widthL   = (strokeWidth.coerceIn(0, 255).toLong())
        return fillL or strokeL or cornerL or widthL
    }
}
