package com.timely.msminutes.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.timely.msminutes.R
import com.timely.msminutes.data.TimerItem
import com.timely.msminutes.data.TimerRepository
import com.timely.msminutes.receiver.NotificationActionReceiver
import com.timely.msminutes.ui.MainActivity
import com.timely.msminutes.util.NotificationChannels
import com.timely.msminutes.util.RingPlayer
import com.timely.msminutes.util.TimerScheduler

class TimerService : Service() {
    private var ringPlayer: RingPlayer? = null

    override fun onCreate() {
        super.onCreate()
        ringPlayer = RingPlayer()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        val action = intent.getAction()
        val timerId = intent.getLongExtra(TimerScheduler.EXTRA_TIMER_ID, -1)

        if (ACTION_STOP == action) {
            ringPlayer!!.stop()
            val repo = TimerRepository(this)
            val item = repo.getById(timerId)
            if (item != null) {
                item.state = TimerItem.STATE_FINISHED
                item.remainingMillis = 0
                repo.update(item)
            }
            NotificationManagerCompat.from(this).cancel((30000 + timerId).toInt())
            stopSelf()
            return START_NOT_STICKY
        }

        if (ACTION_FINISHED == action) {
            val repo = TimerRepository(this)
            val target = repo.getById(timerId)
            if (target == null) {
                stopSelf()
                return START_NOT_STICKY
            }
            target.state = TimerItem.STATE_FINISHED
            target.remainingMillis = 0
            repo.update(target)

            startForeground(NOTIF_ID, buildFinishedNotification(target))
            ringPlayer!!.start(this, target.soundUri, target.isVibrate, false, 0)
            return START_STICKY
        }

        stopSelf()
        return START_NOT_STICKY
    }

    private fun buildFinishedNotification(item: TimerItem): Notification {
        val stopIntent = Intent(this, NotificationActionReceiver::class.java)
            .setAction(NotificationActionReceiver.ACTION_TIMER_END)
        stopIntent.putExtra(TimerScheduler.EXTRA_TIMER_ID, item.id)
        val stopPi = PendingIntent.getBroadcast(
            this, (40000 + item.id).toInt(), stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val contentIntent = Intent(this, MainActivity::class.java)
        val contentPi = PendingIntent.getActivity(
            this, (41000 + item.id).toInt(), contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val lbl = item.label
        val title = if (!lbl.isNullOrEmpty()) "Timer: " + lbl else "Timer done"

        return NotificationCompat.Builder(this, NotificationChannels.TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle(title)
            .setContentText("Time is up")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(contentPi)
            .setOngoing(true)
            .addAction(0, "End", stopPi)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        ringPlayer!!.stop()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    companion object {
        const val ACTION_FINISHED: String = "action_timer_finished"
        const val ACTION_STOP: String = "action_timer_stop"

        private const val NOTIF_ID = 9101
    }
}
