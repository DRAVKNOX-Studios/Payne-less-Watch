package com.timely.msminutes.util

import android.content.Context
import android.os.PowerManager

object WakeLockHolder {
    private var lock: PowerManager.WakeLock? = null

    @Synchronized
    fun acquire(context: Context) {
        if (lock?.isHeld == true) return
        val pm = context.applicationContext
            .getSystemService(Context.POWER_SERVICE) as PowerManager
        lock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "timely:alarm_wakeup"
        ).also {
            it.acquire(60_000L)
        }
    }

    @Synchronized
    fun release() {
        lock?.let { if (it.isHeld) it.release() }
        lock = null
    }
}
