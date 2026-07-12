package com.timely.msminutes

import com.timely.msminutes.data.TimerItem
import org.junit.Assert.*
import org.junit.Test

class TimerItemTest {

    @Test
    fun defaultStateIsRunning() {
        assertEquals(TimerItem.STATE_RUNNING, TimerItem().state)
    }

    @Test
    fun defaultIdIsZero() {
        assertEquals(0L, TimerItem().id)
    }

    @Test
    fun defaultTotalMillisIsZero() {
        assertEquals(0L, TimerItem().totalMillis)
    }

    @Test
    fun stateRunningIsZero() {
        assertEquals(0, TimerItem.STATE_RUNNING)
    }

    @Test
    fun statePausedIsOne() {
        assertEquals(1, TimerItem.STATE_PAUSED)
    }

    @Test
    fun stateFinishedIsTwo() {
        assertEquals(2, TimerItem.STATE_FINISHED)
    }

    @Test
    fun allThreeStatesAreUnique() {
        val states = setOf(TimerItem.STATE_RUNNING, TimerItem.STATE_PAUSED, TimerItem.STATE_FINISHED)
        assertEquals(3, states.size)
    }

    @Test
    fun remainingMillisCanBeSetIndependentlyFromTotal() {
        val timer = TimerItem().apply {
            totalMillis = 60_000L
            remainingMillis = 30_000L
        }
        assertEquals(60_000L, timer.totalMillis)
        assertEquals(30_000L, timer.remainingMillis)
        assertTrue(timer.remainingMillis < timer.totalMillis)
    }

    @Test
    fun labelCanBeSetAndRetrieved() {
        val timer = TimerItem().apply { label = "Work session" }
        assertEquals("Work session", timer.label)
    }

    @Test
    fun vibrateDefaultsFalse() {
        assertFalse(TimerItem().isVibrate)
    }

    @Test
    fun endTimestampDefaultsToZero() {
        assertEquals(0L, TimerItem().endTimestamp)
    }
}
