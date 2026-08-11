package com.timely.msminutes.ui.settings

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.ToolbarRenderer
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.ColorPickerItemRenderer
import com.timely.msminutes.ui.canvas.items.DurationPickerItemRenderer
import com.timely.msminutes.ui.canvas.items.HeaderItemRenderer
import com.timely.msminutes.ui.canvas.items.InlineEditItemRenderer
import com.timely.msminutes.ui.canvas.items.SelectorItemRenderer
import com.timely.msminutes.ui.canvas.items.SoundItemRenderer
import com.timely.msminutes.ui.canvas.items.ToggleItemRenderer
import com.timely.msminutes.ui.view.CustomColorPickerDialog
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class SettingsActivity : AppCompatActivity(), ThemeListener {
    private var prefs: Prefs? = null
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var toolbarRenderer: ToolbarRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = FrameLayout(this)
        setContentView(root)

        hostView = CanvasHostView(this)
        root.addView(hostView)

        toolbarRenderer = ToolbarRenderer(this, "Settings", showBack = true) {
            finish()
        }
        hostView.addRenderer(toolbarRenderer)

        listView = CanvasListView(this, hostView) {}
        hostView.addRenderer(listView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val w = root.width.toFloat()
            val h = root.height.toFloat()
            if (w > 0 && h > 0) {
                val d = resources.displayMetrics.density
                val toolbarH = 56f * d
                toolbarRenderer.onLayout(0f, systemBars.top.toFloat(), w, systemBars.top.toFloat() + toolbarH)
                listView.onLayout(0f, systemBars.top.toFloat() + toolbarH, w, h - systemBars.bottom.toFloat())
                reload()
            }
            insets
        }

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            ViewCompat.requestApplyInsets(root)
        }
    }

    private fun reload() {
        val p = prefs ?: return
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()
        
        items.add(HeaderItemRenderer(this, "ALARM"))
        items.add(ToggleItemRenderer(this, "24-hour format", p.is24Hour()) {
            p.set24Hour(it)
            notifyUpdate(this)
        })
        items.add(ToggleItemRenderer(this, "Gradual volume", p.isGradualVolumeDefault) {
            p.isGradualVolumeDefault = it
        })
        
        val powerActions = listOf("Nothing", "Snooze", "Dismiss")
        items.add(SelectorItemRenderer(this, "Power button", powerActions.getOrElse(p.powerButtonAction) { "Snooze" }) {
            showCanvasSelector("Power button", powerActions, p.powerButtonAction) { index ->
                p.powerButtonAction = index
                reload()
            }
        })

        items.add(HeaderItemRenderer(this, "SNOOZE"))
        items.add(DurationPickerItemRenderer(this, "Snooze duration", p.defaultSnoozeMinutes) {
            p.defaultSnoozeMinutes = it
        })

        items.add(HeaderItemRenderer(this, "WIDGET"))
        items.add(InlineEditItemRenderer(this, hostView, listView, "Note", p.widgetNote ?: "", "Enter widget note") {
            p.widgetNote = it
            notifyUpdate(this)
        })
        items.add(ToggleItemRenderer(this, "Transparent background", p.isWidgetTransparent) {
            p.isWidgetTransparent = it
            notifyUpdate(this)
        })

        items.add(HeaderItemRenderer(this, "THEME"))
        items.add(ToggleItemRenderer(this, "Use Custom Colors", p.isCustomTheme) {
            p.isCustomTheme = it
            notifyUpdate(this)
            ThemeStore.get().refresh()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                reload()
            }
        })

        items.add(ColorPickerItemRenderer(this, "Background", p.backgroundColor) {
            showCustomColorPicker(p.backgroundColor) {
                p.backgroundColor = it
                if (p.isCustomTheme) {
                    p.fontColor = if (ThemeApplier.isLight(it)) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
                notifyUpdate(this)
                ThemeStore.get().refresh()
                reload()
            }
        })

        items.add(ColorPickerItemRenderer(this, "Accent", p.accentColor) {
            showCustomColorPicker(p.accentColor) {
                p.accentColor = it
                notifyUpdate(this)
                ThemeStore.get().refresh()
                reload()
            }
        })

        items.add(ColorPickerItemRenderer(this, "Text", p.fontColor) {
            showCustomColorPicker(p.fontColor) {
                p.fontColor = it
                notifyUpdate(this)
                ThemeStore.get().refresh()
                reload()
            }
        })

        listView.setItems(items)
        hostView.invalidate()
    }

    private fun showCanvasSelector(title: String, options: List<String>, current: Int, onSelected: (Int) -> Unit) {
        com.timely.msminutes.ui.canvas.CanvasDialog(this) { d, lv ->
            val dItems = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()
            dItems.add(HeaderItemRenderer(this, title))
            for (i in options.indices) {
                dItems.add(SoundItemRenderer(this, options[i], i == current) {
                    onSelected(i)
                    d.dismiss()
                })
            }
            dItems.add(ButtonItemRenderer(this, "Cancel", isDanger = true) {
                d.dismiss()
            })
            lv.setItems(dItems)
        }.show()
    }

    private fun showCustomColorPicker(initialColor: Int, onSelected: (Int) -> Unit) {
        CustomColorPickerDialog(this, initialColor) { color ->
            onSelected(color)
        }.show()
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
        val root = findViewById<android.view.View>(android.R.id.content)
        root?.setBackgroundColor(t.background)
        hostView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs = null
    }
}
