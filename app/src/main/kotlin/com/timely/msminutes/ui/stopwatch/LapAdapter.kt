package com.timely.msminutes.ui.stopwatch

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.ui.view.SharedViewHolder
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens

class LapAdapter(private val laps: MutableList<String?>) : RecyclerView.Adapter<SharedViewHolder>() {
    private var tokens: ThemeTokens? = ThemeStore.get().current()

    fun setTokens(t: ThemeTokens?) {
        tokens = t
        notifyDataSetChanged()
    }

    /** DiffUtil swap — avoids a full rebind when only the newest lap changes. */
    fun submitLaps(newLaps: List<String?>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = laps.size
            override fun getNewListSize() = newLaps.size
            override fun areItemsTheSame(o: Int, n: Int) = laps[o] == newLaps[n]
            override fun areContentsTheSame(o: Int, n: Int) = laps[o] == newLaps[n]
        })
        laps.clear()
        laps.addAll(newLaps)
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int = 3

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedViewHolder {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_lap, parent, false)
        return SharedViewHolder(v)
    }

    override fun onBindViewHolder(holder: SharedViewHolder, position: Int) {
        holder.secondaryText?.text = (laps.size - position).toString()
        holder.primaryText?.text   = laps[position]
        
        tokens?.let {
            ThemeApplier.applyTextSecondary(holder.secondaryText, it)
            ThemeApplier.applyTextPrimary(holder.primaryText, it)
        }
    }

    override fun getItemCount() = laps.size
}
