package com.timely.msminutes.ui.timer

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.ImageViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.ui.view.SharedViewHolder
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import kotlin.math.max

class TimerAdapter(private val listener: Listener) : RecyclerView.Adapter<SharedViewHolder>() {

    interface Listener {
        fun onPauseResume(item: TimerItem?)
        fun onCancel(item: TimerItem?)
        fun onReset(item: TimerItem?)
    }

    private val items: MutableList<TimerItem> = ArrayList()
    private var tokens: ThemeTokens? = ThemeStore.get().current()

    fun setTokens(t: ThemeTokens?) {
        tokens = t
        notifyDataSetChanged()
    }

    fun submit(newItems: List<TimerItem>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newItems.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].id == newItems[n].id
            override fun areContentsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = newItems[n]
                return a.state == b.state &&
                    a.remainingMillis == b.remainingMillis &&
                    a.label == b.label
            }
        })
        items.clear()
        items.addAll(newItems)
        diff.dispatchUpdatesTo(this)
    }

    fun refreshTimes() {
        notifyItemRangeChanged(0, itemCount, PAYLOAD_TICK)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return SharedViewHolder(view)
    }

    override fun onBindViewHolder(holder: SharedViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && payloads[0] == PAYLOAD_TICK) {
            val item = items.getOrNull(position) ?: return
            bindTick(holder, item)
            return
        }
        onBindViewHolder(holder, position)
    }

    override fun onBindViewHolder(holder: SharedViewHolder, position: Int) {
        val item = items[position]

        holder.deleteBg?.visibility = View.GONE
        holder.foregroundCard?.translationX = 0f

        bindTick(holder, item)
        holder.secondaryText?.text = if (item.label.isNullOrEmpty()) "Timer" else item.label
        
        holder.toggle?.visibility = View.GONE
        holder.tertiaryText?.visibility = View.GONE
        holder.accentText?.visibility = View.GONE
        holder.dateText?.visibility = View.GONE
        holder.actionBtn?.visibility = View.VISIBLE
        holder.iconBtn?.visibility = View.VISIBLE

        holder.actionBtn?.setOnClickListener { listener.onPauseResume(item) }
        holder.iconBtn?.setOnClickListener      { listener.onReset(item) }

        val t = tokens ?: return
        ThemeApplier.applyCard(holder.foregroundCard, t, t.isCustom)
        ThemeApplier.applyCardDelete(holder.deleteBg, t, t.isCustom)

        ThemeApplier.applyTextPrimary(holder.primaryText, t)
        ThemeApplier.applyTextSecondary(holder.secondaryText, t)
        ThemeApplier.applyAccentButton(holder.actionBtn, t)

        holder.iconBtn?.let {
            ImageViewCompat.setImageTintList(it, ColorStateList.valueOf(t.textPrimary))
        }
    }

    private fun bindTick(holder: SharedViewHolder, item: TimerItem) {
        val displayMillis = when (item.state) {
            TimerItem.STATE_RUNNING ->
                max(0L, item.endTimestamp - System.currentTimeMillis())
            else -> item.remainingMillis
        }
        holder.primaryText?.text = TimeFormatUtil.formatTimer(displayMillis)
        holder.actionBtn?.text = when (item.state) {
            TimerItem.STATE_RUNNING  -> "Pause"
            TimerItem.STATE_FINISHED -> "Restart"
            else                     -> "Resume"
        }
        tokens?.let { ThemeApplier.applyAccentButton(holder.actionBtn, it) }
    }

    override fun getItemViewType(position: Int): Int = 1
    override fun getItemCount(): Int = items.size
    fun getItem(pos: Int): TimerItem? = items.getOrNull(pos)

    companion object {
        private const val PAYLOAD_TICK = "tick"
    }
}
