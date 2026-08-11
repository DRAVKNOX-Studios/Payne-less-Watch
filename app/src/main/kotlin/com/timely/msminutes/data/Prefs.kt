package com.timely.msminutes.data

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.text.format.DateFormat

class Prefs(context: Context) {
    private val sp: SharedPreferences
    private val context: Context

    init {
        this.context = context.applicationContext
        sp = this.context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    }

    fun is24Hour(): Boolean {
        if (!sp.contains(KEY_24H_SET)) {
            return DateFormat.is24HourFormat(context)
        }
        return sp.getBoolean(KEY_24H_VALUE, true)
    }

    fun set24Hour(value: Boolean) {
        sp.edit()
            .putBoolean(KEY_24H_SET, true)
            .putBoolean(KEY_24H_VALUE, value)
            .apply()
    }

    var theme: String?
        get() = sp.getString("theme", "system")
        set(value) {
            sp.edit().putString("theme", value).apply()
        }

    var accentColor: Int
        get() {
            if (!this.isCustomTheme) return DEFAULT_ACCENT
            return sp.getInt("accent_color", DEFAULT_ACCENT)
        }
        set(color) {
            sp.edit().putInt("accent_color", color).apply()
        }

    var backgroundColor: Int
        get() {
            if (!this.isCustomTheme) {
                return if (this.isDarkMode) -0xedecea else -0x1
            }
            return sp.getInt("background_color", -0x1)
        }
        set(color) {
            sp.edit().putInt("background_color", color).apply()
        }

    var fontColor: Int
        get() {
            if (!this.isCustomTheme) {
                return if (this.isDarkMode) -0xd0c0b else -0xe5e3e2
            }
            return sp.getInt("font_color", -0x1000000)
        }
        set(color) {
            sp.edit().putInt("font_color", color).apply()
        }

    private val isDarkMode: Boolean
        get() {
            val nightModeFlags = context.resources
                .configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return nightModeFlags == Configuration.UI_MODE_NIGHT_YES
        }

    var isCustomTheme: Boolean
        get() = sp.getBoolean("custom_theme", false)
        set(custom) {
            sp.edit().putBoolean("custom_theme", custom).apply()
        }

    var isGradualVolumeDefault: Boolean
        get() = sp.getBoolean("gradual_volume_default", false)
        set(value) {
            sp.edit().putBoolean("gradual_volume_default", value).apply()
        }

    var powerButtonAction: Int
        get() = sp.getInt("power_button_action", POWER_ACTION_SNOOZE)
        set(action) {
            sp.edit().putInt("power_button_action", action).apply()
        }

    var defaultSnoozeMinutes: Int
        get() = sp.getInt("default_snooze_minutes", 10)
        set(minutes) {
            sp.edit().putInt("default_snooze_minutes", minutes).apply()
        }

    val alarmVolumeRampSeconds: Int
        get() = sp.getInt("volume_ramp_seconds", 30)

    var lastStopwatchElapsed: Long
        get() = sp.getLong("stopwatch_elapsed", 0)
        set(millis) {
            sp.edit().putLong("stopwatch_elapsed", millis).apply()
        }

    var isStopwatchRunning: Boolean
        get() = sp.getBoolean("stopwatch_running", false)
        set(running) {
            sp.edit().putBoolean("stopwatch_running", running).apply()
        }

    var stopwatchStartBase: Long
        get() = sp.getLong("stopwatch_start_base", 0)
        set(base) {
            sp.edit().putLong("stopwatch_start_base", base).apply()
        }

    var stopwatchLaps: String?
        get() = sp.getString("stopwatch_laps", "")
        set(csv) {
            sp.edit().putString("stopwatch_laps", csv).apply()
        }

    var widgetNote: String?
        get() = sp.getString("widget_note", "")
        set(note) {
            sp.edit().putString("widget_note", note).apply()
        }

    var isWidgetTransparent: Boolean
        get() = sp.getBoolean("widget_transparent", false)
        set(transparent) {
            sp.edit().putBoolean("widget_transparent", transparent).apply()
        }

    var customSounds: Set<String>
        get() = sp.getStringSet("custom_sounds", emptySet()) ?: emptySet()
        set(value) {
            sp.edit().putStringSet("custom_sounds", value).apply()
        }

    fun addCustomSound(uri: String) {
        val current = customSounds.toMutableSet()
        current.add(uri)
        customSounds = current
    }

    var worldClocks: Set<String>
        get() = sp.getStringSet("world_clocks", emptySet()) ?: emptySet()
        set(value) {
            sp.edit().putStringSet("world_clocks", value).apply()
        }

    /** Returns true if the world_clocks key has ever been written to prefs.
     *  An empty set is a valid user state (all clocks deleted) and returns true.
     *  Only returns false on a true first launch before any write has occurred. */
    fun hasWorldClocks(): Boolean = sp.contains("world_clocks")

    fun addWorldClock(zoneId: String) {
        val current = worldClocks.toMutableSet()
        current.add(zoneId)
        worldClocks = current
    }

    fun removeWorldClock(zoneId: String) {
        val current = worldClocks.toMutableSet()
        current.remove(zoneId)
        worldClocks = current
    }

    companion object {
        private const val FILE = "timely_prefs"
        private const val KEY_24H_SET   = "format_24h_set"
        private const val KEY_24H_VALUE = "format_24h"
        const val DEFAULT_ACCENT: Int = -0xa8de

        const val POWER_ACTION_NONE: Int = 0
        const val POWER_ACTION_SNOOZE: Int = 1
        const val POWER_ACTION_DISMISS: Int = 2
    }
}
