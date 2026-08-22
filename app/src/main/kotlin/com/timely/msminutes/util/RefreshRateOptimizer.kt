package com.timely.msminutes.util

import android.os.Build
import android.view.Window
import android.view.WindowManager

object RefreshRateOptimizer {

    /**
     * Attempts to optimize the window's refresh rate for smoother scrolling.
     * Sets the preferred refresh rate to the display's maximum.
     */
    fun optimize(window: Window) {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window.context.display
            } else {
                @Suppress("DEPRECATION")
                window.context.getSystemService(WindowManager::class.java).defaultDisplay
            }

            val maxRefreshRate = display?.supportedModes?.maxByOrNull { it.refreshRate }?.refreshRate ?: 0f
            
            if (maxRefreshRate > 0f) {
                val params = window.attributes
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    val mode = display?.supportedModes?.maxByOrNull { it.refreshRate }
                    if (mode != null) {
                        params.preferredDisplayModeId = mode.modeId
                    }
                } else {
                    @Suppress("DEPRECATION")
                    params.preferredRefreshRate = maxRefreshRate
                }
                window.attributes = params
            }
        } catch (e: Exception) {
            // Ignore if not supported
        }
    }
}
