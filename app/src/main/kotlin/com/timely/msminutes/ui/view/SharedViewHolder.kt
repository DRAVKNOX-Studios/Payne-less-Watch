package com.timely.msminutes.ui.view

import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R

/**
 * A shared ViewHolder used by multiple adapters (Alarm, Timer, WorldClock)
 * to maximize view reuse across the entire app.
 *
 * All fields are nullable because different layouts (item_alarm, item_world_clock)
 * are shared within the same RecycledViewPool using the same itemViewType.
 */
class SharedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val foregroundCard: View?   = itemView.findViewById(R.id.foreground_card)
    val deleteBg: View?         = itemView.findViewById(R.id.delete_background)
    val primaryText: TextView?  = itemView.findViewById(R.id.text_primary)
    val secondaryText: TextView? = itemView.findViewById(R.id.text_secondary)
    val tertiaryText: TextView? = itemView.findViewById(R.id.text_tertiary)
    val accentText: TextView?   = itemView.findViewById(R.id.text_accent)
    val toggle: SwitchCompat?   = itemView.findViewById(R.id.switch_enabled)
    val actionBtn: Button?      = itemView.findViewById(R.id.btn_action)
    val iconBtn: ImageView?     = itemView.findViewById(R.id.btn_icon)
    val dateText: TextView?     = itemView.findViewById(R.id.text_date)
}
