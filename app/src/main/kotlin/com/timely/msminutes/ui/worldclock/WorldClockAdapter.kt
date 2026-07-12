package com.timely.msminutes.ui.worldclock

import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.ui.view.SharedViewHolder
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeTokens
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

data class TimeTick(val time: String, val date: String)

class WorldClockAdapter(
    zoneIds: List<String>,
    is24Hour: Boolean
) : RecyclerView.Adapter<SharedViewHolder>() {

    @Volatile private var timeFmt: DateTimeFormatter =
        DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm:ss" else "hh:mm:ss a", Locale.US)
    private val dateFmt = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.US)

    fun setIs24Hour(is24Hour: Boolean) {
        timeFmt = DateTimeFormatter.ofPattern(if (is24Hour) "HH:mm:ss" else "hh:mm:ss a", Locale.US)
        notifyItemRangeChanged(0, rows.size)
    }

    @Volatile private var tokens: ThemeTokens? = null

    fun setTokens(t: ThemeTokens?) {
        tokens = t
        notifyItemRangeChanged(0, rows.size)
    }

    private data class RowMeta(
        val zoneId:    ZoneId,
        val city:      String,
        val region:    String,
        val offsetStr: String
    )

    private val rows: List<RowMeta> = zoneIds.map { zoneStr ->
        val zid    = ZoneId.of(zoneStr)
        val parts  = zoneStr.split("/")
        val city   = parts.last().replace("_", " ")
        val region = if (parts.size > 1) parts.first() else ""
        val now    = ZonedDateTime.now(zid)
        val secs   = zid.rules.getOffset(now.toInstant()).totalSeconds
        val sign   = if (secs >= 0) "+" else "-"
        val abs    = Math.abs(secs)
        val offset = "UTC$sign${abs / 3600}:%02d".format((abs % 3600) / 60)
        RowMeta(zid, city, region, offset)
    }

    private val tickBuffer = ArrayList<TimeTick>(rows.size)

    private val mainHandler = Handler(Looper.getMainLooper())
    private var isTicking = false

    override fun getItemViewType(position: Int): Int = 2

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SharedViewHolder =
        SharedViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.item_world_clock, parent, false))

    override fun getItemCount(): Int = rows.size

    override fun getItemId(position: Int): Long = rows[position].zoneId.id.hashCode().toLong()

    override fun onBindViewHolder(h: SharedViewHolder, pos: Int) {
        val r   = rows[pos]
        val now = ZonedDateTime.now(r.zoneId)
        
        h.secondaryText?.text = r.city
        h.tertiaryText?.text  = r.region
        h.accentText?.text    = r.offsetStr
        h.primaryText?.text   = now.format(timeFmt)
        h.dateText?.text      = now.format(dateFmt)

        // Hide unused views in SharedViewHolder
        h.toggle?.visibility = android.view.View.GONE
        h.actionBtn?.visibility = android.view.View.GONE
        h.iconBtn?.visibility = android.view.View.GONE

        val t = tokens ?: return
        ThemeApplier.applyCard(h.foregroundCard, t, t.isCustom)
        ThemeApplier.applyTextPrimary(h.primaryText, t)
        ThemeApplier.applyTextPrimary(h.secondaryText, t)
        ThemeApplier.applyTextSecondary(h.tertiaryText, t)
        ThemeApplier.applyTextSecondary(h.accentText, t)
        h.dateText?.let { ThemeApplier.applyTextSecondary(it, t) }
    }

    override fun onBindViewHolder(h: SharedViewHolder, pos: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) { onBindViewHolder(h, pos); return }
        val tick = payloads.filterIsInstance<TimeTick>().lastOrNull()
        if (tick != null) {
            h.primaryText?.text = tick.time
            h.dateText?.text    = tick.date
        } else {
            onBindViewHolder(h, pos)
        }
    }

    fun tick() {
        if (!hasObservers() || isTicking) return
        isTicking = true
        AppExecutors.get().diskIO {
            try {
                val now = Instant.now()
                val fmt = timeFmt

                tickBuffer.clear()
                for (i in rows.indices) {
                    val zdt = now.atZone(rows[i].zoneId)
                    tickBuffer.add(TimeTick(zdt.format(fmt), zdt.format(dateFmt)))
                }

                val snapshot = tickBuffer.toList()

                mainHandler.post {
                    if (snapshot.size == rows.size) {
                        for (i in snapshot.indices) {
                            notifyItemChanged(i, snapshot[i])
                        }
                    }
                    isTicking = false
                }
            } catch (e: Exception) {
                isTicking = false
            }
        }
    }

    fun shutdown() {
        mainHandler.removeCallbacksAndMessages(null)
        tickBuffer.clear()
    }
}
