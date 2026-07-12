package com.timely.msminutes.view

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout

class SwipeDismissView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    interface Listener {
        fun onSwipeUpDismiss()
        fun onSwipeDownSnooze()
    }

    private var listener: Listener? = null
    private var startY = 0f
    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.getAction()) {
            MotionEvent.ACTION_DOWN -> {
                startY = event.getY()
                return true
            }

            MotionEvent.ACTION_UP -> {
                val deltaY = event.getY() - startY
                if (listener != null) {
                    if (deltaY < -THRESHOLD) {
                        listener!!.onSwipeUpDismiss()
                    } else if (deltaY > THRESHOLD) {
                        listener!!.onSwipeDownSnooze()
                    }
                }
                return true
            }

            else -> return true
        }
    }

    companion object {
        private const val THRESHOLD = 180f
    }
}
