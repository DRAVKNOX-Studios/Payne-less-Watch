package com.timely.msminutes.util

import android.graphics.Color
import androidx.annotation.ColorInt

class ThemeTokens @JvmOverloads constructor(
    @field:ColorInt @param:ColorInt val primary: Int,
    @field:ColorInt @param:ColorInt val background: Int,
    @field:ColorInt @param:ColorInt val surface: Int,
    @field:ColorInt @param:ColorInt val textPrimary: Int,
    @field:ColorInt @param:ColorInt val textSecondary: Int,
    @field:ColorInt @param:ColorInt val accent: Int,
    @field:ColorInt @param:ColorInt val font: Int = if (ThemeApplier.isLight(accent)) Color.BLACK else Color.WHITE,
    val isCustom: Boolean = false
)
