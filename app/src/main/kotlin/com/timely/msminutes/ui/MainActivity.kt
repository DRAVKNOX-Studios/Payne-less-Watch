package com.timely.msminutes.ui

import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.AlarmClock
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.timely.msminutes.R
import com.timely.msminutes.service.StopwatchService
import com.timely.msminutes.ui.alarm.AlarmEditActivity
import com.timely.msminutes.ui.settings.SettingsActivity
import com.timely.msminutes.ui.view.UndoBarController
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class MainActivity : AppCompatActivity(), ThemeListener {
    private var tabAlarm: TextView? = null
    private var tabTimer: TextView? = null
    private var tabStopwatch: TextView? = null
    private var tabWorldClock: TextView? = null
    private var toolbarTitle: TextView? = null
    private var btnSettings: ImageView? = null
    private var fragmentContainer: View? = null

    private var sharedFab: TextView? = null
    private var sharedUndoBar: View? = null
    private var sharedUndoMessage: TextView? = null
    private var sharedUndoButton: TextView? = null
    private var sharedUndoController: UndoBarController? = null

    private val sharedViewPool = RecyclerView.RecycledViewPool().apply {
        setMaxRecycledViews(1, 12)
        setMaxRecycledViews(2, 12)
        setMaxRecycledViews(3, 12)
    }

    private var currentPosition: Int = 0
    private var keepUiWarm: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val toolbar = findViewById<Toolbar?>(R.id.toolbar)
        setSupportActionBar(toolbar)

        tabAlarm      = findViewById(R.id.tab_alarm)
        tabTimer      = findViewById(R.id.tab_timer)
        tabStopwatch  = findViewById(R.id.tab_stopwatch)
        tabWorldClock = findViewById(R.id.tab_world_clock)
        btnSettings   = findViewById(R.id.btn_settings)
        toolbarTitle  = findViewById(R.id.toolbar_title)
        fragmentContainer = findViewById(R.id.fragment_container)

        sharedFab         = findViewById(R.id.shared_fab)
        sharedUndoBar     = findViewById(R.id.shared_undo_bar)
        sharedUndoMessage = findViewById(R.id.shared_undo_message)
        sharedUndoButton  = findViewById(R.id.shared_undo_button)

        sharedUndoController = UndoBarController(
            sharedUndoBar!!, sharedUndoMessage!!, sharedUndoButton!!,
            onUndoClicked = { },
            onHide = { }
        )

        tabAlarm!!.setOnClickListener      { showFragment(0) }
        tabTimer!!.setOnClickListener      { showFragment(1) }
        tabStopwatch!!.setOnClickListener  { showFragment(2) }
        tabWorldClock!!.setOnClickListener { showFragment(3) }
        btnSettings!!.setOnClickListener   { startActivity(Intent(this, SettingsActivity::class.java)) }

        MainActivityPermissionHelper.requestNotificationPermission(this)
        MainActivityPermissionHelper.requestFullScreenIntentPermission(this)

        if (savedInstanceState == null) {
            handleIntent(intent)
            showFragment(currentPosition)
        }
    }

    private fun showFragment(position: Int) {
        val fragment: Fragment = when (position) {
            0 -> com.timely.msminutes.ui.alarm.AlarmFragment()
            1 -> com.timely.msminutes.ui.timer.TimerFragment()
            2 -> com.timely.msminutes.ui.stopwatch.StopwatchFragment()
            3 -> com.timely.msminutes.ui.worldclock.WorldClockFragment()
            else -> com.timely.msminutes.ui.alarm.AlarmFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        currentPosition = position
        updateTabSelection(position)
        updateSharedViewsVisibility(position)
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
    }

    override fun onResume() {
        super.onResume()
        keepUiWarm = false
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
        releaseUiGraphics()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        if (level >= TRIM_MEMORY_UI_HIDDEN) {
            releaseUiGraphics()
            com.timely.msminutes.widget.GooglyEyesController.clearCache()
            sharedViewPool.clear()
        } else if (level >= TRIM_MEMORY_BACKGROUND || level >= TRIM_MEMORY_RUNNING_LOW) {
            sharedViewPool.clear()
        }
    }

    private fun releaseUiGraphics() {
        if (keepUiWarm) return
        sharedFab?.background = null
        sharedUndoBar?.background = null
        findViewById<View>(R.id.main_root)?.background = null
        findViewById<View>(R.id.toolbar)?.background = null
        sharedViewPool.clear()
        com.timely.msminutes.util.SharedDrawablePool.invalidate()
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null) return
        ThemeApplier.applyWindow(window, t)
        ThemeApplier.applyBackground(findViewById(R.id.main_root), t)
        ThemeApplier.applyTitleBar(findViewById(R.id.toolbar), t)
        toolbarTitle?.setTextColor(t.textPrimary)
        btnSettings?.let { ThemeApplier.applyToolbarIcon(it, t) }
        ThemeApplier.applyTabBar(t, tabAlarm!!, tabTimer!!, tabStopwatch!!, tabWorldClock!!)
        sharedFab?.let { ThemeApplier.applyFabDrawable(it, t) }
        sharedUndoController?.applyTheme(t)
        updateTabSelection(currentPosition)
    }

    private fun updateSharedViewsVisibility(position: Int) {
        if (position != 0 && position != 1) {
            sharedFab?.visibility = View.GONE
            sharedUndoController?.hide()
        }
    }

    fun getSharedFab(): TextView? = sharedFab
    fun getSharedUndoController(): UndoBarController? = sharedUndoController
    fun getSharedViewPool(): RecyclerView.RecycledViewPool = sharedViewPool

    private fun updateTabSelection(position: Int) {
        tabAlarm!!.isSelected      = position == 0
        tabTimer!!.isSelected      = position == 1
        tabStopwatch!!.isSelected  = position == 2
        tabWorldClock!!.isSelected = position == 3
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == null) return
        when (intent.action) {
            AlarmClock.ACTION_SET_ALARM -> {
                showFragment(0)
                startActivity(Intent(this, AlarmEditActivity::class.java))
            }
            AlarmClock.ACTION_SHOW_ALARMS -> showFragment(0)
            AlarmClock.ACTION_SET_TIMER,
            AlarmClock.ACTION_SHOW_TIMERS,
            ACTION_SHOW_TIMER -> showFragment(1)
            ACTION_START_STOPWATCH -> {
                showFragment(2)
                val p = ThemeStore.get().prefs()
                if (p != null && !p.isStopwatchRunning) {
                    p.stopwatchStartBase  = System.currentTimeMillis()
                    p.isStopwatchRunning  = true
                    startForegroundService(
                        Intent(this, StopwatchService::class.java)
                            .setAction(StopwatchService.ACTION_START)
                    )
                    notifyUpdate(this)
                }
            }
            ACTION_SHOW_STOPWATCH -> showFragment(2)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        MainActivityPermissionHelper.handlePermissionResult(this, requestCode, grantResults)
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentContainer = null
        tabAlarm = null
        tabTimer = null
        tabStopwatch = null
        tabWorldClock = null
        toolbarTitle = null
        btnSettings = null
        sharedFab = null
        sharedUndoBar = null
        sharedUndoMessage = null
        sharedUndoButton = null
        sharedUndoController?.shutdown()
        sharedUndoController = null
    }

    companion object {
        const val ACTION_SHOW_TIMER: String      = "com.timely.msminutes.SHOW_TIMER"
        const val ACTION_SHOW_STOPWATCH: String  = "com.timely.msminutes.SHOW_STOPWATCH"
        const val ACTION_START_STOPWATCH: String = "com.timely.msminutes.START_STOPWATCH"
    }
}