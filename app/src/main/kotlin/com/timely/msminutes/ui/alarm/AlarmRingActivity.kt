package com.timely.msminutes.ui.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.timely.msminutes.R
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.service.AlarmRingService
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil
import com.timely.msminutes.view.SwipeDismissView

class AlarmRingActivity : AppCompatActivity(), ThemeListener {
    private var alarmId: Long = 0
    private var prefs: Prefs? = null
    private var time: TextView? = null
    private var label: TextView? = null
    private var note: TextView? = null
    private var hint: TextView? = null
    private var swipeView: SwipeDismissView? = null

    private var isHandlingAction = false

    private val finishReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AlarmRingService.ACTION_FINISH_UI) {
                isHandlingAction = true
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // MUST be called before super.onCreate() so the window flags are in
        // place before the framework creates the decor view. Calling them
        // after setContentView results in a brief dark flash on Android 8-10
        // where the window is composed before our flags take effect.
        applyLockScreenFlags()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_alarm_ring)

        prefs     = Prefs(this)
        alarmId   = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)
        time      = findViewById(R.id.text_ring_time)
        label     = findViewById(R.id.text_ring_label)
        note      = findViewById(R.id.text_ring_note)
        hint      = findViewById(R.id.text_ring_hint)
        swipeView = findViewById(R.id.swipe_view)

        val alarm = AlarmRepository(this).getById(alarmId)
        if (alarm != null) {
            time?.text = TimeFormatUtil.formatClock(
                alarm.hour, alarm.minute, prefs?.is24Hour() == true
            )
            val lbl = alarm.label
            label?.text = if (lbl.isNullOrEmpty()) "Alarm" else lbl
            val n = alarm.note
            if (!n.isNullOrEmpty()) {
                note?.text = n
                note?.visibility = View.VISIBLE
            }
        }

        swipeView!!.setListener(object : SwipeDismissView.Listener {
            override fun onSwipeUpDismiss()  { dismiss() }
            override fun onSwipeDownSnooze() { snooze()  }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { /* block back during alarm */ }
        })

        registerReceiver(
            finishReceiver,
            IntentFilter(AlarmRingService.ACTION_FINISH_UI),
            RECEIVER_NOT_EXPORTED
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(finishReceiver) } catch (_: IllegalArgumentException) {}
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
        isHandlingAction = false
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null) return
        swipeView?.setBackgroundColor(t.accent)
        val fontColor = t.textPrimary
        time?.setTextColor(fontColor)
        label?.setTextColor(fontColor);  label?.alpha = 0.7f
        note?.setTextColor(fontColor)
        hint?.setTextColor(fontColor);   hint?.alpha  = 0.6f
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) return true
        return super.onKeyDown(keyCode, event)
    }

    private fun dismiss() {
        if (isHandlingAction) return
        isHandlingAction = true
        startForegroundService(
            Intent(this, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_DISMISS)
        )
        finish()
    }

    private fun snooze() {
        if (isHandlingAction) return
        isHandlingAction = true
        startForegroundService(
            Intent(this, AlarmRingService::class.java).setAction(AlarmRingService.ACTION_SNOOZE)
        )
        finish()
    }

    @Suppress("DEPRECATION")
    private fun applyLockScreenFlags() {
        // Apply legacy window flags unconditionally — these work on all API
        // levels and some OEM builds (Samsung, Xiaomi) only honour the
        // window flags, not the manifest attributes or the new API calls.
        window.addFlags(
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        // Also call the non-deprecated API on O_MR1+ so the framework's
        // own lock-screen compositor is aware.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            // requestDismissKeyguard is async but that's fine — window flags
            // above already turned the screen on synchronously.
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager?)
                ?.requestDismissKeyguard(this, null)
        }
    }
}
