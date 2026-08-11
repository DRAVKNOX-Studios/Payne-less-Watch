package com.timely.msminutes.data

class Alarm {
    var id: Long = 0
    var hour: Int = 0
    var minute: Int = 0
    var repeatDays: Int = 0
    var isEnabled: Boolean = false
    var isVibrate: Boolean = false
    var soundUri: String? = null
    var label: String? = null
    var note: String? = null
    var isGradualVolume: Boolean = false
    var snoozeMinutes: Int = 0

    val isRepeating: Boolean
        get() = repeatDays != 0

    fun isDayEnabled(dayIndex: Int): Boolean {
        return (repeatDays and (1 shl dayIndex)) != 0
    }

    fun setDayEnabled(dayIndex: Int, value: Boolean) {
        if (value) {
            repeatDays = repeatDays or (1 shl dayIndex)
        } else {
            repeatDays = repeatDays and (1 shl dayIndex).inv()
        }
    }
}
