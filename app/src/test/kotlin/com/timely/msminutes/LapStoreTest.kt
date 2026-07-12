package com.timely.msminutes

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class LapStoreTest {

    private lateinit var store: FakeLapStore

    @Before
    fun setUp() {
        store = FakeLapStore()
    }

    @Test
    fun emptyStoreHasNoLaps() {
        assertTrue(store.getLaps().isEmpty())
    }

    @Test
    fun addLapIncreasesCount() {
        store.addLap(1000L)
        assertEquals(1, store.getLaps().size)
    }

    @Test
    fun lapsAreReturnedInInsertionOrder() {
        store.addLap(1000L)
        store.addLap(2000L)
        store.addLap(3000L)
        val laps = store.getLaps()
        assertEquals(1000L, laps[0])
        assertEquals(2000L, laps[1])
        assertEquals(3000L, laps[2])
    }

    @Test
    fun clearRemovesAllLaps() {
        store.addLap(500L)
        store.addLap(750L)
        store.clear()
        assertTrue(store.getLaps().isEmpty())
    }

    @Test
    fun lapCountMatchesInsertedCount() {
        repeat(10) { store.addLap(it * 1000L) }
        assertEquals(10, store.getLaps().size)
    }

    @Test
    fun getLapsReturnsCopyNotReference() {
        store.addLap(1000L)
        val laps = store.getLaps()
        store.addLap(2000L)
        assertEquals(1, laps.size)
    }

    @Test
    fun zeroMillisLapIsValid() {
        store.addLap(0L)
        assertEquals(1, store.getLaps().size)
        assertEquals(0L, store.getLaps()[0])
    }

    private class FakeLapStore {
        private val laps = mutableListOf<Long>()
        fun addLap(millis: Long) { laps.add(millis) }
        fun getLaps(): List<Long> = laps.toList()
        fun clear() { laps.clear() }
    }
}
