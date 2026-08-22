package com.timely.msminutes.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.AlarmClock
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.timely.msminutes.R
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.ui.alarm.AlarmEditActivity

class ClockWidgetProvider : AppWidgetProvider() {
    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        // Size changed — force a re-render on the next tick.
        RenderCache.invalidate(appWidgetId)
        updateAppWidget(context, appWidgetManager, appWidgetId)
    }

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
        if (appWidgetIds.isNotEmpty()) {
            scheduleNextUpdate(context)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (WidgetNotifier.ACTION_UPDATE_WIDGET == intent.action) {
            val fromService = intent.getBooleanExtra(WidgetNotifier.EXTRA_FROM_SERVICE, false)
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isNotEmpty()) {
                for (appWidgetId in ids) {
                    updateAppWidget(context, manager, appWidgetId)
                }
                // Only schedule next heartbeat if this wasn't from the high-frequency service
                if (!fromService) {
                    scheduleNextUpdate(context)
                }
            }
        }
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduleNextUpdate(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        super.onDeleted(context, appWidgetIds)
        for (id in appWidgetIds) RenderCache.invalidate(id)
    }

    /**
     * Per-widget render cache keyed on widget ID.
     *
     * WidgetState is a data class, so its auto-generated equals() provides
     * structural comparison — no custom cacheKey() method required.
     * A cache hit (same state + same pixel dimensions) returns the existing
     * Bitmap directly, avoiding a WidgetRenderer.render() call and the
     * associated allocation + RemoteViews binder transfer every minute.
     */
    private object RenderCache {
        private data class Entry(
            val state: WidgetState,
            val width: Int,
            val height: Int,
            val bitmap: Bitmap
        )

        private val cache = mutableMapOf<Int, Entry>()

        fun getOrRender(
            widgetId: Int,
            width: Int,
            height: Int,
            drawDensity: Float,
            prefs: Prefs,
            state: WidgetState
        ): Bitmap {
            val cached = cache[widgetId]
            if (cached != null &&
                cached.state == state &&
                cached.width == width &&
                cached.height == height
            ) {
                return cached.bitmap
            }
            val bitmap = WidgetRenderer.render(width, height, drawDensity, prefs, state)
            cache[widgetId] = Entry(state, width, height, bitmap)
            return bitmap
        }

        fun invalidate(widgetId: Int) {
            cache.remove(widgetId)
        }
    }

    companion object {
        private const val TAG = "ClockWidgetProvider"

        fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_clock)
            val prefs = Prefs(context)

            // 1. Compute pixel dimensions; cap to stay under the 1 MB RemoteViews binder limit.
            val options = appWidgetManager.getAppWidgetOptions(appWidgetId)
            val density = context.resources.displayMetrics.density

            val minWidth  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)
            val minHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
            val maxWidth  = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)
            val maxHeight = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)

            val isPortrait = context.resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_PORTRAIT
            
            var widthDp = if (isPortrait) minWidth else maxWidth
            var heightDp = if (isPortrait) maxHeight else minHeight
            
            // Fallbacks if options are missing
            if (widthDp <= 0) widthDp = 250
            if (heightDp <= 0) heightDp = 110

            var width  = (widthDp * density).toInt()
            var height = (heightDp * density).toInt()

            var drawDensity = density
            // 250 000 px * 4 bytes = 1 MB limit (tight)
            // Let's stay slightly safer at 200 000 px
            val maxPixels = 200_000
            if (width * height > maxPixels) {
                val scale = Math.sqrt(maxPixels.toDouble() / (width * height)).toFloat()
                width       = (width  * scale).toInt().coerceAtLeast(1)
                height      = (height * scale).toInt().coerceAtLeast(1)
                drawDensity *= scale
            } else {
                width  = width.coerceAtLeast(1)
                height = height.coerceAtLeast(1)
            }

            // 2. Render — or reuse cached bitmap if state + size are unchanged.
            try {
                val state  = WidgetInfoProvider.getWidgetState(context)
                val bitmap = RenderCache.getOrRender(
                    appWidgetId, width, height, drawDensity, prefs, state
                )
                views.setImageViewBitmap(R.id.widget_background_img, bitmap)
                views.setViewVisibility(R.id.widget_background_img, View.VISIBLE)
            } catch (e: Exception) {
                Log.e(TAG, "Widget render failed", e)
            }

            // 3. Accent tint for legacy add-button background.
            views.setInt(R.id.widget_btn_add_bg, "setColorFilter", prefs.accentColor)

            // 4. PendingIntents for tap targets.
            val immutable = PendingIntent.FLAG_IMMUTABLE

            views.setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), immutable)
            )
            views.setOnClickPendingIntent(
                R.id.alarm_container,
                PendingIntent.getActivity(
                    context, 2,
                    Intent(context, MainActivity::class.java).setAction(AlarmClock.ACTION_SHOW_ALARMS),
                    immutable
                )
            )
            views.setOnClickPendingIntent(
                R.id.timer_container,
                PendingIntent.getActivity(
                    context, 3,
                    Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_SHOW_TIMER),
                    immutable
                )
            )
            views.setOnClickPendingIntent(
                R.id.stopwatch_container,
                PendingIntent.getActivity(
                    context, 4,
                    Intent(context, MainActivity::class.java).setAction(MainActivity.ACTION_SHOW_STOPWATCH),
                    immutable
                )
            )
            views.setOnClickPendingIntent(
                R.id.widget_btn_add,
                PendingIntent.getActivity(context, 1, Intent(context, AlarmEditActivity::class.java), immutable)
            )

            appWidgetManager.updateAppWidget(appWidgetId, views)

            if (GooglyEyesController.isEasterEggMinute(context)) {
                context.startService(Intent(context, GooglyEyesService::class.java))
            }
        }

        internal fun scheduleNextUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, ClockWidgetProvider::class.java))
            if (ids.isEmpty()) return

            val am     = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, ClockWidgetProvider::class.java)
                .setAction(WidgetNotifier.ACTION_UPDATE_WIDGET)
            val now        = System.currentTimeMillis()
            val nextMinute = (now / 60_000 + 1) * 60_000
            val pi = PendingIntent.getBroadcast(
                context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // setAlarmClock is the most reliable way to wake up even in Doze.
            // For a clock widget, this is critical to avoid de-sync.
            val showIntent = PendingIntent.getActivity(
                context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
            )
            am.setAlarmClock(AlarmManager.AlarmClockInfo(nextMinute, showIntent), pi)
        }
    }
}
