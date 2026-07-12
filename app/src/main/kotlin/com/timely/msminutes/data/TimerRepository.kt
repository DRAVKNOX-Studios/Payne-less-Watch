package com.timely.msminutes.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.timely.msminutes.util.AppExecutors
import java.util.function.Consumer

class TimerRepository(context: Context) {
    private val dbHelper: DbHelper = DbHelper.get(context)

    fun insert(item: TimerItem): Long {
        val db = dbHelper.writableDatabase
        val id = db.insert(DbHelper.TABLE_TIMERS, null, toValues(item))
        item.id = id
        return id
    }

    fun update(item: TimerItem) {
        val db = dbHelper.writableDatabase
        db.update(
            DbHelper.TABLE_TIMERS, toValues(item), "id=?",
            arrayOf(item.id.toString())
        )
    }

    fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_TIMERS, "id=?", arrayOf(id.toString()))
    }

    fun getById(id: Long): TimerItem? {
        val db = dbHelper.readableDatabase
        val c = db.query(
            DbHelper.TABLE_TIMERS, null, "id=?",
            arrayOf(id.toString()), null, null, null
        )
        var result: TimerItem? = null
        if (c.moveToFirst()) result = fromCursor(c)
        c.close()
        return result
    }

    val all: MutableList<TimerItem>
        get() {
            val list: MutableList<TimerItem> = ArrayList()
            val db = dbHelper.readableDatabase
            val c =
                db.query(DbHelper.TABLE_TIMERS, null, null, null, null, null, "id")
            while (c.moveToNext()) list.add(fromCursor(c))
            c.close()
            return list
        }

    fun getAllAsync(callback: Consumer<MutableList<TimerItem>>) {
        AppExecutors.get().diskIO {
            val result = all
            AppExecutors.get().mainThread { callback.accept(result) }
        }
    }

    private fun toValues(item: TimerItem): ContentValues {
        val cv = ContentValues()
        cv.put("totalMillis", item.totalMillis)
        cv.put("remainingMillis", item.remainingMillis)
        cv.put("endTimestamp", item.endTimestamp)
        cv.put("state", item.state)
        cv.put("vibrate", if (item.isVibrate) 1 else 0)
        cv.put("soundUri", item.soundUri)
        cv.put("label", item.label)
        return cv
    }

    private fun fromCursor(c: Cursor): TimerItem {
        val t = TimerItem()
        t.id = c.getLong(c.getColumnIndexOrThrow("id"))
        t.totalMillis = c.getLong(c.getColumnIndexOrThrow("totalMillis"))
        t.remainingMillis = c.getLong(c.getColumnIndexOrThrow("remainingMillis"))
        t.endTimestamp = c.getLong(c.getColumnIndexOrThrow("endTimestamp"))
        t.state = c.getInt(c.getColumnIndexOrThrow("state"))
        t.isVibrate = c.getInt(c.getColumnIndexOrThrow("vibrate")) == 1
        t.soundUri = c.getString(c.getColumnIndexOrThrow("soundUri"))
        t.label = c.getString(c.getColumnIndexOrThrow("label"))
        return t
    }
}
