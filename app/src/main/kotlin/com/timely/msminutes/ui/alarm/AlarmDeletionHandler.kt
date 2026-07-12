package com.timely.msminutes.ui.alarm

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.ui.view.UndoBarController
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class AlarmDeletionHandler(
    private val context: Context,
    private val repository: AlarmRepository,
    private val undoController: UndoBarController?,
    private val adapter: AlarmAdapter?,
    private val onReload: () -> Unit,
    private val onUpdateVisibility: (Boolean) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val pendingDeletes = HashMap<Long, Pair<Alarm, Runnable>>()

    fun stageDelete(alarm: Alarm) {
        pendingDeletes.remove(alarm.id)?.let { handler.removeCallbacks(it.second) }

        val current = (0 until (adapter?.itemCount ?: 0))
            .mapNotNull { i: Int -> adapter?.getItem(i) }
            .filter { a: Alarm -> a.id != alarm.id }
            .toMutableList()
        adapter?.submit(current)
        onUpdateVisibility(current.isEmpty())

        val deleteRunnable = Runnable {
            pendingDeletes.remove(alarm.id)
            commitDelete(alarm)
            if (pendingDeletes.isEmpty()) undoController?.hide()
        }
        pendingDeletes[alarm.id] = Pair(alarm, deleteRunnable)
        handler.postDelayed(deleteRunnable, UNDO_WINDOW_MS)

        undoController?.show("Alarm deleted", UNDO_WINDOW_MS)
    }

    fun onUndoClicked() {
        pendingDeletes.forEach { (_, pair) -> handler.removeCallbacks(pair.second) }
        pendingDeletes.clear()
        undoController?.hide()
        onReload()
    }

    fun commitDelete(alarm: Alarm) {
        AppExecutors.get().diskIO {
            AlarmScheduler.cancel(context, alarm.id)
            repository.delete(alarm.id)
            AppExecutors.get().mainThread {
                notifyUpdate(context)
                onReload()
            }
        }
    }

    fun handlePause() {
        undoController?.hide()
        val alarmsToCommit = pendingDeletes.values.map { it.first }
        pendingDeletes.clear()
        
        if (alarmsToCommit.isNotEmpty()) {
            AppExecutors.get().diskIO {
                alarmsToCommit.forEach { alarm ->
                    AlarmScheduler.cancel(context, alarm.id)
                    repository.delete(alarm.id)
                }
                AppExecutors.get().mainThread {
                    notifyUpdate(context)
                }
            }
        }
    }

    fun isPending(id: Long): Boolean = pendingDeletes.containsKey(id)

    companion object {
        private const val UNDO_WINDOW_MS = 3_000L
    }
}
