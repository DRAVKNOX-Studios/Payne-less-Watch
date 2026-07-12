package com.timely.msminutes.ui.settings

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class SettingsActivity : AppCompatActivity(), ThemeListener {
    private var prefs: Prefs? = null
    private var adapter: SettingsAdapter? = null
    private var recycler: RecyclerView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        prefs = Prefs(this)
        
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        recycler = findViewById(R.id.settings_recycler)
        recycler?.layoutManager = LinearLayoutManager(this)
        
        setupAdapter()
    }

    private fun setupAdapter() {
        val p = prefs ?: return
        val items = listOf(
            SettingsAdapter.SettingItem.Header("ALARM"),
            SettingsAdapter.SettingItem.Toggle(1, "24-hour format", p.is24Hour()) {
                p.set24Hour(it)
                notifyUpdate(this)
            },
            SettingsAdapter.SettingItem.Toggle(2, "Gradual volume", p.isGradualVolumeDefault) {
                p.isGradualVolumeDefault = it
            },
            SettingsAdapter.SettingItem.Header("SNOOZE"),
            SettingsAdapter.SettingItem.Duration(5, "Snooze duration", p.defaultSnoozeMinutes) {
                p.defaultSnoozeMinutes = it
            },
            SettingsAdapter.SettingItem.Header("WIDGET"),
            SettingsAdapter.SettingItem.Edit(3, "Note", p.widgetNote ?: "") {
                p.widgetNote = it
                notifyUpdate(this)
            },
            SettingsAdapter.SettingItem.Toggle(4, "Transparent background", p.isWidgetTransparent) {
                p.isWidgetTransparent = it
                notifyUpdate(this)
            },
            SettingsAdapter.SettingItem.Header("THEME"),
            SettingsAdapter.SettingItem.Toggle(6, "Use Custom Colors", p.isCustomTheme) {
                p.isCustomTheme = it
                notifyUpdate(this)
                ThemeStore.get().refresh()
            },
            SettingsAdapter.SettingItem.ColorPicker(7, "Background", p.backgroundColor) {
                p.backgroundColor = it
                if (p.isCustomTheme) {
                    p.fontColor = if (ThemeApplier.isLight(it)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                notifyUpdate(this)
                ThemeStore.get().refresh()
            },
            SettingsAdapter.SettingItem.ColorPicker(8, "Accent", p.accentColor) {
                p.accentColor = it
                notifyUpdate(this)
                ThemeStore.get().refresh()
            },
            SettingsAdapter.SettingItem.ColorPicker(9, "Text", p.fontColor) {
                p.fontColor = it
                notifyUpdate(this)
                ThemeStore.get().refresh()
            }
        )
        
        adapter = SettingsAdapter(items)
        recycler?.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null) return
        ThemeApplier.applyWindow(window, t)
        findViewById<View>(R.id.settings_root).setBackgroundColor(t.background)
        adapter?.setTokens(t)
    }

    override fun onDestroy() {
        super.onDestroy()
        recycler?.adapter = null
        recycler = null
        adapter = null
        prefs = null
    }
}
