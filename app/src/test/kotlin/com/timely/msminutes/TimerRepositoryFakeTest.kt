package com.timely.msminutes

import com.timely.msminutes.data.TimerItem
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class TimerRepositoryFakeTest {

    private lateinit var store: FakeTimerStore

    @Before
    fun setUp() {
        store = FakeTimerStore()
    }

    @Test
    fun insertAddsItem() {
        store.insert(makeTimer(60_000, TimerItem.STATE_PAUSED))
        assertEquals(1, store.getAll().size)
    }

    @Test
    fun insertAssignsId() {
        val timer = makeTimer(30_000, TimerItem.STATE_RUNNING)
        val id = store.insert(timer)
        assertTrue(id > 0)
        assertEquals(id, timer.id)
    }

    @Test
    fun updateChangesState() {
        val timer = makeTimer(60_000, TimerItem.STATE_RUNNING)
        store.insert(timer)
        timer.state = TimerItem.STATE_PAUSED
        store.update(timer)
        assertEquals(TimerItem.STATE_PAUSED, store.getAll()[0].state)
    }

    @Test
    fun deleteRemovesItem() {
        val timer = makeTimer(60_000, TimerItem.STATE_RUNNING)
        val id = store.insert(timer)
        store.delete(id)
        assertTrue(store.getAll().isEmpty())
    }

    @Test
    fun getAllReturnsAllInserted() {
        store.insert(makeTimer(10_000, TimerItem.STATE_RUNNING))
        store.insert(makeTimer(20_000, TimerItem.STATE_PAUSED))
        store.insert(makeTimer(30_000, TimerItem.STATE_FINISHED))
        assertEquals(3, store.getAll().size)
    }

    @Test
    fun deleteNonExistentIdIsNoOp() {
        store.insert(makeTimer(5_000, TimerItem.STATE_RUNNING))
        store.delete(999L)
        assertEquals(1, store.getAll().size)
    }

    @Test
    fun updateNonExistentIdIsNoOp() {
        val timer = makeTimer(5_000, TimerItem.STATE_RUNNING).apply { id = 999L }
        store.update(timer)
        assertTrue(store.getAll().isEmpty())
    }

    @Test
    fun stateConstantsAreDistinct() {
        assertNotEquals(TimerItem.STATE_RUNNING, TimerItem.STATE_PAUSED)
        assertNotEquals(TimerItem.STATE_PAUSED, TimerItem.STATE_FINISHED)
        assertNotEquals(TimerItem.STATE_RUNNING, TimerItem.STATE_FINISHED)
    }

    @Test
    fun multipleInsertsHaveIncrementingIds() {
        val id1 = store.insert(makeTimer(1_000, TimerItem.STATE_RUNNING))
        val id2 = store.insert(makeTimer(2_000, TimerItem.STATE_PAUSED))
        val id3 = store.insert(makeTimer(3_000, TimerItem.STATE_FINISHED))
        assertTrue(id1 < id2)
        assertTrue(id2 < id3)
    }

    @Test
    fun updatePreservesOtherFields() {
        val timer = makeTimer(60_000, TimerItem.STATE_RUNNING).apply { label = "original" }
        store.insert(timer)
        timer.state = TimerItem.STATE_PAUSED
        store.update(timer)
        val updated = store.getAll()[0]
        assertEquals("original", updated.label)
        assertEquals(60_000L, updated.totalMillis)
    }

    @Test
    fun insertReturnsCopyNotReference() {
        val timer = makeTimer(60_000, TimerItem.STATE_RUNNING)
        store.insert(timer)
        timer.state = TimerItem.STATE_FINISHED
        assertEquals(TimerItem.STATE_RUNNING, store.getAll()[0].state)
    }

    private fun makeTimer(totalMillis: Long, state: Int) = TimerItem().apply {
        this.totalMillis = totalMillis
        remainingMillis = totalMillis
        endTimestamp = 0L
        this.state = state
        isVibrate = false
        soundUri = null
        label = "test"
    }

    private class FakeTimerStore {
        private val items = mutableListOf<TimerItem>()
        private var nextId = 1L

        fun insert(item: TimerItem): Long {
            item.id = nextId++
            items.add(copyOf(item))
            return item.id
        }

        fun update(item: TimerItem) {
            val idx = items.indexOfFirst { it.id == item.id }
            if (idx >= 0) items[idx] = copyOf(item)
        }

        fun delete(id: Long) {
            items.removeAll { it.id == id }
        }

        fun getAll(): List<TimerItem> = items.toList()

        private fun copyOf(src: TimerItem) = TimerItem().apply {
            id = src.id
            totalMillis = src.totalMillis
            remainingMillis = src.remainingMillis
            endTimestamp = src.endTimestamp
            state = src.state
            isVibrate = src.isVibrate
            soundUri = src.soundUri
            label = src.label
        }
    }
}
