package com.timely.msminutes.widget

import android.content.Context
import android.content.Intent

object WidgetNotifier {
    const val ACTION_UPDATE_WIDGET: String = "com.timely.msminutes.UPDATE_WIDGET"
    const val EXTRA_FROM_SERVICE: String = "from_service"

    @JvmStatic
    fun notifyUpdate(context: Context) {
        val intent = Intent(ACTION_UPDATE_WIDGET)
        intent.setPackage(context.packageName)
        context.sendBroadcast(intent)
    }

    @JvmStatic
    fun notifyServiceUpdate(context: Context) {
        val intent = Intent(ACTION_UPDATE_WIDGET)
        intent.setPackage(context.packageName)
        intent.putExtra(EXTRA_FROM_SERVICE, true)
        context.sendBroadcast(intent)
    }
}
