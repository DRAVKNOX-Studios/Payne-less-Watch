package com.timely.msminutes.ui.alarm

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.view.SharedViewHolder
import com.timely.msminutes.ui.view.SwipeToDeleteCallback
import com.timely.msminutes.ui.view.UndoBarController
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AlarmTimeUtil
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class AlarmFragment : Fragment(), ThemeListener {
    private var mRoot: View? = null
    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var adapter: AlarmAdapter? = null
    private var repository: AlarmRepository? = null
    private var undoController: UndoBarController? = null
    private var deletionHandler: AlarmDeletionHandler? = null

    private val handler = Handler(Looper.getMainLooper())

    private val ticker: Runnable = object : Runnable {
        override fun run() {
            updateRemainingTimes()
            handler.postDelayed(this, 1000L)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRoot = inflater.inflate(R.layout.fragment_alarm, container, false)
        recyclerView = mRoot!!.findViewById(R.id.recycler_alarms)
        emptyView    = mRoot!!.findViewById(R.id.empty_view)

        val main = requireActivity() as MainActivity
        val fab = main.getSharedFab()
        undoController = main.getSharedUndoController()
        undoController?.setUndoListener { onUndoClicked() }

        recyclerView!!.setRecycledViewPool(main.getSharedViewPool())
        recyclerView!!.clipChildren = false
        recyclerView!!.setItemViewCacheSize(0)

        repository = AlarmRepository(requireContext())
        recyclerView!!.layoutManager = LinearLayoutManager(requireContext())

        adapter = AlarmAdapter(object : AlarmAdapter.Listener {
            override fun onToggle(alarm: Alarm?, enabled: Boolean) {
                if (alarm == null) return
                alarm.isEnabled = enabled
                AppExecutors.get().diskIO {
                    repository?.update(alarm)
                    AppExecutors.get().mainThread {
                        if (!isAdded) return@mainThread
                        if (enabled) {
                            AlarmScheduler.schedule(requireContext(), alarm)
                            val remaining = AlarmTimeUtil.getRemainingTimeText(alarm)
                            if (remaining != null) android.widget.Toast
                                .makeText(requireContext(), remaining, android.widget.Toast.LENGTH_LONG)
                                .show()
                        } else {
                            AlarmScheduler.cancel(requireContext(), alarm.id)
                        }
                        notifyUpdate(requireContext())
                        reload()
                    }
                }
            }

            override fun onClick(alarm: Alarm?) {
                if (alarm == null) return
                val intent = Intent(requireContext(), AlarmEditActivity::class.java)
                intent.putExtra(AlarmEditActivity.EXTRA_ALARM_ID, alarm.id)
                startActivity(intent)
            }

            override fun onDelete(alarm: Alarm?) {
                if (alarm == null) return
                deletionHandler?.commitDelete(alarm)
            }
        })

        deletionHandler = AlarmDeletionHandler(
            requireContext(), repository!!, undoController, adapter,
            onReload = { reload() },
            onUpdateVisibility = { isEmpty -> updateEmptyVisibility(isEmpty) }
        )

        recyclerView!!.adapter = adapter
        attachSwipeToDelete()

        fab!!.setOnClickListener {
            startActivity(Intent(requireContext(), AlarmEditActivity::class.java))
        }

        return mRoot
    }

    private fun attachSwipeToDelete() {
        val rv = recyclerView ?: return
        ItemTouchHelper(SwipeToDeleteCallback { pos ->
            val alarm = adapter?.getItem(pos) ?: return@SwipeToDeleteCallback
            handler.post { deletionHandler?.stageDelete(alarm) }
        }).attachToRecyclerView(rv)
    }

    private fun onUndoClicked() {
        deletionHandler?.onUndoClicked()
    }

    private fun updateEmptyVisibility(isEmpty: Boolean) {
        emptyView?.visibility    = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (isEmpty) View.GONE    else View.VISIBLE
    }

    override fun onStart()  { super.onStart();  ThemeStore.get().subscribe(this) }
    override fun onStop()   { super.onStop();   ThemeStore.get().unsubscribe(this) }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null || mRoot == null || !isAdded) return
        adapter?.setTokens(t)
        recyclerView?.let { ThemeApplier.applyScrollbar(it, t) }
    }

    override fun onResume() {
        super.onResume()
        reload()
        
        val main = requireActivity() as MainActivity
        main.getSharedFab()?.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                startActivity(Intent(requireContext(), AlarmEditActivity::class.java))
            }
        }

        handler.removeCallbacks(ticker)
        handler.post(ticker)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(ticker)
        (requireActivity() as? MainActivity)?.getSharedFab()?.visibility = View.GONE

        deletionHandler?.handlePause()
    }

    private fun reload() {
        repository?.getAllAsync { alarms ->
            if (!isAdded) return@getAllAsync
            val list = alarms ?: mutableListOf()
            val filtered = list.filter { deletionHandler?.isPending(it.id) == false }.toMutableList()
            adapter?.submit(filtered)
            updateEmptyVisibility(filtered.isEmpty())
        }
    }

    private fun updateRemainingTimes() {
        val rv = recyclerView ?: return
        val ad = adapter ?: return
        for (i in 0 until rv.childCount) {
            val child  = rv.getChildAt(i)
            val holder = rv.getChildViewHolder(child) as? SharedViewHolder ?: continue
            val pos    = rv.getChildAdapterPosition(child)
            if (pos == RecyclerView.NO_POSITION || pos >= ad.itemCount) continue
            val alarm  = ad.getItem(pos) ?: continue
            val remaining = AlarmTimeUtil.getRemainingTimeText(alarm)
            holder.accentText?.text       = remaining ?: ""
            holder.accentText?.visibility = if (remaining != null) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView?.adapter = null
        mRoot = null
        recyclerView = null
        emptyView = null
        adapter = null
        repository = null
        undoController = null
    }
}
