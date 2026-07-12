package com.timely.msminutes.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class WidgetTickReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.getAction()
        if (Intent.ACTION_TIME_TICK == action
            || Intent.ACTION_TIME_CHANGED == action
            || Intent.ACTION_TIMEZONE_CHANGED == action
        ) {
            notifyUpdate(context)
        }
    }
}
