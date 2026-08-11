package com.timely.msminutes.ui.stopwatch

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.timely.msminutes.data.LapStore
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.service.StopwatchService
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.StopwatchHeaderRenderer
import com.timely.msminutes.ui.canvas.items.LapItemRenderer
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class StopwatchFragment : Fragment(), ThemeListener {

    private lateinit var hostView: CanvasHostView
    private lateinit var headerRenderer: StopwatchHeaderRenderer
    private lateinit var listView: CanvasListView

    private var prefs: Prefs? = null
    private val laps: MutableList<String?> = ArrayList()

    private val tickHandler = Handler(Looper.getMainLooper())
    private val tickRunnable: Runnable = object : Runnable {
        override fun run() {
            updateUI()
            tickHandler.postDelayed(this, TICK_MS)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hostView = CanvasHostView(requireContext())
        headerRenderer = StopwatchHeaderRenderer(
            requireContext(),
            onStartPause = { if (prefs?.isStopwatchRunning == true) pause() else start() },
            onLapReset = { if (prefs?.isStopwatchRunning == true) addLap() else reset() }
        )
        listView = CanvasListView(requireContext(), hostView) { }
        
        hostView.addRenderer(headerRenderer)
        hostView.addRenderer(listView)

        prefs = Prefs(requireContext())
        restoreLaps()

        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = (right - left).toFloat()
            val h = (bottom - top).toFloat()
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            
            val d = resources.displayMetrics.density
            val headerH = 150f * d
            headerRenderer.onLayout(0f, 0f, w, headerH)
            listView.onLayout(0f, headerH, w, h)
            reloadLaps()
        }
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
        tickHandler.removeCallbacks(tickRunnable)
    }

    override fun onResume() {
        super.onResume()
        updateUI()
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
        hostView.invalidate()
    }

    private fun updateUI() {
        headerRenderer.timeText = TimeFormatUtil.formatStopwatch(currentElapsed())
        val running = prefs?.isStopwatchRunning == true
        headerRenderer.leftBtnText = if (running) "Pause" else "Start"
        headerRenderer.rightBtnText = if (running) "Lap" else "Reset"
        hostView.invalidate()
    }

    private fun reloadLaps() {
        val renderers = laps.filterNotNull().mapIndexed { index, time ->
            LapItemRenderer(requireContext(), laps.size - index, time)
        }
        listView.setItems(renderers)
        hostView.invalidate()
    }

    private fun restoreLaps() {
        laps.clear()
        laps.addAll(LapStore.decode(prefs?.stopwatchLaps))
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
        updateUI()
        tickHandler.postDelayed(tickRunnable, TICK_MS)
        notifyUpdate(requireContext())
    }

    private fun pause() {
        val p = prefs ?: return
        p.lastStopwatchElapsed += System.currentTimeMillis() - p.stopwatchStartBase
        p.isStopwatchRunning = false
        requireContext().startService(
            Intent(requireContext(), StopwatchService::class.java)
                .setAction(StopwatchService.ACTION_STOP)
        )
        updateUI()
        tickHandler.removeCallbacks(tickRunnable)
        notifyUpdate(requireContext())
    }

    private fun reset() {
        val p = prefs ?: return
        p.lastStopwatchElapsed = 0
        p.stopwatchStartBase   = 0
        laps.clear()
        saveLaps()
        reloadLaps()
        updateUI()
        notifyUpdate(requireContext())
    }

    private fun addLap() {
        laps.add(0, TimeFormatUtil.formatStopwatch(currentElapsed()))
        saveLaps()
        reloadLaps()
    }

    private fun currentElapsed(): Long {
        val p = prefs ?: return 0
        return if (p.isStopwatchRunning)
            p.lastStopwatchElapsed + (System.currentTimeMillis() - p.stopwatchStartBase)
        else
            p.lastStopwatchElapsed
    }

    companion object {
        private const val TICK_MS = 33L
    }
}
