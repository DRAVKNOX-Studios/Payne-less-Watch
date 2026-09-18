package com.timely.msminutes.data

data class StopwatchHistoryItem(
    var id: Long = 0,
    var label: String = "",
    var elapsedTime: Long = 0,
    var laps: String = "",
    var timestamp: Long = System.currentTimeMillis()
)
