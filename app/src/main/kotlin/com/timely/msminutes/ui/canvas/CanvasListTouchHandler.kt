package com.timely.msminutes.ui.canvas

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.widget.OverScroller
import kotlin.math.abs

class CanvasListTouchHandler(
    private val context: Context,
    private val host: View,
    private val listView: CanvasListView,
    private val scroller: OverScroller
) {
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
    private val MIN_FLING_PX_PER_S = 600f

    internal val swipeHandler = CanvasListSwipeHandler(host, listView)
    private var touchHandledByItem: ItemRenderer? = null

    private var swipeDownX = 0f
    private var swipeDownY = 0f
    private var swipeLockX = 0f
    private var swipeBaseOffset = 0f
    private var isHorizontalLock = false
    private var isVerticalLock = false
    private var wasHorizontalSwipe = false
    private var velocityTracker: VelocityTracker? = null

    private val gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            scroller.forceFinished(true)
            return true
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (isHorizontalLock || touchHandledByItem != null) return false
            if (isVerticalLock) {
                listView.scrollY += distanceY * 1.1f
                listView.clampScroll()
                host.invalidate()
                return true
            }
            return false
        }

        override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            if (isHorizontalLock) return true
            if (!isVerticalLock) return false
            // Use a slight multiplier for a snappier feel
            scroller.fling(0, listView.scrollY.toInt(), 0, (-velocityY * 1.4f).toInt(), 0, 0, 0, listView.maxScroll().toInt())
            host.invalidate()
            return true
        }

        override fun onSingleTapUp(e: MotionEvent): Boolean {
            if (wasHorizontalSwipe) return false
            val dx = e.x - swipeDownX
            val dy = e.y - swipeDownY
            if (dx * dx + dy * dy > touchSlop * touchSlop) return false

            val touchXInList = e.x - listView.bounds.left
            val touchYInList = e.y - listView.bounds.top + listView.scrollY
            for (item in listView.items) {
                if (touchXInList >= item.left && touchXInList <= item.left + (if (item.width > 0) item.width else listView.bounds.width()) &&
                    touchYInList >= item.top && touchYInList <= item.top + item.height) {
                    item.onClick(touchXInList - item.left, touchYInList - item.top)
                    host.invalidate()
                    return true
                }
            }
            return false
        }
    })

    fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x - listView.bounds.left
        val touchY = event.y - listView.bounds.top + listView.scrollY

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.recycle()
                velocityTracker = VelocityTracker.obtain()
            }
        }
        velocityTracker?.addMovement(event)

        touchHandledByItem?.let { item ->
            val handled = item.onTouchEvent(event, touchX, touchY - item.top)
            if (handled) {
                if (event.actionMasked == MotionEvent.ACTION_UP ||
                    event.actionMasked == MotionEvent.ACTION_CANCEL) touchHandledByItem = null
                return true
            } else if (event.actionMasked == MotionEvent.ACTION_MOVE ||
                       event.actionMasked == MotionEvent.ACTION_UP ||
                       event.actionMasked == MotionEvent.ACTION_CANCEL) {
                touchHandledByItem = null
            }
        }

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val idsToClean = swipeHandler.activeAnimators.keys.toList()
                for (id in idsToClean) {
                    val currentX = swipeHandler.swipeStates.getOrDefault(id, 0f)
                    val isDeletion = currentX > listView.bounds.width() * 0.6f
                    if (!isDeletion) {
                        swipeHandler.activeAnimators[id]?.cancel()
                        swipeHandler.activeAnimators.remove(id)
                        swipeHandler.swipeStates.remove(id)
                        swipeHandler.resetItemSwipeX(id, 0f)
                    }
                }

                swipeDownX = event.x
                swipeDownY = event.y
                swipeLockX = event.x
                isHorizontalLock = false
                isVerticalLock = false
                wasHorizontalSwipe = false
                swipeHandler.activeSwipedItemId = null

                var itemHandled = false
                for (item in listView.items) {
                    if (touchX >= item.left && touchX <= item.left + (if (item.width > 0) item.width else listView.bounds.width()) &&
                        touchY >= item.top && touchY <= item.top + item.height) {
                        if (item.onTouchEvent(event, touchX - item.left, touchY - item.top)) {
                            itemHandled = true
                            touchHandledByItem = item
                            break
                        }
                    }
                }
                gestureDetector.onTouchEvent(event)
                return itemHandled || true
            }

            MotionEvent.ACTION_MOVE -> {
                if (touchHandledByItem != null) {
                    gestureDetector.onTouchEvent(event)
                    return true
                }

                val dx = event.x - swipeDownX
                val dy = event.y - swipeDownY
                val absDx = abs(dx)
                val absDy = abs(dy)
                val distSq = dx * dx + dy * dy

                if (!isHorizontalLock && !isVerticalLock && distSq > (touchSlop * 0.8f) * (touchSlop * 0.8f)) {
                    if (absDx >= absDy * 1.1f) {
                        isHorizontalLock = true
                        wasHorizontalSwipe = true
                        swipeLockX = event.x

                        val hitY = swipeDownY - listView.bounds.top + listView.scrollY
                        val hit = listView.items.find { hitY >= it.top && hitY <= it.top + it.height }
                        if (hit != null && hit.id != -1L && hit.isSwipeable) {
                            swipeHandler.activeSwipedItemId = hit.id
                            swipeHandler.activeAnimators[hit.id]?.let { anim ->
                                anim.cancel()
                                swipeHandler.activeAnimators.remove(hit.id)
                            }
                            swipeBaseOffset = swipeHandler.swipeStates.getOrDefault(hit.id, 0f)
                        } else {
                            isHorizontalLock = false
                            wasHorizontalSwipe = false
                        }
                } else if (absDy > absDx * 1.2f) {
                        isVerticalLock = true
                        host.parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                if (isHorizontalLock) {
                    val itemId = swipeHandler.activeSwipedItemId ?: return gestureDetector.onTouchEvent(event)
                    val item = listView.items.find { it.id == itemId } ?: return gestureDetector.onTouchEvent(event)
                    val itemW = if (item.width > 0) item.width else listView.bounds.width()
                    val lockDx = event.x - swipeLockX
                    val newSwipe = (swipeBaseOffset + lockDx).coerceIn(-itemW, itemW)
                    swipeHandler.swipeStates[itemId] = newSwipe
                    swipeHandler.resetItemSwipeX(itemId, newSwipe)
                    host.invalidate()
                    return true
                }

                gestureDetector.onTouchEvent(event)
                return isVerticalLock
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                gestureDetector.onTouchEvent(event)
                touchHandledByItem = null

                var swipeVelocityX = 0f
                velocityTracker?.let { vt ->
                    vt.computeCurrentVelocity(1000)
                    swipeVelocityX = vt.xVelocity
                    vt.recycle()
                    velocityTracker = null
                }

                val swipedId = swipeHandler.activeSwipedItemId
                val swipedItem = listView.items.find { it.id == swipedId }
                val itemW = if (swipedItem != null && swipedItem.width > 0) swipedItem.width else listView.bounds.width()
                val isCancelEvent = event.actionMasked == MotionEvent.ACTION_CANCEL

                if (!isHorizontalLock && !isVerticalLock) {
                    isHorizontalLock = false
                    isVerticalLock = false
                    swipeHandler.activeSwipedItemId = null
                    return true
                }

                val statesToProcess = swipeHandler.swipeStates.toMap()
                for ((itemId, currentSwipe) in statesToProcess) {
                    if (currentSwipe == 0f) {
                        swipeHandler.swipeStates.remove(itemId)
                        swipeHandler.resetItemSwipeX(itemId, 0f)
                        continue
                    }
                    val item = listView.items.find { it.id == itemId }

                    if (itemId == swipedId && !isCancelEvent) {
                        if (currentSwipe > 0f) {
                            val isFlingDelete = swipeVelocityX > MIN_FLING_PX_PER_S
                            val isPastThreshold = currentSwipe > itemW * 0.30f
                            if (isPastThreshold || isFlingDelete) {
                                if (item != null) swipeHandler.animateDeletion(item) else swipeHandler.swipeStates.remove(itemId)
                            } else {
                                if (item != null) swipeHandler.animateSnapBack(item) else {
                                    swipeHandler.swipeStates.remove(itemId)
                                    swipeHandler.resetItemSwipeX(itemId, 0f)
                                }
                            }
                        } else {
                            // Left swipe (Copy)
                            val isFlingCopy = swipeVelocityX < -MIN_FLING_PX_PER_S
                            val isPastThreshold = currentSwipe < -itemW * 0.30f
                            if (isPastThreshold || isFlingCopy) {
                                if (item != null) {
                                    swipeHandler.animateSnapBack(item)
                                    item.onCopy()
                                } else {
                                    swipeHandler.swipeStates.remove(itemId)
                                    swipeHandler.resetItemSwipeX(itemId, 0f)
                                }
                            } else {
                                if (item != null) swipeHandler.animateSnapBack(item) else {
                                    swipeHandler.swipeStates.remove(itemId)
                                    swipeHandler.resetItemSwipeX(itemId, 0f)
                                }
                            }
                        }
                    } else {
                        if (item != null) swipeHandler.animateSnapBack(item) else {
                            swipeHandler.swipeStates.remove(itemId)
                            swipeHandler.resetItemSwipeX(itemId, 0f)
                        }
                    }
                }

                for (item in listView.items) {
                    if (item.swipeX != 0f &&
                        !swipeHandler.swipeStates.containsKey(item.id) &&
                        !swipeHandler.activeAnimators.containsKey(item.id)) {
                        swipeHandler.animateSnapBack(item)
                    }
                }

                isHorizontalLock = false
                isVerticalLock = false
                swipeHandler.activeSwipedItemId = null
                host.invalidate()
                return true
            }
        }

        return gestureDetector.onTouchEvent(event)
    }
}
