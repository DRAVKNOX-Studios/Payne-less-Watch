package com.timely.msminutes.ui.alarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.CanvasListView
import com.timely.msminutes.ui.canvas.ToolbarRenderer
import com.timely.msminutes.ui.canvas.items.ButtonItemRenderer
import com.timely.msminutes.ui.canvas.items.DayToggleRenderer
import com.timely.msminutes.ui.canvas.items.DurationPickerItemRenderer
import com.timely.msminutes.ui.canvas.items.EditItemRenderer
import com.timely.msminutes.ui.canvas.items.InlineEditItemRenderer
import com.timely.msminutes.ui.canvas.items.TimePickerItemRenderer
import com.timely.msminutes.ui.canvas.items.ToggleItemRenderer
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AlarmTimeUtil
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate
import java.util.Calendar

class AlarmEditActivity : AppCompatActivity(), ThemeListener {
    private lateinit var hostView: CanvasHostView
    private lateinit var listView: CanvasListView
    private lateinit var toolbarRenderer: ToolbarRenderer

    private var repository: AlarmRepository? = null
    private var alarm: Alarm? = null
    private var selectedSoundUri: Uri? = null
    private var is24Hour = false
    private var prefs: Prefs? = null

    private var soundPickerHelper: SoundPickerHelper? = null
    private var isSaving = false

    private val customSoundLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result: ActivityResult? ->
        if (result?.resultCode == RESULT_OK && result.data != null) {
            val uri = result.data?.data
            if (uri != null) {
                contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                prefs?.addCustomSound(uri.toString())
                selectedSoundUri = uri
                soundPickerHelper?.onCustomSoundPicked(uri)
                reload()
            }
        }
    }

    private var lastWidth = 0f
    private var isInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        
        val repo = AlarmRepository(this)
        repository = repo
        val p = Prefs(this)
        prefs = p
        is24Hour = p.is24Hour()

        val root = FrameLayout(this)
        setContentView(root)

        hostView = CanvasHostView(this)
        root.addView(hostView)

        toolbarRenderer = ToolbarRenderer(this, "Edit Alarm", showBack = true) {
            finish()
        }
        hostView.addRenderer(toolbarRenderer)

        listView = CanvasListView(this, hostView) {}
        hostView.addRenderer(listView)

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val w = root.width.toFloat()
            val h = root.height.toFloat()
            if (w > 0 && h > 0) {
                val d = resources.displayMetrics.density
                val toolbarH = 56f * d
                toolbarRenderer.onLayout(0f, systemBars.top.toFloat(), w, systemBars.top.toFloat() + toolbarH)
                listView.onLayout(0f, systemBars.top.toFloat() + toolbarH, w, h - systemBars.bottom.toFloat())
                
                if (!isInitialized || lastWidth != w) {
                    isInitialized = true
                    lastWidth = w
                    reload()
                }
            }
            insets
        }

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            ViewCompat.requestApplyInsets(root)
        }

        soundPickerHelper = SoundPickerHelper(this, customSoundLauncher) { uri, _ ->
            selectedSoundUri = uri
            reload()
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId != -1L) {
            alarm = repo.getById(alarmId)
            alarm?.let {
                if (it.soundUri != null) selectedSoundUri = Uri.parse(it.soundUri)
            }
        } else {
            val newAlarm = Alarm()
            newAlarm.isGradualVolume = p.isGradualVolumeDefault
            newAlarm.snoozeMinutes   = p.defaultSnoozeMinutes
            val now = Calendar.getInstance()
            newAlarm.hour   = now.get(Calendar.HOUR_OF_DAY)
            newAlarm.minute = now.get(Calendar.MINUTE)
            alarm = newAlarm
            selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        }
    }

    private fun reload() {
        val a = alarm ?: return
        val items = mutableListOf<com.timely.msminutes.ui.canvas.ItemRenderer>()
        
        items.add(TimePickerItemRenderer(this, hostView, listView, a.hour, a.minute, is24Hour) { h, m ->
            a.hour = h
            a.minute = m
        })

        val daysArray = BooleanArray(7) { i -> a.isDayEnabled(i) }
        items.add(DayToggleRenderer(this, daysArray) { index ->
            a.setDayEnabled(index, !a.isDayEnabled(index))
            reload()
        })

        items.add(InlineEditItemRenderer(this, hostView, listView, "Label", a.label ?: "", "Alarm label") {
            a.label = it
        })

        items.add(InlineEditItemRenderer(this, hostView, listView, "Note", a.note ?: "", "Alarm note") {
            a.note = it
        })

        items.add(ToggleItemRenderer(this, "Vibrate", a.isVibrate) {
            a.isVibrate = it
        })

        items.add(ToggleItemRenderer(this, "Gradual Volume", a.isGradualVolume) {
            a.isGradualVolume = it
        })

        val soundName = if (selectedSoundUri != null) soundPickerHelper?.getRingtoneName(selectedSoundUri!!) ?: "Default" else "Silent"
        items.add(EditItemRenderer(this, "Sound", soundName) {
            soundPickerHelper?.openSoundPicker(selectedSoundUri)
        })

        items.add(DurationPickerItemRenderer(this, "Snooze duration", a.snoozeMinutes) {
            a.snoozeMinutes = it
        })

        if (a.id != 0L) {
            items.add(ButtonItemRenderer(this, "Delete Alarm", isDanger = true) {
                deleteAlarm()
            })
        }

        items.add(ButtonItemRenderer(this, "Save") {
            if (!isSaving) {
                isSaving = true
                save()
            }
        })

        listView.setItems(items)
        hostView.invalidate()
    }

    private fun save() {
        val currentAlarm = alarm ?: return
        currentAlarm.soundUri = selectedSoundUri?.toString()
        currentAlarm.isEnabled = true

        if (currentAlarm.id == 0L) repository?.insert(currentAlarm)
        else repository?.update(currentAlarm)

        AlarmScheduler.schedule(this, currentAlarm)
        notifyUpdate(this)
        val remaining = AlarmTimeUtil.getRemainingTimeText(currentAlarm)
        if (remaining != null) Toast.makeText(this, remaining, Toast.LENGTH_LONG).show()
        finish()
    }

    private fun deleteAlarm() {
        val currentAlarm = alarm ?: return
        AlarmScheduler.cancel(this, currentAlarm.id)
        repository?.delete(currentAlarm.id)
        notifyUpdate(this)
        finish()
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
        if (t == null) return
        ThemeApplier.applyWindow(window, t)
        hostView.invalidate()
    }

    override fun onDestroy() {
        super.onDestroy()
        repository = null
        alarm = null
        prefs = null
        soundPickerHelper = null
    }

    companion object {
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    }
}
