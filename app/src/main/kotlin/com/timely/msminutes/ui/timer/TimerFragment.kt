package com.timely.msminutes.ui.timer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.timer.TimerCreateDialog.OnCreateListener
import com.timely.msminutes.ui.view.SwipeToDeleteCallback
import com.timely.msminutes.ui.view.UndoBarController
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class TimerFragment : Fragment(), ThemeListener {
    private var mRoot: View? = null
    private var recyclerView: RecyclerView? = null
    private var emptyView: View? = null
    private var adapter: TimerAdapter? = null
    private var repository: TimerRepository? = null
    private var currentDialog: TimerCreateDialog? = null
    private var undoController: UndoBarController? = null
    private var actionHandler: TimerActionHandler? = null
    private var deletionHandler: TimerDeletionHandler? = null

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable: Runnable = object : Runnable {
        override fun run() {
            adapter?.refreshTimes()
            handler.postDelayed(this, TICK_MS)
        }
    }

    private val customSoundLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null && currentDialog != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                currentDialog?.setSelectedSound(uri, getFileName(uri))
            }
        }
    }

    private fun getFileName(uri: Uri): String? {
        var name: String? = "Custom"
        try {
            requireContext().contentResolver
                .query(uri, null, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        name = cursor.getString(
                            cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)
                        )
                    }
                }
        } catch (ignored: Exception) {}
        return name
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mRoot = inflater.inflate(R.layout.fragment_timer, container, false)
        recyclerView = mRoot?.findViewById(R.id.recycler_timers)
        emptyView    = mRoot?.findViewById(R.id.empty_view)

        val main = requireActivity() as MainActivity
        undoController = main.getSharedUndoController()
        undoController?.setUndoListener { onUndoClicked() }

        recyclerView?.setRecycledViewPool(main.getSharedViewPool())
        recyclerView?.clipChildren = false
        recyclerView?.setItemViewCacheSize(0)

        val repo = TimerRepository(requireContext())
        repository = repo
        actionHandler = TimerActionHandler(requireContext(), repo) { reload() }

        recyclerView?.layoutManager = LinearLayoutManager(requireContext())

        adapter = TimerAdapter(object : TimerAdapter.Listener {
            override fun onPauseResume(item: TimerItem?) { if (item != null) actionHandler?.toggleTimer(item) }
            override fun onCancel(item: TimerItem?)      { if (item != null) deletionHandler?.commitDelete(item) }
            override fun onReset(item: TimerItem?)       { if (item != null) actionHandler?.resetTimer(item) }
        })

        deletionHandler = TimerDeletionHandler(
            requireContext(), repo, undoController, adapter,
            onReload = { reload() },
            onUpdateVisibility = { isEmpty -> updateEmptyVisibility(isEmpty) }
        )

        recyclerView?.adapter = adapter
        attachSwipeToDelete()

        return mRoot
    }

    private fun attachSwipeToDelete() {
        val rv = recyclerView ?: return
        ItemTouchHelper(SwipeToDeleteCallback { pos ->
            val item = adapter?.getItem(pos) ?: return@SwipeToDeleteCallback
            handler.post { deletionHandler?.stageDelete(item) }
        }).attachToRecyclerView(rv)
    }

    private fun onUndoClicked() {
        deletionHandler?.onUndoClicked()
    }

    private fun updateEmptyVisibility(isEmpty: Boolean) {
        emptyView?.visibility    = if (isEmpty) View.VISIBLE else View.GONE
        recyclerView?.visibility = if (isEmpty) View.GONE    else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        recyclerView?.adapter = null
        mRoot = null
        recyclerView = null
        emptyView = null
        adapter = null
        repository = null
        currentDialog?.dismiss()
        currentDialog = null
        undoController = null
        actionHandler = null
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
                showCreateDialog()
            }
        }

        handler.removeCallbacks(tickRunnable)
        handler.postDelayed(tickRunnable, TICK_MS)
    }

    private fun showCreateDialog() {
        if (currentDialog != null) return
        val repo = repository ?: return

        val dialog = TimerCreateDialog(
            requireContext(),
            object : OnCreateListener {
                override fun onCreate(item: TimerItem?) {
                    if (item == null) return
                    AppExecutors.get().diskIO {
                        repo.insert(item)
                        actionHandler?.startTimerAsync(item)
                        AppExecutors.get().mainThread {
                            if (!isAdded) return@mainThread
                            notifyUpdate(requireContext())
                            reload()
                        }
                    }
                }
                override fun onPickCustomSound() {
                    val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    customSoundLauncher.launch(intent)
                }
            })
        dialog.setOnDismissListener { currentDialog = null }
        currentDialog = dialog
        dialog.show()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(tickRunnable)
        (requireActivity() as? MainActivity)?.getSharedFab()?.visibility = View.GONE

        deletionHandler?.handlePause()
    }

    private fun reload() {
        repository?.getAllAsync { items ->
            if (!isAdded) return@getAllAsync
            val nonNull: List<TimerItem> = items?.filterNotNull() ?: emptyList()
            // Filter out items that are currently in the undo period.
            val filtered = nonNull.filter { deletionHandler?.isPending(it.id) == false }
            adapter?.submit(filtered)
            updateEmptyVisibility(filtered.isEmpty())
        }
    }

    companion object {
        private const val TICK_MS        = 500L
    }
}
