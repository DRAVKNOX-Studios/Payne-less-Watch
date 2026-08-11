package com.timely.msminutes.widget

data class WidgetState(
    val minute: Long = 0,
    val backgroundColor: Int = 0,
    val accentColor: Int = 0,
    val fontColor: Int = 0,
    val isWidgetTransparent: Boolean = false,
    val widgetNote: String? = null,
    val is24Hour: Boolean = true,
    val pupilOffsets: List<Float> = emptyList(),
    val alarmInfo: String? = null,
    val alarmLabel: String? = null,
    val timerMillis: Long? = null,
    val timerLabel: String? = null,
    val stopwatchMillis: Long? = null,
)
