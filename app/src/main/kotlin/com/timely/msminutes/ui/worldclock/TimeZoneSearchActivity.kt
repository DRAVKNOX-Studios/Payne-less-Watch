package com.timely.msminutes.ui.worldclock

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
import com.timely.msminutes.ui.canvas.items.SearchItemRenderer
import com.timely.msminutes.ui.canvas.items.TimeZoneItemRenderer
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import java.time.ZoneId
import java.util.Locale

class TimeZoneSearchActivity : AppCompatActivity(), ThemeStore.ThemeListener {

    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var toolbarRenderer: ToolbarRenderer
    
    private var searchQuery: String = ""
    private val allZones = ZoneId.getAvailableZoneIds().sorted().map { id ->
        val parts = id.split("/")
        TimeZoneItem(
            id = id,
            city = parts.last().replace("_", " "),
            region = if (parts.size > 1) parts.first() else "Other"
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val root = FrameLayout(this)
        setContentView(root)

        hostView = CanvasHostView(this)
        root.addView(hostView)

        toolbarRenderer = ToolbarRenderer(this, "Select City", showBack = true) {
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
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()
        
        items.add(SearchItemRenderer(this, hostView, listView, "Search City", searchQuery) { q ->
            if (q != searchQuery) {
                searchQuery = q
                reload()
            }
        })

        val filtered = if (searchQuery.isEmpty()) {
            allZones
        } else {
            val lower = searchQuery.lowercase(Locale.ROOT)
            allZones.filter {
                it.city.lowercase(Locale.ROOT).contains(lower) ||
                        it.region.lowercase(Locale.ROOT).contains(lower) ||
                        it.id.lowercase(Locale.ROOT).contains(lower)
            }
        }

        for (zone in filtered) {
            items.add(TimeZoneItemRenderer(this, zone.city, zone.region) {
                Prefs(this).addWorldClock(zone.id)
                finish()
            })
        }

        listView.setItems(items)
        hostView.invalidate()
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

    data class TimeZoneItem(val id: String, val city: String, val region: String)
}
