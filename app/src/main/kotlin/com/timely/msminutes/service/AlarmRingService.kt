package com.timely.msminutes.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.Alarm
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.ui.alarm.AlarmRingActivity
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.AppExecutors
import com.timely.msminutes.util.NotificationChannels
import com.timely.msminutes.util.RingPlayer
import com.timely.msminutes.util.TimeFormatUtil
import com.timely.msminutes.util.WakeLockHolder
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class AlarmRingService : Service() {
    private var ringPlayer: RingPlayer? = null
    private var cpuWakeLock: WakeLock? = null
    private var currentAlarmId: Long = -1
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismissRunnable: Runnable? = null
    private var isRinging = false

    private lateinit var repo: AlarmRepository

    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_SCREEN_OFF || !isRinging) return
            when (Prefs(this@AlarmRingService).powerButtonAction) {
                Prefs.POWER_ACTION_SNOOZE  -> handleSnooze()
                Prefs.POWER_ACTION_DISMISS -> handleDismiss()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        ringPlayer = RingPlayer()
        repo = AlarmRepository(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) { stopSelf(); return START_NOT_STICKY }

        when (intent.action) {
            ACTION_DISMISS -> { handleDismiss(); return START_NOT_STICKY }
            ACTION_SNOOZE  -> { handleSnooze();  return START_NOT_STICKY }
        }

        currentAlarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        acquireCpuWakeLock()

        val fullscreenPi = AlarmNotificationHelper.buildFullscreenPendingIntent(this, currentAlarmId)
        val placeholder  = AlarmNotificationHelper.buildPlaceholderNotification(this, fullscreenPi)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIF_ID, placeholder, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIF_ID, placeholder)
        }

        WakeLockHolder.release()

        try {
            startActivity(
                Intent(this, AlarmRingActivity::class.java).apply {
                    putExtra(AlarmScheduler.EXTRA_ALARM_ID, currentAlarmId)
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK
                            or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    )
                }
            )
        } catch (_: Exception) {
        }

        AppExecutors.get().diskIO {
            val alarm = repo.getById(currentAlarmId)
            AppExecutors.get().mainThread {
                if (alarm == null) stopSelf() else onAlarmLoaded(alarm)
            }
        }
        return START_STICKY
    }

    private fun onAlarmLoaded(alarm: Alarm) {
        val prefs = Prefs(this)
        (getSystemService(NOTIFICATION_SERVICE) as NotificationManager?)
            ?.notify(NOTIF_ID, AlarmNotificationHelper.buildNotification(this, alarm, prefs))

        ringPlayer!!.start(
            this, alarm.soundUri, alarm.isVibrate,
            alarm.isGradualVolume, prefs.alarmVolumeRampSeconds
        )

        isRinging = true
        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenOffReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(screenOffReceiver, filter)
        }

        scheduleAutoDismiss(alarm)

        AppExecutors.get().diskIO {
            if (!alarm.isRepeating) { alarm.isEnabled = false; repo.update(alarm) }
            else AlarmScheduler.schedule(this, alarm)
        }
        notifyUpdate(this)
    }

    private fun scheduleAutoDismiss(alarm: Alarm) {
        autoDismissRunnable = Runnable { postMissedNotification(alarm); stopSelf() }
        handler.postDelayed(autoDismissRunnable!!, AUTO_DISMISS_MS)
    }

    private fun finishActivity() {
        sendBroadcast(Intent(ACTION_FINISH_UI).setPackage(packageName))
    }

    private fun handleDismiss() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        finishActivity()
        stopSelf()
    }

    private fun handleSnooze() {
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        finishActivity()
        AppExecutors.get().diskIO {
            val alarm = repo.getById(currentAlarmId)
            val minutes = if (alarm != null && alarm.snoozeMinutes > 0)
                alarm.snoozeMinutes else Prefs(this).defaultSnoozeMinutes
            AppExecutors.get().mainThread {
                AlarmScheduler.scheduleSnooze(this, currentAlarmId, minutes)
            }
        }
        stopSelf()
    }

    private fun unregisterScreenOffReceiver() {
        if (!isRinging) return
        isRinging = false
        try { unregisterReceiver(screenOffReceiver) } catch (_: IllegalArgumentException) {}
    }

    private fun postMissedNotification(alarm: Alarm) {
        val n = AlarmNotificationHelper.buildMissedNotification(this, alarm)
        try { NotificationManagerCompat.from(this).notify((20000 + alarm.id).toInt(), n) }
        catch (_: SecurityException) {}
    }

    private fun acquireCpuWakeLock() {
        val pm = getSystemService(POWER_SERVICE) as PowerManager?
        if (pm != null) {
            cpuWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "timely:alarm_cpu")
            cpuWakeLock!!.acquire(AUTO_DISMISS_MS)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterScreenOffReceiver()
        ringPlayer!!.stop()
        if (cpuWakeLock?.isHeld == true) cpuWakeLock!!.release()
        autoDismissRunnable?.let { handler.removeCallbacks(it) }
        WakeLockHolder.release()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_DISMISS:   String = "action_dismiss"
        const val ACTION_SNOOZE:    String = "action_snooze"
        const val ACTION_FINISH_UI: String = "com.timely.msminutes.action.FINISH_ALARM_UI"

        private const val NOTIF_ID      = 9001
        private val AUTO_DISMISS_MS     = 15 * 60_000L
    }
}
