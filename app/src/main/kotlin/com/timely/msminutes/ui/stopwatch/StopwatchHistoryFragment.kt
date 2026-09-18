package com.timely.msminutes.ui.stopwatch

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.timely.msminutes.data.StopwatchHistoryItem
import com.timely.msminutes.data.StopwatchRepository
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.canvas.CanvasDialog
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.ComicBubbleRenderer
import com.timely.msminutes.ui.canvas.ItemRenderer
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.HeaderItemRenderer
import com.timely.msminutes.ui.canvas.items.HistoryItemRenderer
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens

class StopwatchHistoryFragment : Fragment(), ThemeListener {
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var emptyRenderer: ComicBubbleRenderer
    private var repository: StopwatchRepository? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        hostView = CanvasHostView(requireContext())
        hostView.drawBackground = false
        emptyRenderer = ComicBubbleRenderer(requireContext(), "No history yet")
        listView = CanvasListView(requireContext(), hostView) { isEmpty ->
            emptyRenderer.isVisible = isEmpty
            hostView.invalidate()
        }
        hostView.addRenderer(listView)
        hostView.addRenderer(emptyRenderer)

        repository = StopwatchRepository(requireContext())

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
    }

    private fun reload() {
        repository?.getAllAsync { items ->
            if (!isAdded) return@getAllAsync
            val renderers = mutableListOf<ItemRenderer>()
            
            if (items.isNotEmpty()) {
                renderers.add(ButtonItemRenderer(requireContext(), "Clear History", isDanger = true) {
                    confirmClearHistory()
                })
            }

            renderers.addAll(items.map { item ->
                HistoryItemRenderer(requireContext(), item, 
                    onUpdate = { 
                        listView.layoutItems()
                        hostView.invalidate() 
                    },
                    onDelete = { deleteItem(item) }
                )
            })
            listView.setItems(renderers)
            hostView.invalidate()
        }
    }

    private fun confirmClearHistory() {
        CanvasDialog(requireContext()) { d, list ->
            val items = mutableListOf<ItemRenderer>()
            items.add(HeaderItemRenderer(requireContext(), "Clear History"))
            items.add(ButtonItemRenderer(requireContext(), "Clear All", isDanger = true) {
                clearHistory()
                d.dismiss()
            })
            items.add(ButtonItemRenderer(requireContext(), "Cancel") {
                d.dismiss()
            })
            list.setItems(items)
        }.show()
    }

    private fun clearHistory() {
        AppExecutors.get().diskIO {
            repository?.deleteAll()
            AppExecutors.get().mainThread {
                if (isAdded) reload()
            }
        }
    }

    private fun deleteItem(item: StopwatchHistoryItem) {
        AppExecutors.get().diskIO {
            repository?.delete(item.id)
            AppExecutors.get().mainThread {
                if (isAdded) {
                    reload()
                    (activity as? MainActivity)?.showUndo("Session deleted") {
                        AppExecutors.get().diskIO {
                            repository?.insert(item)
                            AppExecutors.get().mainThread { if (isAdded) reload() }
                        }
                    }
                }
            }
        }
    }
}
