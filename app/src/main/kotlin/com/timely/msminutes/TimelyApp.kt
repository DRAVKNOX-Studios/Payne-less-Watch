package com.timely.msminutes

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.util.NotificationChannels
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeUtil

class TimelyApp : Application() {
    override fun onCreate() {
        super.onCreate()

        NotificationChannels.createAll(this)

        val prefs = Prefs(this)
        ThemeUtil.applyTheme(prefs.theme)
        ThemeStore.get().init(prefs)

        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                ThreadPolicy.Builder().detectAll().penaltyLog().build()
            )
        }
    }
}
