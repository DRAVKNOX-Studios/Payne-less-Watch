package com.timely.msminutes.data

class TimerItem {
    var id: Long = 0
    var totalMillis: Long = 0
    var remainingMillis: Long = 0
    var endTimestamp: Long = 0
    var state: Int = 0
    var isVibrate: Boolean = false
    var soundUri: String? = null
    var label: String? = null

    companion object {
        const val STATE_RUNNING: Int = 0
        const val STATE_PAUSED: Int = 1
        const val STATE_FINISHED: Int = 2
    }
}
