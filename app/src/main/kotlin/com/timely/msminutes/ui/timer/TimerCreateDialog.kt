package com.timely.msminutes.ui.timer

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Window
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.EditItemRenderer
import com.timely.msminutes.ui.canvas.items.HeaderItemRenderer
import com.timely.msminutes.ui.canvas.items.InlineEditItemRenderer
import com.timely.msminutes.ui.canvas.items.TimerPickerItemRenderer
import com.timely.msminutes.ui.canvas.items.ToggleItemRenderer
import com.timely.msminutes.ui.view.SoundPickerDialog
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.RefreshRateOptimizer
import com.timely.msminutes.util.ThemeStore

class TimerCreateDialog(private val context: Context, private val listener: OnCreateListener) {

    interface OnCreateListener {
        fun onCreate(item: TimerItem?)
        fun onPickCustomSound()
    }

    private var selectedSound: Uri? = null
    private var selectedSoundName: String? = null

    private var dialog: Dialog? = null
    private var soundPickerDialog: SoundPickerDialog? = null
    private var onDismissListener: (() -> Unit)? = null
    
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView

    private var h: Int = 0
    private var m: Int = 5
    private var s: Int = 0
    private var label: String = ""
    private var isVibrate: Boolean = true

    fun setOnDismissListener(l: () -> Unit) {
        onDismissListener = l
    }

    fun setSelectedSound(uri: Uri?, name: String?) {
        selectedSound     = uri
        selectedSoundName = name
        reload()
    }

    fun show() {
        if (dialog != null && dialog?.isShowing == true) return

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
            listView.onLayout(0f, 0f, w, h)
            reload()
            
            val contentH = listView.getContentHeight()
            if (contentH > 0) {
                val maxH = context.resources.displayMetrics.heightPixels * 0.85f
                val finalH = (contentH + 24f * context.resources.displayMetrics.density).coerceAtMost(maxH)
                dialog?.window?.setLayout((context.resources.displayMetrics.widthPixels * 0.9f).toInt(), finalH.toInt())
            }
        }

        selectedSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        selectedSoundName = "Default"
        AppExecutors.get().diskIO {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(
                context.applicationContext, RingtoneManager.TYPE_ALARM
            )
            val name = try {
                RingtoneManager.getRingtone(context, uri)?.getTitle(context) ?: "Default"
            } catch (_: Exception) { "Default" }
            Handler(Looper.getMainLooper()).post {
                selectedSound     = uri
                selectedSoundName = name
                if (dialog?.isShowing == true) reload()
            }
        }

        val d = Dialog(context)
        dialog = d
        d.requestWindowFeature(Window.FEATURE_NO_TITLE)
        d.setContentView(hostView)
        d.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val t = ThemeStore.get().current()
        if (t != null) {
            val shape = GradientDrawable().apply {
                setColor(t.surface)
                cornerRadius = 24f * context.resources.displayMetrics.density
            }
            d.window?.setBackgroundDrawable(shape)
        }

        d.setOnDismissListener {
            dialog            = null
            selectedSound     = null
            selectedSoundName = null
            onDismissListener?.invoke()
        }
        d.show()
        RefreshRateOptimizer.optimize(d.window!!)
    }

    private fun reload() {
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()

        items.add(HeaderItemRenderer(context, "New Timer"))
        
        items.add(TimerPickerItemRenderer(context, hostView, listView, h, m, s) { hh, mm, ss ->
            h = hh; m = mm; s = ss
        })

        items.add(InlineEditItemRenderer(context, hostView, listView, "Label", label, "Timer label") {
            label = it
        })

        items.add(ToggleItemRenderer(context, "Vibrate", isVibrate) {
            isVibrate = it
        })

        items.add(EditItemRenderer(context, "Sound", selectedSoundName ?: "Default") {
            openSoundPicker()
        })

        items.add(ButtonItemRenderer(context, "Start") {
            val totalMs = (h * 3600L + m * 60L + s) * 1000L
            if (totalMs > 0) {
                val item = TimerItem().apply {
                    this.totalMillis     = totalMs
                    this.remainingMillis = totalMs
                    this.isVibrate       = this@TimerCreateDialog.isVibrate
                    this.soundUri        = selectedSound?.toString()
                    this.label           = this@TimerCreateDialog.label.trim()
                }
                listener.onCreate(item)
            }
            dialog?.dismiss()
        })

        items.add(ButtonItemRenderer(context, "Cancel", isDanger = true) {
            dialog?.dismiss()
        })

        listView.setItems(items)
        hostView.invalidate()
    }

    private fun openSoundPicker() {
        val picker = SoundPickerDialog(
            context, selectedSound,
            object : SoundPickerDialog.OnSoundSelectedListener {
                override fun onSoundSelected(name: String?, uri: Uri?) {
                    selectedSound     = uri
                    selectedSoundName = name
                    reload()
                }
                override fun onAddCustom() {
                    listener.onPickCustomSound()
                }
            }
        )
        soundPickerDialog = picker
        picker.show()
    }

    fun onCustomSoundPicked(uri: Uri, name: String?) {
        selectedSound = uri
        selectedSoundName = name
        soundPickerDialog?.let {
            if (it.isShowing) {
                it.loadSounds()
                it.setSelectedUri(uri)
            }
        }
        reload()
    }

    fun resumeAfterCustomPicker() {
        reload()
    }

    fun dismiss() {
        dialog?.dismiss()
    }
}
