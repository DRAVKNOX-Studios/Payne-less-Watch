package com.timely.msminutes.widget

import android.content.Context
import android.content.Intent

object WidgetNotifier {
    const val ACTION_UPDATE_WIDGET: String = "com.timely.msminutes.UPDATE_WIDGET"

    @JvmStatic
    fun notifyUpdate(context: Context) {
        val intent = Intent(ACTION_UPDATE_WIDGET)
        intent.setPackage(context.getPackageName())
        context.sendBroadcast(intent)
    }
}
