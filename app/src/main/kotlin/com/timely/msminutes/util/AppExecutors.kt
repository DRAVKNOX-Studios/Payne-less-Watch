package com.timely.msminutes.util

import android.os.Handler
import android.os.Looper
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutorService
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class AppExecutors private constructor() {
    private val disk: ExecutorService
    private val main: Handler

    init {
        disk = ThreadPoolExecutor(
            1, 1,
            0L, TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(64)
        ) { r, _ ->
            r.run()
        }
        main = Handler(Looper.getMainLooper())
    }

    fun diskIO(r: Runnable?) {
        if (r != null) disk.execute(r)
    }

    fun mainThread(r: Runnable) {
        main.post(r)
    }

    companion object {
        private val INSTANCE = AppExecutors()

        fun get(): AppExecutors = INSTANCE
    }
}
