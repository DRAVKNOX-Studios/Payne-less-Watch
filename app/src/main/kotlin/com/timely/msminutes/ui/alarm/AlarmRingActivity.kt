package com.timely.msminutes.ui.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.FrameLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.timely.msminutes.data.AlarmRepository
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.service.AlarmRingService
import com.timely.msminutes.ui.canvas.AlarmRingRenderer
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.util.AlarmScheduler
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.util.TimeFormatUtil

class AlarmRingActivity : AppCompatActivity(), ThemeListener {
    private var alarmId: Long = 0
    private var prefs: Prefs? = null
    private lateinit var hostView: CanvasHostView
    private var ringRenderer: AlarmRingRenderer? = null

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
        enableEdgeToEdge()
        applyLockScreenFlags()
        super.onCreate(savedInstanceState)
        
        prefs = Prefs(this)
        alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1)

        val root = FrameLayout(this)
        setContentView(root)
        
        hostView = CanvasHostView(this)
        hostView.drawBackground = false
        root.addView(hostView)

        val alarm = AlarmRepository(this).getById(alarmId)
        if (alarm != null) {
            val timeStr = TimeFormatUtil.formatClock(
                alarm.hour, alarm.minute, prefs?.is24Hour() == true
            )
            ringRenderer = AlarmRingRenderer(
                this, timeStr, alarm.label ?: "Alarm", alarm.note,
                onDismiss = { dismiss() },
                onSnooze = { snooze() }
            )
            hostView.addRenderer(ringRenderer!!)
        }

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            ringRenderer?.onLayout(
                systemBars.left.toFloat(),
                systemBars.top.toFloat(),
                (root.width - systemBars.right).toFloat(),
                (root.height - systemBars.bottom).toFloat()
            )
            hostView.invalidate()
            insets
        }

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, _, _, _, _ ->
            ViewCompat.requestApplyInsets(root)
        }

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
        hostView.invalidate()
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
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager?)
                ?.requestDismissKeyguard(this, null)
        }
    }
}
