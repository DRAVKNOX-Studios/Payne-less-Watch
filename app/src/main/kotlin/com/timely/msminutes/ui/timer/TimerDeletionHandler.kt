package com.timely.msminutes.ui.timer

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.ui.view.UndoBarController
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.TimerScheduler
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class TimerDeletionHandler(
    private val context: Context,
    private val repository: TimerRepository,
    private val undoController: UndoBarController?,
    private val adapter: TimerAdapter?,
    private val onReload: () -> Unit,
    private val onUpdateVisibility: (Boolean) -> Unit
) {
    private val handler = Handler(Looper.getMainLooper())
    private val pendingDeletes = HashMap<Long, Pair<TimerItem, Runnable>>()

    fun stageDelete(item: TimerItem) {
        pendingDeletes.remove(item.id)?.let { handler.removeCallbacks(it.second) }

        val current = (0 until (adapter?.itemCount ?: 0))
            .mapNotNull { i: Int -> adapter?.getItem(i) }
            .filter { t: TimerItem -> t.id != item.id }
            .toMutableList()
        adapter?.submit(current)
        onUpdateVisibility(current.isEmpty())

        val deleteRunnable = Runnable {
            pendingDeletes.remove(item.id)
            commitDelete(item)
            if (pendingDeletes.isEmpty()) undoController?.hide()
        }
        pendingDeletes[item.id] = Pair(item, deleteRunnable)
        handler.postDelayed(deleteRunnable, UNDO_WINDOW_MS)

        undoController?.show("Timer deleted", UNDO_WINDOW_MS)
    }

    fun onUndoClicked() {
        pendingDeletes.forEach { (_, pair) -> handler.removeCallbacks(pair.second) }
        pendingDeletes.clear()
        undoController?.hide()
        onReload()
    }

    fun commitDelete(item: TimerItem) {
        AppExecutors.get().diskIO {
            TimerScheduler.cancel(context, item.id)
            repository.delete(item.id)
            AppExecutors.get().mainThread {
                notifyUpdate(context)
                onReload()
            }
        }
    }

    fun handlePause() {
        undoController?.hide()
        val itemsToCommit = pendingDeletes.values.map { it.first }
        pendingDeletes.clear()
        
        if (itemsToCommit.isNotEmpty()) {
            AppExecutors.get().diskIO {
                itemsToCommit.forEach { item ->
                    TimerScheduler.cancel(context, item.id)
                    repository.delete(item.id)
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
