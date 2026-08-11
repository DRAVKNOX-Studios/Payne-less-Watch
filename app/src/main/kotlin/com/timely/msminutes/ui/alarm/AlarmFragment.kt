package com.timely.msminutes.ui.alarm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.items.AlarmItemRenderer
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class AlarmFragment : Fragment(), ThemeListener {
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var emptyRenderer: com.timely.msminutes.ui.canvas.TextRenderer
    private var repository: AlarmRepository? = null

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            hostView.invalidate()
            handler.postDelayed(this, 500L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hostView = CanvasHostView(requireContext())
        hostView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        emptyRenderer = com.timely.msminutes.ui.canvas.TextRenderer(requireContext(), "No alarms yet")
        listView = CanvasListView(requireContext(), hostView) { isEmpty ->
            emptyRenderer.isVisible = isEmpty
            hostView.invalidate()
        }
        hostView.addRenderer(listView)
        hostView.addRenderer(emptyRenderer)

        repository = AlarmRepository(requireContext())

        return hostView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        handler.post(tickRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
    }

    private fun reload() {
        repository?.getAllAsync { alarms ->
            if (!isAdded) return@getAllAsync
            val list = alarms?.filter { !AlarmRepository.pendingDeletions.contains(it.id) } ?: mutableListOf()
            val renderers = list.map { alarm ->
                AlarmItemRenderer(requireContext(), alarm, 
                    onToggle = { enabled -> toggleAlarm(alarm, enabled) },
                    onClickAction = { editAlarm(alarm) },
                    onDeleteAction = { stageDeleteAlarm(alarm) },
                    onCopyAction = { copyAlarm(alarm) }
                )
            }
            listView.setItems(renderers)
            hostView.invalidate()
        }
    }

    private fun copyAlarm(alarm: Alarm) {
        val newAlarm = Alarm().apply {
            hour = alarm.hour
            minute = alarm.minute
            repeatDays = alarm.repeatDays
            isEnabled = false
            isVibrate = alarm.isVibrate
            soundUri = alarm.soundUri
            label = if (alarm.label != null) "${alarm.label} (Copy)" else "Alarm (Copy)"
            note = alarm.note
            isGradualVolume = alarm.isGradualVolume
            snoozeMinutes = alarm.snoozeMinutes
        }
        AppExecutors.get().diskIO {
            val newId = repository?.insert(newAlarm) ?: -1L
            AppExecutors.get().mainThread {
                if (isAdded) {
                    reload()
                    val main = activity as? MainActivity
                    main?.showUndo("Alarm copied") {
                        main.hideUndo()
                        if (newId != -1L) {
                            AppExecutors.get().diskIO {
                                repository?.delete(newId)
                                AppExecutors.get().mainThread {
                                    if (isAdded) {
                                        notifyUpdate(requireContext())
                                        reload()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun stageDeleteAlarm(alarm: Alarm) {
        AlarmRepository.pendingDeletions.add(alarm.id)
        reload()
        
        val main = activity as? MainActivity
        val appContext = requireContext().applicationContext
        main?.showUndo("Alarm deleted") {
            AlarmRepository.pendingDeletions.remove(alarm.id)
            reload()
        }
        
        // Finalize deletion after 5 seconds if not undone
        handler.postDelayed({
            if (AlarmRepository.pendingDeletions.contains(alarm.id)) {
                commitDeleteAlarm(alarm, appContext)
            }
        }, 5000L)
    }

    private fun commitDeleteAlarm(alarm: Alarm, appContext: android.content.Context) {
        AppExecutors.get().diskIO {
            repository?.delete(alarm.id)
            AlarmRepository.pendingDeletions.remove(alarm.id)
            AppExecutors.get().mainThread {
                AlarmScheduler.cancel(appContext, alarm.id)
                notifyUpdate(appContext)
                if (isAdded) reload()
            }
        }
    }

    private fun toggleAlarm(alarm: Alarm, enabled: Boolean) {
        alarm.isEnabled = enabled
        AppExecutors.get().diskIO {
            repository?.update(alarm)
            AppExecutors.get().mainThread {
                if (!isAdded) return@mainThread
                if (enabled) {
                    AlarmScheduler.schedule(requireContext(), alarm)
                } else {
                    AlarmScheduler.cancel(requireContext(), alarm.id)
                }
                notifyUpdate(requireContext())
                reload()
            }
        }
    }

    private fun editAlarm(alarm: Alarm) {
        val intent = Intent(requireContext(), AlarmEditActivity::class.java)
        intent.putExtra(AlarmEditActivity.EXTRA_ALARM_ID, alarm.id)
        startActivity(intent)
    }
}
