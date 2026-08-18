package com.timely.msminutes.ui.canvas

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator

class CanvasListSwipeHandler(
    private val host: View,
    private val listView: CanvasListView
) {
    var activeSwipedItemId: Long? = null
    val swipeStates = mutableMapOf<Long, Float>()
    val activeAnimators = mutableMapOf<Long, ValueAnimator>()

    fun resetItemSwipeX(itemId: Long, value: Float) {
        listView.items.forEach { if (it.id == itemId) it.swipeX = value }
    }

    fun animateSnapBack(item: ItemRenderer) {
        val itemId = item.id
        if (itemId == -1L) { item.swipeX = 0f; return }
        activeAnimators[itemId]?.cancel()
        val startX = swipeStates.getOrDefault(itemId, item.swipeX)
        if (startX == 0f) {
            activeAnimators.remove(itemId)
            swipeStates.remove(itemId)
            resetItemSwipeX(itemId, 0f)
            return
        }
        val animator = ValueAnimator.ofFloat(startX, 0f).apply {
            duration = 250
            interpolator = DecelerateInterpolator(1.5f)
            addUpdateListener {
                val v = it.animatedValue as Float
                swipeStates[itemId] = v
                resetItemSwipeX(itemId, v)
                host.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (activeAnimators[itemId] === animation) {
                        activeAnimators.remove(itemId)
                        swipeStates.remove(itemId)
                        resetItemSwipeX(itemId, 0f)
                    } else if (!activeAnimators.containsKey(itemId)) {
                        swipeStates.remove(itemId)
                        resetItemSwipeX(itemId, 0f)
                    }
                    host.invalidate()
                }
            })
        }
        activeAnimators[itemId] = animator
        animator.start()
    }

    fun animateDeletion(item: ItemRenderer) {
        val itemId = item.id
        if (itemId == -1L) return
        activeAnimators[itemId]?.cancel()
        val startX = swipeStates.getOrDefault(itemId, item.swipeX)
        val endX = if (item.width > 0) item.width else listView.bounds.width()
        val animator = ValueAnimator.ofFloat(startX, endX).apply {
            duration = 200
            interpolator = AccelerateInterpolator(1.2f)
            addUpdateListener {
                val v = it.animatedValue as Float
                swipeStates[itemId] = v
                resetItemSwipeX(itemId, v)
                host.invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (activeAnimators[itemId] === animation) {
                        activeAnimators.remove(itemId)
                        swipeStates.remove(itemId)
                        resetItemSwipeX(itemId, endX)
                        item.onDelete()
                    } else if (!activeAnimators.containsKey(itemId)) {
                        swipeStates.remove(itemId)
                        resetItemSwipeX(itemId, 0f)
                    }
                    host.invalidate()
                }
            })
        }
        activeAnimators[itemId] = animator
        animator.start()
    }

    fun isAnimating(): Boolean =
        activeSwipedItemId != null ||
        swipeStates.any { it.value != 0f } ||
        activeAnimators.isNotEmpty()
}
