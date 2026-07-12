package com.timely.msminutes

import com.timely.msminutes.data.Alarm
import com.timely.msminutes.util.AlarmTimeUtil
import org.junit.Assert.*
import org.junit.Test
import java.util.Calendar

class AlarmTimeUtilTest {

    private fun alarmInMinutes(minutesFromNow: Int): Alarm {
        val c = Calendar.getInstance().apply { add(Calendar.MINUTE, minutesFromNow) }
        return Alarm().apply {
            hour = c.get(Calendar.HOUR_OF_DAY)
            minute = c.get(Calendar.MINUTE)
            repeatDays = 0
            isEnabled = true
        }
    }

    @Test
    fun disabledAlarmReturnsNull() {
        val alarm = alarmInMinutes(5).apply { isEnabled = false }
        assertNull(AlarmTimeUtil.getRemainingTimeText(alarm))
    }

    @Test
    fun enabledFutureAlarmReturnsNonNull() {
        val alarm = alarmInMinutes(3)
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        assertNotNull(result)
        assertTrue(result!!.startsWith("Alarm in"))
    }

    @Test
    fun minuteComponentAppearsForSoonAlarm() {
        val alarm = alarmInMinutes(5)
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        assertNotNull(result)
        assertTrue(result!!.contains("minute"))
    }

    @Test
    fun secondsAppearsForImmediateAlarm() {
        val alarm = alarmInMinutes(0).apply {
            hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            minute = Calendar.getInstance().get(Calendar.MINUTE)
        }
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        if (result != null) {
            assertTrue(result.contains("second"))
        }
    }

    @Test
    fun textDoesNotExceedReasonableLength() {
        val alarm = alarmInMinutes(90)
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        assertNotNull(result)
        assertTrue(result!!.length < 80)
    }

    @Test
    fun hourComponentAppearsForAlarmMoreThanSixtyMinutesAway() {
        val alarm = alarmInMinutes(90)
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        assertNotNull(result)
        assertTrue("Expected 'hour' in result: $result", result!!.contains("hour"))
    }

    @Test
    fun alarmExactlyOneDayAwayReturnsNonNull() {
        val alarm = alarmInMinutes(1440)
        val result = AlarmTimeUtil.getRemainingTimeText(alarm)
        assertNotNull(result)
    }
}
