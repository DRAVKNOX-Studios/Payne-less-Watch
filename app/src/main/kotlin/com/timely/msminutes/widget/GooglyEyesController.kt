package com.timely.msminutes.widget

import android.content.Context
import com.timely.msminutes.data.Prefs
import java.util.Calendar
import kotlin.math.abs
import kotlin.math.sign

object GooglyEyesController {
    private val EYE_POSITIONS = arrayOf(
        floatArrayOf(0f, 0f),
        floatArrayOf(-6f, 0f),
        floatArrayOf(6f, 0f),
        floatArrayOf(0f, -6f),
        floatArrayOf(0f, 6f),
        floatArrayOf(-4f, -4f),
        floatArrayOf(4f, -4f),
        floatArrayOf(-4f, 4f),
        floatArrayOf(4f, 4f),
    )

    private val currentPos = Array(4) { floatArrayOf(0f, 0f) }
    private val targetIdx = IntArray(4) { 0 }
    private val targetChangeTime = LongArray(4) { 0L }
    private val holdDurations = longArrayOf(2800L, 3500L, 2200L, 4000L)
    private val phaseOffsets = longArrayOf(0L, 1100L, 600L, 1800L)
    private const val INTERP_SPEED_DP_PER_MS = 0.025f
    private var initialised = false

    private fun pickNextTarget(eyeIdx: Int): Int {
        var next: Int
        do {
            next = ((System.currentTimeMillis() / 1L + eyeIdx * 31L + phaseOffsets[eyeIdx]) % EYE_POSITIONS.size).toInt()
        } while (next == targetIdx[eyeIdx])
        return next
    }

    fun tick(now: Long) {
        if (!initialised) {
            for (i in 0..3) {
                targetIdx[i] = (i * 2 + 1) % EYE_POSITIONS.size
                targetChangeTime[i] = now - phaseOffsets[i]
            }
            initialised = true
        }

        for (i in 0..3) {
            val elapsed = now - targetChangeTime[i]
            if (elapsed >= holdDurations[i]) {
                val next = (targetIdx[i] + i + 3) % EYE_POSITIONS.size
                targetIdx[i] = if (next == targetIdx[i]) (next + 1) % EYE_POSITIONS.size else next
                targetChangeTime[i] = now
            }

            val target = EYE_POSITIONS[targetIdx[i]]
            val cur = currentPos[i]
            val maxStep = INTERP_SPEED_DP_PER_MS * 100f
            cur[0] = lerpStep(cur[0], target[0], maxStep)
            cur[1] = lerpStep(cur[1], target[1], maxStep)
        }
    }

    private fun lerpStep(current: Float, target: Float, maxStep: Float): Float {
        val diff = target - current
        return if (abs(diff) <= maxStep) target
        else current + sign(diff) * maxStep
    }

    fun reset() {
        initialised = false
        for (i in 0..3) {
            currentPos[i][0] = 0f
            currentPos[i][1] = 0f
            targetIdx[i] = 0
        }
    }

    fun getPupilOffset(eyeIdx: Int): FloatArray {
        return if (eyeIdx in currentPos.indices) currentPos[eyeIdx] else floatArrayOf(0f, 0f)
    }

    fun isEasterEggMinute(context: Context): Boolean {
        val prefs = Prefs(context)
        val cal = Calendar.getInstance()
        return prefs.is24Hour() && cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0
    }
}
