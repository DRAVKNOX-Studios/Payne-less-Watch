package com.timely.msminutes.data

object LapStore {
    private const val SEPARATOR = "\n"

    fun encode(laps: List<String?>?): String {
        if (laps.isNullOrEmpty()) return ""
        val sb = StringBuilder(laps.size * 12)
        for (i in laps.indices) {
            if (i > 0) sb.append(SEPARATOR)
            sb.append(laps[i])
        }
        return sb.toString()
    }

    fun decode(stored: String?): MutableList<String> {
        if (stored.isNullOrEmpty()) return mutableListOf()
        val parts = stored.split(SEPARATOR)
        val result = ArrayList<String>(parts.size)
        for (entry in parts) {
            if (entry.isNotEmpty()) result.add(entry)
        }
        return result
    }
}
