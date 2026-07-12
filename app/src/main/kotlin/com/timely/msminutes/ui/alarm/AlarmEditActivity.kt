package com.timely.msminutes.ui.alarm

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.appcompat.widget.Toolbar
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.view.DurationPicker
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AlarmTimeUtil
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate
import java.util.Calendar

class AlarmEditActivity : AppCompatActivity(), ThemeListener {
    private var hourPicker: NumberPicker? = null
    private var minutePicker: NumberPicker? = null
    private var amPmPicker: NumberPicker? = null
    private var labelInput: EditText? = null
    private var noteInput: EditText? = null
    private var vibrateSwitch: SwitchCompat? = null
    private var gradualVolumeSwitch: SwitchCompat? = null
    private var soundLabel: TextView? = null
    private var snoozePicker: DurationPicker? = null
    private val dayToggles = arrayOfNulls<TextView>(7)
    private val daySelected = BooleanArray(7)
    private var editRoot: View? = null

    private var repository: AlarmRepository? = null
    private var alarm: Alarm? = null
    private var selectedSoundUri: Uri? = null
    private var is24Hour = false
    private var prefs: Prefs? = null

    private var soundPickerHelper: SoundPickerHelper? = null
    private var daySelectionHelper: DaySelectionHelper? = null
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
                selectedSoundUri = uri
                soundLabel?.text = soundPickerHelper?.getFileName(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_edit)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        val repo = AlarmRepository(this)
        repository = repo
        val p = Prefs(this)
        prefs = p
        is24Hour = p.is24Hour()
        editRoot = findViewById(R.id.edit_root)

        hourPicker   = findViewById(R.id.picker_hours)
        minutePicker = findViewById(R.id.picker_minutes)
        amPmPicker   = findViewById(R.id.picker_am_pm)

        hourPicker?.let { hp ->
            if (is24Hour) {
                hp.minValue = 0
                hp.maxValue = 23
                amPmPicker?.visibility = View.GONE
            } else {
                hp.minValue = 1
                hp.maxValue = 12
                amPmPicker?.let { ap ->
                    ap.minValue = 0
                    ap.maxValue = 1
                    ap.displayedValues = arrayOf("AM", "PM")
                    ap.visibility = View.VISIBLE
                }
            }
        }
        minutePicker?.minValue = 0
        minutePicker?.maxValue = 59

        labelInput          = findViewById(R.id.input_label)
        noteInput           = findViewById(R.id.input_note)
        vibrateSwitch       = findViewById(R.id.switch_vibrate)
        gradualVolumeSwitch = findViewById(R.id.switch_gradual_volume)
        soundLabel          = findViewById(R.id.text_sound)
        snoozePicker        = findViewById(R.id.picker_snooze)

        val dayIds = intArrayOf(
            R.id.day_mon, R.id.day_tue, R.id.day_wed,
            R.id.day_thu, R.id.day_fri, R.id.day_sat, R.id.day_sun
        )
        for (i in 0..6) {
            dayToggles[i] = findViewById(dayIds[i])
        }
        daySelectionHelper = DaySelectionHelper(dayToggles, daySelected) {
            ThemeStore.get().current()?.let { daySelectionHelper?.applyTheme(it) }
        }

        soundPickerHelper = SoundPickerHelper(this, customSoundLauncher) { uri, name ->
            selectedSoundUri = uri
            soundLabel?.text = name
        }

        findViewById<View>(R.id.text_sound_row).setOnClickListener {
            soundPickerHelper?.openSoundPicker(selectedSoundUri)
        }
        findViewById<View>(R.id.btn_save).setOnClickListener {
            if (!isSaving) {
                isSaving = true
                save()
            }
        }

        ThemeStore.get().current()?.let { t ->
            ThemeApplier.applyAccentButton(findViewById(R.id.btn_save), t)
        }

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        if (alarmId != -1L) alarm = repo.getById(alarmId)

        val currentAlarm = alarm
        if (currentAlarm != null) {
            populate(currentAlarm)
            findViewById<View>(R.id.btn_delete).let {
                it.visibility = View.VISIBLE
                it.setOnClickListener { deleteAlarm() }
            }
        } else {
            val newAlarm = Alarm()
            newAlarm.isGradualVolume = p.isGradualVolumeDefault
            newAlarm.snoozeMinutes   = p.defaultSnoozeMinutes

            val now = Calendar.getInstance()
            newAlarm.hour   = now.get(Calendar.HOUR_OF_DAY)
            newAlarm.minute = now.get(Calendar.MINUTE)

            alarm = newAlarm
            gradualVolumeSwitch?.isChecked = newAlarm.isGradualVolume
            snoozePicker?.setValue(newAlarm.snoozeMinutes)
            populate(newAlarm)

            selectedSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            soundLabel?.text = "Default"

            AppExecutors.get().diskIO {
                val uri = RingtoneManager.getActualDefaultRingtoneUri(
                    applicationContext, RingtoneManager.TYPE_ALARM
                )
                val name = if (uri != null) soundPickerHelper?.getRingtoneName(uri) ?: "Default" else "Default"
                AppExecutors.get().mainThread {
                    if (!isDestroyed) {
                        selectedSoundUri = uri
                        soundLabel?.text = name
                    }
                }
            }
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
        if (t == null) return
        AlarmEditThemeHelper.applyTheme(
            window, editRoot, t, prefs?.isCustomTheme == true,
            hourPicker, minutePicker, amPmPicker,
            labelInput, noteInput, soundLabel,
            vibrateSwitch, gradualVolumeSwitch, snoozePicker,
            daySelectionHelper
        )
    }

    private fun populate(a: Alarm) {
        AlarmEditDataHelper.populate(
            a, is24Hour, hourPicker, minutePicker, amPmPicker,
            labelInput, noteInput, vibrateSwitch, gradualVolumeSwitch,
            snoozePicker, daySelectionHelper
        )

        val uriStr = a.soundUri
        if (uriStr != null) {
            val uri = Uri.parse(uriStr)
            selectedSoundUri = uri
            soundLabel?.text = "..."
            AppExecutors.get().diskIO {
                val name = soundPickerHelper?.getRingtoneName(uri)
                AppExecutors.get().mainThread {
                    if (!isDestroyed) soundLabel?.text = name
                }
            }
        } else {
            selectedSoundUri = null
            soundLabel?.text = "Silent"
        }
    }

    private fun save() {
        val currentAlarm = alarm ?: return
        
        AlarmEditDataHelper.saveToAlarm(
            currentAlarm, is24Hour, hourPicker, minutePicker, amPmPicker,
            labelInput, noteInput, vibrateSwitch, gradualVolumeSwitch,
            snoozePicker, selectedSoundUri, daySelectionHelper
        )

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

    override fun onDestroy() {
        super.onDestroy()
        hourPicker = null
        minutePicker = null
        amPmPicker = null
        labelInput = null
        noteInput = null
        vibrateSwitch = null
        gradualVolumeSwitch = null
        soundLabel = null
        snoozePicker = null
        for (i in 0..6) dayToggles[i] = null
        editRoot = null
        repository = null
        alarm = null
        prefs = null
        soundPickerHelper = null
        daySelectionHelper = null
    }

    companion object {
        const val EXTRA_ALARM_ID: String = "extra_alarm_id"
    }
}
