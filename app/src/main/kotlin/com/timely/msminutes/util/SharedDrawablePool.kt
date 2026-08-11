package com.timely.msminutes.util

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable

object SharedDrawablePool {

    private val pool = object : LinkedHashMap<CacheKey, Drawable.ConstantState>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<CacheKey, Drawable.ConstantState>?): Boolean {
            return size > 12
        }
    }

    const val CORNER_NONE:   Int = 0
    const val CORNER_TITLE:  Int = 0
    const val CORNER_CARD:   Int = 24
    const val CORNER_FAB:    Int = 28
    const val CORNER_PILL:   Int = 50
    const val CORNER_DIALOG: Int = 20
    const val SHAPE_RECT:    Int = 0
    const val SHAPE_OVAL:    Int = 1

    private data class CacheKey(
        val fillColor: Int,
        val cornerDp: Int,
        val strokeColor: Int,
        val strokeWidthDp: Int,
        val shapeType: Int
    )

    fun get(
        fillColor: Int,
        cornerDp: Int,
        density: Float,
        strokeColor: Int = 0,
        strokeWidthDp: Int = 0,
        shapeType: Int = SHAPE_RECT
    ): Drawable {
        if (cornerDp <= 0 && strokeColor == 0 && shapeType == SHAPE_RECT) {
            return ColorDrawable(fillColor)
        }

        val key = CacheKey(fillColor, cornerDp, strokeColor, strokeWidthDp, shapeType)
        
        val cachedState = pool[key]
        if (cachedState != null) {
            return cachedState.newDrawable()
        }

        val template = GradientDrawable().apply {
            shape = if (shapeType == SHAPE_OVAL) GradientDrawable.OVAL else GradientDrawable.RECTANGLE
            setColor(fillColor)
            if (cornerDp > 0 && shapeType == SHAPE_RECT) setCornerRadius(cornerDp * density)
            if (strokeColor != 0 && strokeWidthDp > 0) {
                setStroke((strokeWidthDp * density).toInt(), strokeColor)
            }
        }
        
        val state = template.constantState!!
        pool[key] = state
        
        return state.newDrawable()
    }

    fun invalidate() {
        pool.clear()
    }

    fun invalidate(fillColor: Int, cornerDp: Int, strokeColor: Int = 0, strokeWidthDp: Int = 0, shapeType: Int = SHAPE_RECT) {
        pool.remove(CacheKey(fillColor, cornerDp, strokeColor, strokeWidthDp, shapeType))
    }

    // Retained for binary compatibility if needed, but unused internally now
    fun encodeKey(fill: Int, cornerDp: Int, stroke: Int, strokeWidth: Int, shapeType: Int = SHAPE_RECT): Long {
        val fillL    = (fill.toLong() and 0xFFFFFFFFL) shl 32
        val strokeL  = (stroke.toLong() and 0xFFFFFFFFL) // Increased to 32 bits for safety
        val cornerL  = (cornerDp.coerceIn(0, 255).toLong()) shl 16
        val widthL   = (strokeWidth.coerceIn(0, 255).toLong()) shl 8
        val shapeL   = (shapeType.toLong() and 0xFF)
        return fillL xor strokeL xor cornerL xor widthL xor shapeL
    }
}
