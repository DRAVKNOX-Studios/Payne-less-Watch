package com.timely.msminutes.ui.view

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.Window
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.HeaderItemRenderer
import com.timely.msminutes.ui.canvas.items.SearchItemRenderer
import com.timely.msminutes.ui.canvas.items.SoundItemRenderer
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeStore
import java.util.Locale

class SoundPickerDialog(
    context: Context,
    private val initialUri: Uri?,
    private val listener: OnSoundSelectedListener
) : Dialog(context) {

    interface OnSoundSelectedListener {
        fun onSoundSelected(name: String?, uri: Uri?)
        fun onAddCustom()
    }

    private val sounds: MutableList<SoundItem> = ArrayList()
    private var searchQuery: String = ""
    private var currentPreview: Ringtone? = null
    private var selectedUri: Uri? = initialUri
    
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)

        hostView = CanvasHostView(context)
        hostView.layoutParams = android.view.ViewGroup.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
            android.view.ViewGroup.LayoutParams.MATCH_PARENT
        )

        listView = CanvasListView(context, hostView) {}
        hostView.addRenderer(listView)

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            val w = (right - left).toFloat()
            val h = (bottom - top).toFloat()
            if (w <= 0 || h <= 0) return@addOnLayoutChangeListener
            
            listView.onLayout(0f, 0f, w, h)
            
            // Adjust dialog height to content only if size actually changed
            val contentH = listView.getContentHeight()
            if (contentH > 0) {
                val screenH = context.resources.displayMetrics.heightPixels
                val maxAllowedH = screenH * 0.85f
                val finalH = (contentH + 24f * context.resources.displayMetrics.density).coerceAtMost(maxAllowedH)
                
                val currentH = window?.attributes?.height ?: 0
                if (Math.abs(currentH - finalH.toInt()) > 10) {
                    window?.setLayout((context.resources.displayMetrics.widthPixels * 0.9f).toInt(), finalH.toInt())
                }
            }
        }

        setContentView(hostView)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Force initial window size so it appears immediately
        val screenW = context.resources.displayMetrics.widthPixels
        val screenH = context.resources.displayMetrics.heightPixels
        window?.setLayout((screenW * 0.9f).toInt(), (screenH * 0.7f).toInt())
        
        val t = ThemeStore.get().current()
        if (t != null) {
            val shape = GradientDrawable().apply {
                setColor(t.surface)
                cornerRadius = 24f * context.resources.displayMetrics.density
            }
            window?.setBackgroundDrawable(shape)
        }

        loadSounds()
    }

    fun loadSounds() {
        AppExecutors.get().diskIO {
            val manager = RingtoneManager(context)
            manager.setType(RingtoneManager.TYPE_ALARM)
            
            val tempSounds = mutableListOf<SoundItem>()
            tempSounds.add(SoundItem("Silent", null))
            
            // Add custom sounds from prefs
            val prefs = Prefs(context)
            for (uriString in prefs.customSounds) {
                try {
                    val uri = Uri.parse(uriString)
                    tempSounds.add(SoundItem(getDisplayName(uri), uri))
                } catch (_: Exception) {}
            }

            try {
                manager.cursor?.let { cursor ->
                    while (cursor.moveToNext()) {
                        val title = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX)
                        val uri = manager.getRingtoneUri(cursor.position)
                        tempSounds.add(SoundItem(title, uri))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            AppExecutors.get().mainThread {
                sounds.clear()
                sounds.addAll(tempSounds)
                if (isShowing) reload()
            }
        }
    }

    fun setSelectedUri(uri: Uri?) {
        selectedUri = uri
        reload()
    }

    private fun reload() {
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()
        
        items.add(HeaderItemRenderer(context, "Select Sound"))
        
        items.add(SearchItemRenderer(context, hostView, listView, "Search Sound", searchQuery) { q ->
            searchQuery = q
            reload()
        })

        items.add(ButtonItemRenderer(context, "Add Custom Sound") {
            listener.onAddCustom()
        })

        val filtered = if (searchQuery.isEmpty()) {
            sounds
        } else {
            val lower = searchQuery.lowercase(Locale.ROOT)
            sounds.filter { it.name.lowercase(Locale.ROOT).contains(lower) }
        }

        for (sound in filtered) {
            val isSelected = (sound.uri?.toString() == selectedUri?.toString())
            items.add(SoundItemRenderer(context, sound.name, isSelected) {
                stopPreview()
                val ringtone = if (sound.uri != null) RingtoneManager.getRingtone(context, sound.uri) else null
                ringtone?.play()
                currentPreview = ringtone
                selectedUri = sound.uri
                listener.onSoundSelected(sound.name, sound.uri)
                reload()
            })
        }
        
        items.add(ButtonItemRenderer(context, "Done") {
            dismiss()
        })

        listView.setItems(items)
        hostView.invalidate()
    }

    private fun stopPreview() {
        currentPreview?.stop()
        currentPreview = null
    }

    private fun getDisplayName(uri: Uri): String {
        var name = "Custom Sound"
        try {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        name = cursor.getString(index)
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to last path segment if query fails
            uri.lastPathSegment?.let { name = it }
        }
        
        // Clean path and extension
        val cleanName = name.substringAfterLast('/')
        val dot = cleanName.lastIndexOf('.')
        return if (dot > 0) cleanName.substring(0, dot) else cleanName
    }

    override fun onStop() {
        super.onStop()
        stopPreview()
        sounds.clear()
        listView.setItems(emptyList())
    }

    private data class SoundItem(val name: String, val uri: Uri?)
}
