package com.timely.msminutes

import com.timely.msminutes.data.Alarm
import com.timely.msminutes.util.AlarmScheduler
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    private fun makeAlarm(hour: Int, minute: Int, repeatDays: Int): Alarm = Alarm().apply {
        this.hour = hour
        this.minute = minute
        this.repeatDays = repeatDays
        isEnabled = true
    }

    @Test
    fun nonRepeatingFutureTodayReturnsToday() {
        val now = Calendar.getInstance()
        var futureHour = now.get(Calendar.HOUR_OF_DAY)
        var futureMinute = now.get(Calendar.MINUTE) + 2
        if (futureMinute >= 60) {
            futureMinute -= 60
            futureHour = (futureHour + 1) % 24
        }
        val alarm = makeAlarm(futureHour, futureMinute, 0)
        val trigger = AlarmScheduler.nextTriggerMillis(alarm)
        assertTrue("Trigger must be in the future", trigger > System.currentTimeMillis())
        assertTrue("Trigger must be less than 5 min away", trigger - System.currentTimeMillis() < 5 * 60_000L)
    }

    @Test
    fun nonRepeatingPastTimeReturnsTomorrow() {
        val alarm = makeAlarm(0, 0, 0)
        val trigger = AlarmScheduler.nextTriggerMillis(alarm)
        val trigCal = Calendar.getInstance().apply { timeInMillis = trigger }
        val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(tomorrow.get(Calendar.DAY_OF_YEAR), trigCal.get(Calendar.DAY_OF_YEAR))
    }

    @Test
    fun repeatingAlarmAllDaysReturnsFuture() {
        val alarm = makeAlarm(0, 1, 0b1111111)
        val trigger = AlarmScheduler.nextTriggerMillis(alarm)
        assertTrue("Repeating trigger must be in the future", trigger > System.currentTimeMillis())
    }

    @Test
    fun repeatingAlarmWithNoValidDayStillReturnsFuture() {
        val alarm = makeAlarm(23, 59, 0b0000001)
        val trigger = AlarmScheduler.nextTriggerMillis(alarm)
        assertTrue(trigger > 0)
    }

    @Test
    fun isDayEnabledBitmaskCorrect() {
        val alarm = Alarm()
        alarm.setDayEnabled(0, true)
        assertTrue(alarm.isDayEnabled(0))
        assertFalse(alarm.isDayEnabled(1))
        alarm.setDayEnabled(0, false)
        assertFalse(alarm.isDayEnabled(0))
    }

    @Test
    fun isRepeatingReturnsTrueWhenBitsSet() {
        val alarm = makeAlarm(8, 0, 0b0000010)
        assertTrue(alarm.isRepeating)
    }

    @Test
    fun isRepeatingReturnsFalseWhenNoBitsSet() {
        val alarm = makeAlarm(8, 0, 0)
        assertFalse(alarm.isRepeating)
    }

    @Test
    fun allSevenDaysIndividuallyEnableAndDisable() {
        val alarm = Alarm()
        for (day in 0..6) {
            alarm.setDayEnabled(day, true)
            assertTrue("Day $day should be enabled", alarm.isDayEnabled(day))
            alarm.setDayEnabled(day, false)
            assertFalse("Day $day should be disabled", alarm.isDayEnabled(day))
        }
    }

    @Test
    fun multipleDaysBitmaskDoesNotCrossContaminate() {
        val alarm = Alarm()
        alarm.setDayEnabled(1, true)
        alarm.setDayEnabled(3, true)
        assertTrue(alarm.isDayEnabled(1))
        assertTrue(alarm.isDayEnabled(3))
        assertFalse(alarm.isDayEnabled(0))
        assertFalse(alarm.isDayEnabled(2))
        assertFalse(alarm.isDayEnabled(4))
    }

    @Test
    fun triggerTimeIsAlwaysInTheFutureForMidnightAlarm() {
        val alarm = makeAlarm(0, 0, 0b1111111)
        val trigger = AlarmScheduler.nextTriggerMillis(alarm)
        assertTrue(trigger > System.currentTimeMillis())
    }
}
