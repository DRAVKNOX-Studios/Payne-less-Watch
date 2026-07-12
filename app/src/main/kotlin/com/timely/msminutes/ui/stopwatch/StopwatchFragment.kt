package com.timely.msminutes.ui.stopwatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.LapStore
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.service.StopwatchService
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class StopwatchFragment : Fragment(), ThemeListener {

    private var timeText: TextView? = null
    private var startPause: Button? = null
    private var lapReset: Button? = null
    private var lapsRecycler: RecyclerView? = null
    private var lapAdapter: LapAdapter? = null

    private var prefs: Prefs? = null
    private var mTokens: ThemeTokens? = null
    private val laps: MutableList<String?> = ArrayList()

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable: Runnable = object : Runnable {
        override fun run() {
            updateTimeDisplay()
            tickHandler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_stopwatch, container, false)
        timeText     = root.findViewById(R.id.text_stopwatch_time)
        startPause   = root.findViewById(R.id.btn_stopwatch_start_pause)
        lapReset     = root.findViewById(R.id.btn_stopwatch_lap_reset)
        lapsRecycler = root.findViewById(R.id.recycler_laps)

        val p = Prefs(requireContext())
        prefs = p

        lapsRecycler!!.layoutManager = LinearLayoutManager(requireContext())
        lapsRecycler!!.setItemViewCacheSize(0)
        
        (requireActivity() as? MainActivity)?.let { lapsRecycler!!.setRecycledViewPool(it.getSharedViewPool()) }

        lapAdapter = LapAdapter(laps)
        lapsRecycler!!.adapter = lapAdapter

        restoreLaps()

        startPause!!.setOnClickListener { if (p.isStopwatchRunning) pause() else start() }
        lapReset!!.setOnClickListener   { if (p.isStopwatchRunning) addLap() else reset() }

        ThemeStore.get().current()?.let { t ->
            applyTheme(t)
        }

        updateButtons()
        updateTimeDisplay()

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        tickHandler.removeCallbacksAndMessages(null)
        lapsRecycler?.adapter = null
        timeText     = null
        startPause   = null
        lapReset     = null
        lapsRecycler = null
        lapAdapter   = null
        prefs = null
        mTokens = null
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
    }

    override fun onResume() {
        super.onResume()
        updateButtons()
        updateTimeDisplay()
        tickHandler.removeCallbacks(tickRunnable)
        if (prefs?.isStopwatchRunning == true) {
            tickHandler.postDelayed(tickRunnable, TICK_MS)
        }
    }

    override fun onPause() {
        super.onPause()
        tickHandler.removeCallbacks(tickRunnable)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null) return
        mTokens = t
        timeText?.setTextColor(t.textPrimary)
        lapAdapter?.setTokens(t)
        applyTheme(t)
    }

    private fun applyTheme(t: ThemeTokens) {
        mTokens = t
        timeText?.setTextColor(t.textPrimary)
        ThemeApplier.applyAccentButton(startPause, t)
        ThemeApplier.applyAccentButton(lapReset, t)
        lapsRecycler?.let { ThemeApplier.applyScrollbar(it, t) }
    }

    private fun updateTimeDisplay() {
        timeText?.text = TimeFormatUtil.formatStopwatch(currentElapsed())
    }

    private fun restoreLaps() {
        laps.clear()
        laps.addAll(LapStore.decode(prefs?.stopwatchLaps))
        lapAdapter?.notifyDataSetChanged()
    }

    private fun saveLaps() {
        prefs?.stopwatchLaps = LapStore.encode(laps)
    }

    private fun start() {
        val p = prefs ?: return
        p.stopwatchStartBase = System.currentTimeMillis()
        p.isStopwatchRunning = true
        requireContext().startForegroundService(
            Intent(requireContext(), StopwatchService::class.java)
                .setAction(StopwatchService.ACTION_START)
        )
        updateButtons()
        tickHandler.postDelayed(tickRunnable, TICK_MS)
        notifyUpdate(requireContext())
    }

    private fun pause() {
        val p = prefs ?: return
        p.lastStopwatchElapsed += System.currentTimeMillis() - p.stopwatchStartBase
        p.isStopwatchRunning = false
        requireContext().startForegroundService(
            Intent(requireContext(), StopwatchService::class.java)
                .setAction(StopwatchService.ACTION_STOP)
        )
        updateButtons()
        tickHandler.removeCallbacks(tickRunnable)
        updateTimeDisplay()
        notifyUpdate(requireContext())
    }

    private fun reset() {
        val p = prefs ?: return
        p.lastStopwatchElapsed = 0
        p.stopwatchStartBase   = 0
        laps.clear()
        saveLaps()
        lapAdapter?.notifyDataSetChanged()
        updateTimeDisplay()
        notifyUpdate(requireContext())
    }

    private fun addLap() {
        laps.add(0, TimeFormatUtil.formatStopwatch(currentElapsed()))
        lapAdapter?.notifyItemInserted(0)
        saveLaps()
    }

    private fun currentElapsed(): Long {
        val p = prefs ?: return 0
        return if (p.isStopwatchRunning)
            p.lastStopwatchElapsed + (System.currentTimeMillis() - p.stopwatchStartBase)
        else
            p.lastStopwatchElapsed
    }

    private fun updateButtons() {
        val running = prefs?.isStopwatchRunning == true
        startPause?.text = if (running) "Pause" else "Start"
        lapReset?.text   = if (running) "Lap"   else "Reset"
    }

    companion object {
        private const val TICK_MS = 33L
    }
}
