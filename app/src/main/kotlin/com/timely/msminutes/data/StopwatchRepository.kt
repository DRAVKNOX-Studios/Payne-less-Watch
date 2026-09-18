package com.timely.msminutes.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.timely.msminutes.util.AppExecutors
import java.util.function.Consumer

class StopwatchRepository(context: Context) {
    private val dbHelper: DbHelper = DbHelper.get(context)

    fun insert(item: StopwatchHistoryItem): Long {
        val db = dbHelper.writableDatabase
        val id = db.insert(DbHelper.TABLE_STOPWATCH_HISTORY, null, toValues(item))
        item.id = id
        return id
    }

    fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_STOPWATCH_HISTORY, "id=?", arrayOf(id.toString()))
    }

    fun deleteAll() {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_STOPWATCH_HISTORY, null, null)
    }

    val all: List<StopwatchHistoryItem>
        get() {
            val list = mutableListOf<StopwatchHistoryItem>()
            val db = dbHelper.readableDatabase
            val c = db.query(
                DbHelper.TABLE_STOPWATCH_HISTORY, null, null, null, null, null, "timestamp DESC"
            )
            while (c.moveToNext()) list.add(fromCursor(c))
            c.close()
            return list
        }

    fun getAllAsync(callback: Consumer<List<StopwatchHistoryItem>>) {
        AppExecutors.get().diskIO {
            val result = all
            AppExecutors.get().mainThread { callback.accept(result) }
        }
    }

    private fun toValues(item: StopwatchHistoryItem): ContentValues {
        val cv = ContentValues()
        cv.put("label", item.label)
        cv.put("elapsedTime", item.elapsedTime)
        cv.put("laps", item.laps)
        cv.put("timestamp", item.timestamp)
        return cv
    }

    private fun fromCursor(c: Cursor): StopwatchHistoryItem {
        val item = StopwatchHistoryItem()
        item.id = c.getLong(c.getColumnIndexOrThrow("id"))
        item.label = c.getString(c.getColumnIndexOrThrow("label"))
        item.elapsedTime = c.getLong(c.getColumnIndexOrThrow("elapsedTime"))
        item.laps = c.getString(c.getColumnIndexOrThrow("laps"))
        item.timestamp = c.getLong(c.getColumnIndexOrThrow("timestamp"))
        return item
    }
}
