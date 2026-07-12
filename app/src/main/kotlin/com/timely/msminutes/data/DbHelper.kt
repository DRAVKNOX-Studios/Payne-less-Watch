package com.timely.msminutes.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DbHelper private constructor(context: Context?) :
    SQLiteOpenHelper(context, DB_NAME, null, DB_VERSION) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            ("CREATE TABLE " + TABLE_ALARMS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "hour INTEGER,"
                    + "minute INTEGER,"
                    + "repeatDays INTEGER,"
                    + "enabled INTEGER,"
                    + "vibrate INTEGER,"
                    + "soundUri TEXT,"
                    + "label TEXT,"
                    + "note TEXT,"
                    + "gradualVolume INTEGER,"
                    + "snoozeMinutes INTEGER"
                    + ")")
        )

        db.execSQL(
            ("CREATE TABLE " + TABLE_TIMERS + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "totalMillis INTEGER,"
                    + "remainingMillis INTEGER,"
                    + "endTimestamp INTEGER,"
                    + "state INTEGER,"
                    + "vibrate INTEGER,"
                    + "soundUri TEXT,"
                    + "label TEXT"
                    + ")")
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            migrateV2toV3(db)
        }
    }

    private fun migrateV2toV3(db: SQLiteDatabase) {
        db.execSQL("ALTER TABLE " + TABLE_ALARMS + " ADD COLUMN note TEXT")
    }

    companion object {
        private const val DB_NAME = "timely.db"
        private const val DB_VERSION = 3

        const val TABLE_ALARMS: String = "alarms"
        const val TABLE_TIMERS: String = "timers"

        private var instance: DbHelper? = null

        @Synchronized
        fun get(context: Context): DbHelper {
            if (instance == null) {
                instance = DbHelper(context.getApplicationContext())
            }
            return instance!!
        }
    }
}
