package com.timely.msminutes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.AlarmClock
import android.view.View
import android.widget.RemoteViews
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.alarm.AlarmEditActivity

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        // Update all widgets if alarms/timers/settings changed
        if (WidgetNotifier.ACTION_UPDATE_WIDGET == intent.getAction()) {
            val manager = AppWidgetManager.getInstance(context)
            val ids =
                manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            onUpdate(context, manager, ids)
        }
    }

    companion object {
        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.getPackageName(), R.layout.widget_clock)
            val prefs = Prefs(context)

            val accent = prefs.accentColor
            val bgColor = prefs.backgroundColor
            val fontColor = prefs.fontColor
            val is24h = prefs.is24Hour()
            val isTransparent = prefs.isWidgetTransparent

            // Apply theme colors
            if (isTransparent) {
                views.setViewVisibility(R.id.widget_background_img, View.GONE)
                // In transparent mode, if the font color is too dark and we have an accent shadow, 
                // maybe we should ensure the font is readable.
                // But the user said "accent colored outline and shadow for improved readablility"
            } else {
                views.setViewVisibility(R.id.widget_background_img, View.VISIBLE)
                views.setInt(R.id.widget_background_img, "setColorFilter", bgColor)
            }


            // Update Time with Googly Eyes logic
            GooglyEyesController.updateTime(context, views, is24h, fontColor, accent, isTransparent)


            // Update Note
            val note = prefs.widgetNote
            if (!note.isNullOrBlank()) {
                views.setViewVisibility(R.id.widget_note, View.VISIBLE)
                views.setTextViewText(R.id.widget_note, note)
                views.setTextColor(R.id.widget_note, fontColor)
                if (isTransparent) {
                    applyShadow(views, R.id.widget_note, accent)
                }
            } else {
                views.setViewVisibility(R.id.widget_note, View.GONE)
            }

            val dateColor = (fontColor and 0x00FFFFFF) or (0x99 shl 24)
            views.setTextColor(R.id.widget_date, dateColor)
            if (isTransparent) {
                applyShadow(views, R.id.widget_date, accent)
            }

            views.setInt(R.id.widget_btn_add_bg, "setColorFilter", accent)
            views.setTextColor(R.id.widget_next_alarm, accent)
            views.setInt(R.id.widget_alarm_icon, "setColorFilter", accent)

            views.setTextColor(R.id.widget_timer_chrono, accent)
            views.setTextColor(R.id.widget_timer_label, accent)
            views.setInt(R.id.widget_timer_icon, "setColorFilter", accent)

            views.setTextColor(R.id.widget_stopwatch_chrono, accent)
            views.setInt(R.id.widget_stopwatch_icon, "setColorFilter", accent)

            if (isTransparent) {
                applyShadow(views, R.id.widget_next_alarm, accent)
                applyShadow(views, R.id.widget_alarm_label, accent)
                applyShadow(views, R.id.widget_timer_chrono, accent)
                applyShadow(views, R.id.widget_timer_label, accent)
                applyShadow(views, R.id.widget_stopwatch_chrono, accent)
            }

            // Apply time format to date
            val datePattern = if (is24h) "EEE, d MMM" else "EEE, MMM d"
            views.setCharSequence(R.id.widget_date, "setFormat12Hour", datePattern)
            views.setCharSequence(R.id.widget_date, "setFormat24Hour", datePattern)

            // Open main app on click
            val mainIntent = Intent(context, MainActivity::class.java)
            val mainPendingIntent =
                PendingIntent.getActivity(context, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

            // Deep links for status items
            val alarmIntent =
                Intent(context, MainActivity::class.java).setAction(AlarmClock.ACTION_SHOW_ALARMS)
            views.setOnClickPendingIntent(
                R.id.alarm_container,
                PendingIntent.getActivity(context, 2, alarmIntent, PendingIntent.FLAG_IMMUTABLE)
            )

            val timerIntent =
                Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_SHOW_TIMER)
            views.setOnClickPendingIntent(
                R.id.timer_container,
                PendingIntent.getActivity(context, 3, timerIntent, PendingIntent.FLAG_IMMUTABLE)
            )

            val stopwatchIntent = Intent(
                context,
                MainActivity::class.java
            ).setAction(MainActivity.ACTION_SHOW_STOPWATCH)
            views.setOnClickPendingIntent(
                R.id.stopwatch_container,
                PendingIntent.getActivity(context, 4, stopwatchIntent, PendingIntent.FLAG_IMMUTABLE)
            )

            // Open alarm edit activity
            val addIntent = Intent(context, AlarmEditActivity::class.java)
            val addPendingIntent =
                PendingIntent.getActivity(context, 1, addIntent, PendingIntent.FLAG_IMMUTABLE)
            views.setOnClickPendingIntent(R.id.widget_btn_add, addPendingIntent)

            // Update alarm info
            WidgetInfoProvider.updateAlarmInfo(context, views)

            appWidgetManager.updateAppWidget(appWidgetId, views)

            // Schedule next minute update to ensure time doesn't freeze
            scheduleNextUpdate(context)

            // Start animation service if it's the easter egg minute
            if (GooglyEyesController.isEasterEggMinute(context)) {
                val serviceIntent = Intent(context, GooglyEyesService::class.java)
                context.startService(serviceIntent)
            }
        }

        private fun applyShadow(views: RemoteViews?, id: Int, color: Int) {
            if (Build.VERSION.SDK_INT >= 33) {
                try {
                    val m = RemoteViews::class.java.getMethod(
                        "setTextViewShadowLayer",
                        Int::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                        Float::class.javaPrimitiveType,
                        Int::class.javaPrimitiveType
                    )
                    m.invoke(views, id, 12f, 0f, 0f, color)
                } catch (ignored: Exception) {
                }
            }
        }

        private fun scheduleNextUpdate(context: Context) {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClockWidgetProvider::class.java)
            intent.setAction(WidgetNotifier.ACTION_UPDATE_WIDGET)

            val now = System.currentTimeMillis()
            val nextMinute = (now / 60000 + 1) * 60000

            val pi = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextMinute, pi)
        }
    }
}
