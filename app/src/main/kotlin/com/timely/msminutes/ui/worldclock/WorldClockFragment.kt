package com.timely.msminutes.ui.worldclock

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens

class WorldClockFragment : Fragment(), ThemeListener {

    private val handler = Handler(Looper.getMainLooper())
    private var adapter: WorldClockAdapter? = null
    private var root: View? = null

    private val tick = object : Runnable {
        override fun run() {
            adapter?.tick()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v     = inflater.inflate(R.layout.fragment_world_clock, container, false)
        root      = v
        val prefs = Prefs(requireContext())

        val rv = v.findViewById<RecyclerView>(R.id.world_clock_list)
        (rv.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.setHasFixedSize(true)
        rv.setItemViewCacheSize(0)

        (requireActivity() as? MainActivity)?.let { rv.setRecycledViewPool(it.getSharedViewPool()) }

        adapter = WorldClockAdapter(DEFAULT_ZONES, prefs.is24Hour()).also {
            it.setHasStableIds(true)
        }
        ThemeStore.get().current()?.let {
            adapter?.setTokens(it)
            ThemeApplier.applyScrollbar(rv, it)
        }
        rv.adapter = adapter
        return v
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
        if (t == null || !isAdded) return
        adapter?.setTokens(t)
        view?.findViewById<RecyclerView>(R.id.world_clock_list)?.let {
            ThemeApplier.applyScrollbar(it, t)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter?.setIs24Hour(Prefs(requireContext()).is24Hour())
        handler.removeCallbacks(tick)
        val delay = 1000L - (System.currentTimeMillis() % 1000L)
        handler.postDelayed(tick, delay)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tick)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(tick)
        view?.findViewById<RecyclerView>(R.id.world_clock_list)?.adapter = null
        adapter?.shutdown()
        adapter = null
        root = null
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
