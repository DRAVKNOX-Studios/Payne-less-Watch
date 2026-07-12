package com.timely.msminutes.ui.alarm

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.view.SharedViewHolder
import com.timely.msminutes.util.AlarmTimeUtil
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.ThemeUtil
import com.timely.msminutes.util.TimeFormatUtil

class AlarmAdapter(private val listener: Listener) : RecyclerView.Adapter<SharedViewHolder>() {

    interface Listener {
        fun onToggle(alarm: Alarm?, enabled: Boolean)
        fun onClick(alarm: Alarm?)
        fun onDelete(alarm: Alarm?)
    }

    private val items: MutableList<Alarm> = ArrayList()
    private var tokens: ThemeTokens? = ThemeStore.get().current()

    fun setTokens(t: ThemeTokens?) {
        tokens = t
        notifyDataSetChanged()
    }

    fun submit(newAlarms: MutableList<Alarm>) {
        val diff = DiffUtil.calculateDiff(object : DiffUtil.Callback() {
            override fun getOldListSize() = items.size
            override fun getNewListSize() = newAlarms.size
            override fun areItemsTheSame(o: Int, n: Int) = items[o].id == newAlarms[n].id
            override fun areContentsTheSame(o: Int, n: Int): Boolean {
                val a = items[o]; val b = newAlarms[n]
                return a.hour == b.hour && a.minute == b.minute &&
                    a.isEnabled == b.isEnabled && a.label == b.label &&
                    a.repeatDays == b.repeatDays
            }
        })
        items.clear()
        items.addAll(newAlarms)
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alarm, parent, false)
        return SharedViewHolder(view)
    }

    override fun onBindViewHolder(holder: SharedViewHolder, position: Int) {
        val alarm = items[position]
        val prefs = Prefs(holder.itemView.context)

        holder.deleteBg?.visibility = View.GONE
        holder.foregroundCard?.translationX = 0f

        holder.primaryText?.text = TimeFormatUtil.formatClock(alarm.hour, alarm.minute, prefs.is24Hour())
        holder.secondaryText?.text = if (alarm.label.isNullOrEmpty()) "Alarm" else alarm.label
        holder.tertiaryText?.text = buildDaysText(alarm)

        val remaining = AlarmTimeUtil.getRemainingTimeText(alarm)
        holder.accentText?.text = remaining ?: ""
        holder.accentText?.visibility = if (remaining != null) View.VISIBLE else View.GONE

        holder.toggle?.setOnCheckedChangeListener(null)
        holder.toggle?.isChecked = alarm.isEnabled
        holder.toggle?.visibility = View.VISIBLE
        holder.toggle?.setOnCheckedChangeListener { _, checked -> listener.onToggle(alarm, checked) }

        holder.actionBtn?.visibility = View.GONE
        holder.iconBtn?.visibility = View.GONE
        holder.dateText?.visibility = View.GONE

        tokens?.let { t ->
            ThemeApplier.applyTextPrimary(holder.primaryText, t)
            ThemeApplier.applyTextPrimary(holder.secondaryText, t)
            ThemeApplier.applyTextSecondary(holder.tertiaryText, t)
            ThemeApplier.applyTextSecondary(holder.accentText, t)
            
            holder.toggle?.let { ThemeUtil.tintWidget(it, t.accent, t.background) }
            ThemeApplier.applyCard(holder.foregroundCard, t, t.isCustom)
            ThemeApplier.applyCardDelete(holder.deleteBg, t, t.isCustom)
        }

        holder.foregroundCard?.setOnClickListener { listener.onClick(alarm) }
    }

    private fun buildDaysText(alarm: Alarm): String {
        if (!alarm.isRepeating) return "One time"
        if (alarm.repeatDays == 127) return "Everyday"
        if (alarm.repeatDays == 31)  return "Weekdays"
        if (alarm.repeatDays == 96)  return "Weekend"
        val sb = StringBuilder()
        for (i in 0..6) {
            if (alarm.isDayEnabled(i)) {
                if (sb.isNotEmpty()) sb.append(" ")
                sb.append(DAY_LABELS[i])
            }
        }
        return sb.toString()
    }

    override fun getItemViewType(position: Int): Int = 1
    override fun getItemCount(): Int = items.size
    fun getItem(position: Int): Alarm? = items.getOrNull(position)

    companion object {
        private val DAY_LABELS = arrayOf("M", "T", "W", "T", "F", "S", "S")
    }
}
