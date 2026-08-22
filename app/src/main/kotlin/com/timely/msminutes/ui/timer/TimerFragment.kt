package com.timely.msminutes.ui.timer

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.items.TimerItemRenderer
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class TimerFragment : Fragment(), ThemeListener {
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var emptyRenderer: com.timely.msminutes.ui.canvas.ComicBubbleRenderer
    private var repository: TimerRepository? = null
    private var currentDialog: TimerCreateDialog? = null
    private var actionHandler: TimerActionHandler? = null

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable: Runnable = object : Runnable {
        override fun run() {
            hostView.invalidate()
            handler.postDelayed(this, 100L)
        }
    }

    private val updateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
            reload()
        }
    }

    private val customSoundLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result?.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null && currentDialog != null) {
                requireContext().contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                Prefs(requireContext()).addCustomSound(uri.toString())
                val fileName = getFileName(uri)
                currentDialog?.onCustomSoundPicked(uri, fileName)
            }
        }
    }

    private fun getFileName(uri: android.net.Uri): String? {
        var name = "Custom"
        try {
            requireContext().contentResolver
                .query(uri, null, null, null, null).use { cursor ->
                    if (cursor != null && cursor.moveToFirst()) {
                        val index = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            name = cursor.getString(index)
                        }
                    }
                }
        } catch (ignored: Exception) {
            uri.lastPathSegment?.let { name = it }
        }
        
        // Clean path and extension
        val cleanName = name.substringAfterLast('/')
        val dot = cleanName.lastIndexOf('.')
        return if (dot > 0) cleanName.substring(0, dot) else cleanName
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hostView = CanvasHostView(requireContext())
        hostView.drawBackground = false
        hostView.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        emptyRenderer = com.timely.msminutes.ui.canvas.ComicBubbleRenderer(requireContext(), "No timers yet")
        listView = CanvasListView(requireContext(), hostView) { isEmpty ->
            emptyRenderer.isVisible = isEmpty
            hostView.invalidate()
        }
        hostView.addRenderer(listView)
        hostView.addRenderer(emptyRenderer)

        val repo = TimerRepository(requireContext())
        repository = repo
        actionHandler = TimerActionHandler(requireContext(), repo) { reload() }

        return hostView
    }

    fun showCreateDialog() {
        if (currentDialog != null) return
        val repo = repository ?: return

        val dialog = TimerCreateDialog(
            requireContext(),
            object : TimerCreateDialog.OnCreateListener {
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
                    val intent = android.content.Intent(android.content.Intent.ACTION_OPEN_DOCUMENT).apply {
                        addCategory(android.content.Intent.CATEGORY_OPENABLE)
                        type = "audio/*"
                    }
                    customSoundLauncher.launch(intent)
                }
            })
        dialog.setOnDismissListener { currentDialog = null }
        currentDialog = dialog
        dialog.show()
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
        val filter = android.content.IntentFilter(com.timely.msminutes.widget.WidgetNotifier.ACTION_UPDATE_WIDGET)
        androidx.core.content.ContextCompat.registerReceiver(
            requireContext(),
            updateReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
        requireContext().unregisterReceiver(updateReceiver)
        handler.removeCallbacks(tickRunnable)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        hostView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        reload()
        handler.post(tickRunnable)
    }

    private fun reload() {
        repository?.getAllAsync { items ->
            if (!isAdded) return@getAllAsync
            val nonNull = items?.filterNotNull()?.filter { !TimerRepository.pendingDeletions.contains(it.id) } ?: emptyList()
            val renderers = nonNull.map { item ->
                TimerItemRenderer(requireContext(), item, 
                    onToggle = { actionHandler?.toggleTimer(item) },
                    onReset = { actionHandler?.resetTimer(item) },
                    onDeleteAction = { stageDeleteTimer(item) },
                    onCopyAction = { copyTimer(item) },
                    onEnd = { actionHandler?.stopTimer(item) }
                )
            }
            listView.setItems(renderers)
            hostView.invalidate()
        }
    }

    private fun copyTimer(item: TimerItem) {
        val newItem = TimerItem().apply {
            totalMillis = item.totalMillis
            remainingMillis = item.totalMillis
            state = TimerItem.STATE_PAUSED
            label = if (item.label != null) "${item.label} (Copy)" else "Timer (Copy)"
            soundUri = item.soundUri
            isVibrate = item.isVibrate
        }
        AppExecutors.get().diskIO {
            val newId = repository?.insert(newItem) ?: -1L
            AppExecutors.get().mainThread {
                if (isAdded) {
                    reload()
                    val main = activity as? MainActivity
                    main?.showUndo("Timer copied") {
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

    private fun stageDeleteTimer(item: TimerItem) {
        TimerRepository.pendingDeletions.add(item.id)
        reload()
        
        val main = activity as? MainActivity
        main?.showUndo("Timer deleted") {
            TimerRepository.pendingDeletions.remove(item.id)
            reload()
        }
        
        handler.postDelayed({
            if (TimerRepository.pendingDeletions.contains(item.id)) {
                TimerRepository.pendingDeletions.remove(item.id)
                commitDeleteTimer(item)
            }
        }, 5000L)
    }

    private fun commitDeleteTimer(item: TimerItem) {
        AppExecutors.get().diskIO {
            repository?.delete(item.id)
            AppExecutors.get().mainThread {
                if (!isAdded) return@mainThread
                notifyUpdate(requireContext())
            }
        }
    }
}
