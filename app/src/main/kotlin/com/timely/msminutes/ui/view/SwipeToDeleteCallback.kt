package com.timely.msminutes.ui.view

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R

/**
 * A generic ItemTouchHelper.SimpleCallback that handles swipe-to-reveal delete backgrounds.
 * Expects item layouts to have a View with id 'foreground_card' and 'delete_background'.
 */
class SwipeToDeleteCallback(
    private val onSwiped: (position: Int) -> Unit
) : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean = false

    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.4f

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        val pos = viewHolder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION) {
            onSwiped(pos)
        }
    }

    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean
    ) {
        val foregroundCard = viewHolder.itemView.findViewById<View>(R.id.foreground_card)
        val deleteBackground = viewHolder.itemView.findViewById<View>(R.id.delete_background)

        if (foregroundCard != null) {
            foregroundCard.translationX = dX
        }

        if (deleteBackground != null) {
            val threshold = 8f * viewHolder.itemView.resources.displayMetrics.density
            deleteBackground.visibility = if (dX > threshold) View.VISIBLE else View.GONE
        }
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        val foregroundCard = viewHolder.itemView.findViewById<View>(R.id.foreground_card)
        val deleteBackground = viewHolder.itemView.findViewById<View>(R.id.delete_background)

        foregroundCard?.translationX = 0f
        deleteBackground?.visibility = View.GONE

        super.clearView(recyclerView, viewHolder)
    }
}
