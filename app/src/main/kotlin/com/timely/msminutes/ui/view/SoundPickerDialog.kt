package com.timely.msminutes.ui.view

import android.app.Dialog
import android.content.ContentUris
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.widget.ImageViewCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeUtil

class SoundPickerDialog(
    context: Context,
    private val currentUri: Uri?,
    private val listener: OnSoundSelectedListener
) : Dialog(context) {

    interface OnSoundSelectedListener {
        fun onSoundSelected(name: String?, uri: Uri?)
        fun onAddCustom()
    }

    private val sounds: MutableList<SoundItem> = ArrayList()
    private var currentPreview: Ringtone? = null
    private var selectedPosition = -1
    private var listView: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_sound_picker)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val prefs = Prefs(context)
        val isCustom = prefs.isCustomTheme
        val fontColor = prefs.fontColor
        val accentColor = prefs.accentColor

        if (isCustom) {
            val root = findViewById<View>(R.id.dialog_root)
            if (root != null) {
                val gd = GradientDrawable()
                gd.setColor(prefs.backgroundColor)
                gd.cornerRadius = ThemeUtil.dpToPx(context, 24).toFloat()
                root.background = gd
                ThemeUtil.applyColorsIterative(root, fontColor)
            }
            ImageViewCompat.setImageTintList(
                findViewById<ImageView>(R.id.btn_add_custom),
                ColorStateList.valueOf(fontColor)
            )
            findViewById<Button>(R.id.btn_cancel).setTextColor(fontColor)
        }

        listView = findViewById(R.id.list_sounds)

        sounds.add(SoundItem("Silent", null))
        sounds.add(SoundItem("Default",
            RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)))
        reselectCurrent()

        val adapter = SoundPickerAdapter(context, sounds, isCustom, fontColor, accentColor) { selectedPosition }
        listView!!.adapter = adapter
        listView!!.setOnItemClickListener { _, _, pos, _ ->
            selectedPosition = pos
            playPreview(sounds[pos].uri)
            adapter.notifyDataSetChanged()
        }

        AppExecutors.get().diskIO {
            val loaded = loadRingtonesViaMediaStore()
            Handler(Looper.getMainLooper()).post {
                if (!isShowing) return@post
                sounds.addAll(loaded)
                reselectCurrent()
                adapter.notifyDataSetChanged()
            }
        }

        val btnDone = findViewById<Button>(R.id.btn_done)
        btnDone.setTextColor(accentColor)
        btnDone.setOnClickListener {
            if (selectedPosition != -1) {
                val item = sounds[selectedPosition]
                listener.onSoundSelected(item.name, item.uri)
            }
            dismiss()
        }
        findViewById<View>(R.id.btn_cancel).setOnClickListener { dismiss() }
        findViewById<View>(R.id.btn_add_custom).setOnClickListener {
            listener.onAddCustom()
            dismiss()
        }
    }

    /**
     * Replaces [RingtoneManager.cursor] with a direct [MediaStore] query.
     *
     * Why: [RingtoneManager] internally creates a merged cursor across
     * multiple content providers and allocates a [android.database.CursorWindow]
     * (a shared-memory region) that is typically 2–16 MB depending on the
     * device's ringtone library size.  That window stays mapped into the
     * process RSS for as long as the cursor (and by extension the
     * [RingtoneManager] object) is alive — even on a background thread.
     *
     * A direct [MediaStore] query with a two-column projection
     * ([MediaStore.Audio.Media._ID] + [MediaStore.Audio.Media.TITLE])
     * fetches only the data we actually display.  The cursor window for a
     * 50-row result set with two small string columns is < 50 KB vs the
     * 2–16 MB the full [RingtoneManager] cursor brings in.
     */
    private fun loadRingtonesViaMediaStore(): List<SoundItem> {
        val loaded = mutableListOf<SoundItem>()
        var found = false
        val defaultUri = sounds.getOrNull(1)?.uri

        // Query only the two columns we display — title + the ID to build the URI.
        // IS_ALARM = 1 filters to alarm sounds only, same as
        // RingtoneManager.setType(TYPE_ALARM).
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE
        )
        val selection  = "${MediaStore.Audio.Media.IS_ALARM} = 1"
        val sortOrder  = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                projection, selection, null, sortOrder
            )?.use { cursor ->
                val idCol    = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                while (cursor.moveToNext()) {
                    val id    = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol)
                    val uri   = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.INTERNAL_CONTENT_URI, id
                    )
                    loaded.add(SoundItem(cleanName(title), uri))
                    if (uri == currentUri) found = true
                }
            }
        } catch (_: Exception) { }

        // Append custom URI if not found in the system list.
        if (currentUri != null && !found && currentUri != defaultUri) {
            loaded.add(SoundItem(getFileName(currentUri), currentUri))
        }
        return loaded
    }

    private fun reselectCurrent() {
        for (i in sounds.indices) {
            val uri = sounds[i].uri
            if ((uri == null && currentUri == null) || (uri != null && uri == currentUri)) {
                selectedPosition = i
                return
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        var name: String? = "Custom"
        try {
            context.contentResolver.query(uri, null, null, null, null).use { c ->
                if (c != null && c.moveToFirst())
                    name = c.getString(c.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
            }
        } catch (_: Exception) { }
        return cleanName(name)
    }

    private fun cleanName(name: String?): String {
        var n = name ?: return "Custom"
        if (n.contains("/")) n = n.substring(n.lastIndexOf("/") + 1)
        if (n.contains(".")) n = n.substring(0, n.lastIndexOf("."))
        return n
    }

    private fun playPreview(uri: Uri?) {
        currentPreview?.stop()
        currentPreview = null
        if (uri == null) return
        try {
            currentPreview = RingtoneManager.getRingtone(context, uri)
            currentPreview?.play()
        } catch (_: Exception) { }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        currentPreview?.stop()
        currentPreview = null
        listView?.adapter = null
        listView = null
    }
}
