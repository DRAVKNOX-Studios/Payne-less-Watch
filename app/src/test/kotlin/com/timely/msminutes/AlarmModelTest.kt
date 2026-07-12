package com.timely.msminutes

import com.timely.msminutes.data.Alarm
import org.junit.Assert.*
import org.junit.Test

class AlarmModelTest {

    @Test
    fun defaultAlarmIsNotEnabled() {
        assertFalse(Alarm().isEnabled)
    }

    @Test
    fun defaultAlarmIsNotRepeating() {
        assertFalse(Alarm().isRepeating)
    }

    @Test
    fun defaultSnoozeMinutesIsZero() {
        assertEquals(0, Alarm().snoozeMinutes)
    }

    @Test
    fun labelDefaultsToNull() {
        assertNull(Alarm().label)
    }

    @Test
    fun noteDefaultsToNull() {
        assertNull(Alarm().note)
    }

    @Test
    fun soundUriDefaultsToNull() {
        assertNull(Alarm().soundUri)
    }

    @Test
    fun setDayEnabledTrueThenFalseResultsInZeroRepeatDays() {
        val alarm = Alarm()
        for (i in 0..6) alarm.setDayEnabled(i, true)
        for (i in 0..6) alarm.setDayEnabled(i, false)
        assertEquals(0, alarm.repeatDays)
    }

    @Test
    fun settingAllSevenDaysResultsInCorrectBitmask() {
        val alarm = Alarm()
        for (i in 0..6) alarm.setDayEnabled(i, true)
        assertEquals(0b1111111, alarm.repeatDays)
        assertTrue(alarm.isRepeating)
    }

    @Test
    fun toggleDayIdempotent() {
        val alarm = Alarm()
        alarm.setDayEnabled(2, true)
        alarm.setDayEnabled(2, true)
        assertEquals(1 shl 2, alarm.repeatDays)
    }

    @Test
    fun disablingAlreadyDisabledDayIsNoOp() {
        val alarm = Alarm()
        alarm.setDayEnabled(4, false)
        assertEquals(0, alarm.repeatDays)
    }

    @Test
    fun gradualVolumeDefaultsFalse() {
        assertFalse(Alarm().isGradualVolume)
    }

    @Test
    fun vibrateDefaultsFalse() {
        assertFalse(Alarm().isVibrate)
    }
}
