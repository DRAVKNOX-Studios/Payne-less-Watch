package com.timely.msminutes.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import com.timely.msminutes.util.AppExecutors
import java.util.function.Consumer

class AlarmRepository(context: Context) {
    private val dbHelper: DbHelper = DbHelper.get(context)

    fun insert(alarm: Alarm): Long {
        val db = dbHelper.writableDatabase
        val id = db.insert(DbHelper.TABLE_ALARMS, null, toValues(alarm))
        alarm.id = id
        return id
    }

    fun update(alarm: Alarm) {
        val db = dbHelper.writableDatabase
        db.update(
            DbHelper.TABLE_ALARMS, toValues(alarm), "id=?",
            arrayOf(alarm.id.toString())
        )
    }

    fun delete(id: Long) {
        val db = dbHelper.writableDatabase
        db.delete(DbHelper.TABLE_ALARMS, "id=?", arrayOf(id.toString()))
    }

    fun getById(id: Long): Alarm? {
        val db = dbHelper.readableDatabase
        val c = db.query(
            DbHelper.TABLE_ALARMS, null, "id=?",
            arrayOf(id.toString()), null, null, null
        )
        var result: Alarm? = null
        if (c.moveToFirst()) result = fromCursor(c)
        c.close()
        return result
    }

    fun forEach(block: (Alarm) -> Unit) {
        val db = dbHelper.readableDatabase
        val c = db.query(
            DbHelper.TABLE_ALARMS, null, null, null, null, null, "hour,minute"
        )
        try {
            while (c.moveToNext()) block(fromCursor(c))
        } finally {
            c.close()
        }
    }

    val all: MutableList<Alarm>
        get() {
            val list = mutableListOf<Alarm>()
            forEach { list.add(it) }
            return list
        }

    fun getAllAsync(callback: Consumer<MutableList<Alarm>>) {
        AppExecutors.get().diskIO {
            val result = all
            AppExecutors.get().mainThread { callback.accept(result) }
        }
    }

    private fun toValues(alarm: Alarm): ContentValues {
        val cv = ContentValues()
        cv.put("hour", alarm.hour)
        cv.put("minute", alarm.minute)
        cv.put("repeatDays", alarm.repeatDays)
        cv.put("enabled", if (alarm.isEnabled) 1 else 0)
        cv.put("vibrate", if (alarm.isVibrate) 1 else 0)
        cv.put("soundUri", alarm.soundUri)
        cv.put("label", alarm.label)
        cv.put("note", alarm.note)
        cv.put("gradualVolume", if (alarm.isGradualVolume) 1 else 0)
        cv.put("snoozeMinutes", alarm.snoozeMinutes)
        return cv
    }

    private fun fromCursor(c: Cursor): Alarm {
        val a = Alarm()
        a.id = c.getLong(c.getColumnIndexOrThrow("id"))
        a.hour = c.getInt(c.getColumnIndexOrThrow("hour"))
        a.minute = c.getInt(c.getColumnIndexOrThrow("minute"))
        a.repeatDays = c.getInt(c.getColumnIndexOrThrow("repeatDays"))
        a.isEnabled = c.getInt(c.getColumnIndexOrThrow("enabled")) == 1
        a.isVibrate = c.getInt(c.getColumnIndexOrThrow("vibrate")) == 1
        a.soundUri = c.getString(c.getColumnIndexOrThrow("soundUri"))
        a.label = c.getString(c.getColumnIndexOrThrow("label"))
        val noteIdx = c.getColumnIndex("note")
        a.note = if (noteIdx >= 0) c.getString(noteIdx) else null
        a.isGradualVolume = c.getInt(c.getColumnIndexOrThrow("gradualVolume")) == 1
        a.snoozeMinutes = c.getInt(c.getColumnIndexOrThrow("snoozeMinutes"))
        return a
    }
}
