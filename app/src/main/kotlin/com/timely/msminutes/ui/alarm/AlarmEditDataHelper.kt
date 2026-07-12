package com.timely.msminutes.ui.alarm

import android.net.Uri
import android.widget.EditText
import android.widget.NumberPicker
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.ui.view.DurationPicker

object AlarmEditDataHelper {

    fun populate(
        alarm: Alarm,
        is24Hour: Boolean,
        hourPicker: NumberPicker?,
        minutePicker: NumberPicker?,
        amPmPicker: NumberPicker?,
        labelInput: EditText?,
        noteInput: EditText?,
        vibrateSwitch: androidx.appcompat.widget.SwitchCompat?,
        gradualVolumeSwitch: androidx.appcompat.widget.SwitchCompat?,
        snoozePicker: DurationPicker?,
        daySelectionHelper: DaySelectionHelper?
    ) {
        if (is24Hour) {
            hourPicker?.value = alarm.hour
        } else {
            var h12 = alarm.hour % 12
            if (h12 == 0) h12 = 12
            hourPicker?.value = h12
            amPmPicker?.value = if (alarm.hour < 12) 0 else 1
        }
        minutePicker?.value = alarm.minute
        labelInput?.setText(alarm.label)
        noteInput?.setText(alarm.note)
        vibrateSwitch?.isChecked = alarm.isVibrate
        gradualVolumeSwitch?.isChecked = alarm.isGradualVolume
        snoozePicker?.setValue(if (alarm.snoozeMinutes > 0) alarm.snoozeMinutes else 10)
        
        daySelectionHelper?.populate(alarm.repeatDays)
    }

    fun saveToAlarm(
        alarm: Alarm,
        is24Hour: Boolean,
        hourPicker: NumberPicker?,
        minutePicker: NumberPicker?,
        amPmPicker: NumberPicker?,
        labelInput: EditText?,
        noteInput: EditText?,
        vibrateSwitch: androidx.appcompat.widget.SwitchCompat?,
        gradualVolumeSwitch: androidx.appcompat.widget.SwitchCompat?,
        snoozePicker: DurationPicker?,
        selectedSoundUri: Uri?,
        daySelectionHelper: DaySelectionHelper?
    ) {
        val hp = hourPicker ?: return
        val mp = minutePicker ?: return
        val ap = amPmPicker ?: return
        val sp = snoozePicker ?: return

        var hour = if (is24Hour) hp.value else { 
            var h = hp.value % 12
            if (ap.value == 1) h += 12
            h 
        }
        
        alarm.hour          = hour
        alarm.minute        = mp.value
        alarm.label         = labelInput?.text?.toString()?.trim()
        alarm.note          = noteInput?.text?.toString()?.trim()
        alarm.isVibrate     = vibrateSwitch?.isChecked == true
        alarm.isGradualVolume = gradualVolumeSwitch?.isChecked == true
        alarm.snoozeMinutes = sp.getValue()
        alarm.soundUri      = selectedSoundUri?.toString()
        alarm.isEnabled     = true
        alarm.repeatDays    = daySelectionHelper?.getRepeatDays() ?: 0
    }
}
