package com.timely.msminutes.ui.worldclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.TextRenderer
import com.timely.msminutes.ui.canvas.items.WorldClockItemRenderer
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens

class WorldClockFragment : Fragment(), ThemeListener {

    private val handler = Handler(Looper.getMainLooper())
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var emptyRenderer: TextRenderer

    private val tick = object : Runnable {
        override fun run() {
            hostView.invalidate()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hostView = CanvasHostView(requireContext())
        emptyRenderer = TextRenderer(requireContext(), "No world clocks")
        listView = CanvasListView(requireContext(), hostView) { isEmpty ->
            emptyRenderer.isVisible = isEmpty
            hostView.invalidate()
        }
        hostView.addRenderer(listView)
        hostView.addRenderer(emptyRenderer)
        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initDefaultsIfFirstLaunch()
        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = (right - left).toFloat()
            val h = (bottom - top).toFloat()
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            listView.onLayout(0f, 0f, w, h)
            emptyRenderer.onLayout(0f, 0f, w, h)
            reload()
        }
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
        hostView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        reload()
        handler.removeCallbacks(tick)
        handler.post(tick)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    private fun initDefaultsIfFirstLaunch() {
        val prefs = Prefs(requireContext())
        if (!prefs.hasWorldClocks()) {
            prefs.worldClocks = DEFAULT_ZONES.toSet()
        }
    }

    private fun reload() {
        val prefs = Prefs(requireContext())
        val zones = prefs.worldClocks
        val is24h = prefs.is24Hour()
        val renderers = zones.map { zone ->
            WorldClockItemRenderer(requireContext(), zone, is24h) {
                stageDeleteClock(zone)
            }
        }
        listView.setItems(renderers)
        hostView.invalidate()
    }

    private fun stageDeleteClock(zoneId: String) {
        val prefs = Prefs(requireContext())
        val original = prefs.worldClocks
        prefs.removeWorldClock(zoneId)
        reload()

        val main = activity as? com.timely.msminutes.ui.MainActivity
        main?.showUndo("Clock removed") {
            prefs.worldClocks = original
            reload()
        }
    }

    companion object {
        val DEFAULT_ZONES = listOf(
            "America/Anchorage",
            "America/Los_Angeles",
            "America/Denver",
            "America/Chicago",
            "America/New_York",
            "America/Halifax",
            "America/Sao_Paulo",
            "America/Argentina/Buenos_Aires",
            "UTC",
            "Europe/London",
            "Europe/Lisbon",
            "Europe/Paris",
            "Europe/Berlin",
            "Europe/Rome",
            "Europe/Helsinki",
            "Europe/Moscow",
            "Africa/Cairo",
            "Africa/Nairobi",
            "Africa/Johannesburg",
            "Asia/Dubai",
            "Asia/Karachi",
            "Asia/Kolkata",
            "Asia/Dhaka",
            "Asia/Bangkok",
            "Asia/Singapore",
            "Asia/Shanghai",
            "Asia/Tokyo",
            "Asia/Seoul",
            "Australia/Sydney",
            "Pacific/Auckland"
        )
    }
}
