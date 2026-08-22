package com.timely.msminutes.ui

import android.content.Intent
import android.os.Bundle
import android.provider.AlarmClock
import android.widget.FrameLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.timely.msminutes.service.StopwatchService
import com.timely.msminutes.ui.alarm.AlarmEditActivity
import com.timely.msminutes.ui.canvas.CanvasHostView
import com.timely.msminutes.ui.canvas.TabBarRenderer
import com.timely.msminutes.ui.canvas.ToolbarRenderer
import com.timely.msminutes.ui.canvas.UndoBarRenderer
import com.timely.msminutes.ui.settings.SettingsActivity
import com.timely.msminutes.util.RefreshRateOptimizer
import com.timely.msminutes.util.ThemeApplier
import com.timely.msminutes.util.ThemeStore
import com.timely.msminutes.util.ThemeStore.ThemeListener
import com.timely.msminutes.util.ThemeTokens
import com.timely.msminutes.widget.WidgetNotifier.notifyUpdate

class MainActivity : AppCompatActivity(), ThemeListener {
    private var currentPosition: Int = 0
    private var keepUiWarm: Boolean = false

    private lateinit var root: FrameLayout
    private lateinit var fragmentContainerView: FrameLayout
    private lateinit var hostView: CanvasHostView
    private lateinit var backgroundHostView: CanvasHostView
    
    private lateinit var toolbarRenderer: ToolbarRenderer
    private lateinit var tabBarRenderer: TabBarRenderer
    private lateinit var undoRenderer: UndoBarRenderer
    private lateinit var backgroundRenderer: com.timely.msminutes.ui.canvas.ProceduralBackgroundRenderer

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        RefreshRateOptimizer.optimize(window)
        
        root = FrameLayout(this)
        ThemeStore.get().current()?.let { root.setBackgroundColor(it.background) }
        setContentView(root)

        backgroundHostView = CanvasHostView(this)
        backgroundHostView.drawBackground = false
        root.addView(backgroundHostView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        fragmentContainerView = FrameLayout(this).apply {
            id = android.view.View.generateViewId()
            isClickable = true
            isFocusable = true
        }
        root.addView(fragmentContainerView)

        hostView = CanvasHostView(this)
        hostView.drawBackground = false
        root.addView(hostView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))

        toolbarRenderer = ToolbarRenderer(this, "Payne-less: Watch") {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        backgroundRenderer = com.timely.msminutes.ui.canvas.ProceduralBackgroundRenderer(this)

        tabBarRenderer = TabBarRenderer(this) { position ->
            showFragment(position)
        }
        tabBarRenderer.onAddClick = {
            if (currentPosition == 0) {
                startActivity(Intent(this, AlarmEditActivity::class.java))
            } else if (currentPosition == 1) {
                val fragment = supportFragmentManager.findFragmentById(fragmentContainerView.id) as? com.timely.msminutes.ui.timer.TimerFragment
                fragment?.showCreateDialog()
            } else if (currentPosition == 3) {
                startActivity(Intent(this, com.timely.msminutes.ui.worldclock.TimeZoneSearchActivity::class.java))
            }
        }

        undoRenderer = UndoBarRenderer(this) { }

        backgroundHostView.addRenderer(backgroundRenderer)
        hostView.addRenderer(toolbarRenderer)
        hostView.addRenderer(tabBarRenderer)
        hostView.addRenderer(undoRenderer)
        
        MainActivityPermissionHelper.requestNotificationPermission(this)
        MainActivityPermissionHelper.requestFullScreenIntentPermission(this)
        MainActivityPermissionHelper.requestBatteryOptimizationExemption(this)
        MainActivityPermissionHelper.requestExactAlarmPermission(this)

        if (savedInstanceState == null) {
            handleIntent(intent)
            showFragment(currentPosition)
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        
        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val w = root.width.toFloat()
            val h = root.height.toFloat()
            
            if (w > 0 && h > 0) {
                val d = resources.displayMetrics.density
                val toolbarH = 56f * d
                val tabBarH = 64f * d
                
                // Toolbar at the top, offset by status bar
                toolbarRenderer.onLayout(0f, systemBars.top.toFloat(), w, systemBars.top.toFloat() + toolbarH)
                
                // TabBar as a floating pill at the bottom
                val maxTabBarWidth = 600f * d
                // Reduce side margins on smaller screens to prevent cramping
                val sideMargin = if (w < 600f * d) 16f * d else 48f * d
                val targetTabBarWidth = Math.min(w - sideMargin * 2f, maxTabBarWidth)
                val tabBarLeft = (w - targetTabBarWidth) / 2f
                val tabBarRight = tabBarLeft + targetTabBarWidth
                val tabBarMarginBottom = 16f * d
                val tabBarBottom = h - systemBars.bottom.toFloat() - tabBarMarginBottom
                val tabBarTop = tabBarBottom - tabBarH
                
                tabBarRenderer.onLayout(tabBarLeft, tabBarTop, tabBarRight, tabBarBottom)
                
                val undoH = 56f * d
                val undoMargin = 16f * d
                val undoBottom = tabBarTop - undoMargin
                undoRenderer.onLayout(undoMargin, undoBottom - undoH, w - undoMargin, undoBottom)

                backgroundRenderer.onLayout(0f, 0f, w, h)
                backgroundHostView.invalidate()
                
                // Adjust fragment container margin to fit between toolbar and tabbar
                val params = fragmentContainerView.layoutParams as FrameLayout.LayoutParams
                params.topMargin = (systemBars.top.toFloat() + toolbarH).toInt()
                params.bottomMargin = 0 // Full height to avoid black strip behind floating pill
                fragmentContainerView.layoutParams = params
                
                // Remove padding that was shrinking the child view, let ListView handle its own bottom padding
                fragmentContainerView.setPadding(0, 0, 0, 0)
                
                hostView.invalidate()
            }
            insets
        }

        hostView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) return@addOnLayoutChangeListener
            ViewCompat.requestApplyInsets(root)
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
            .replace(fragmentContainerView.id, fragment)
            .commit()

        currentPosition = position
        tabBarRenderer.setSelectedIndex(position)
        updateSharedViewsVisibility(position)
        hostView.invalidate()
    }

    private fun updateSharedViewsVisibility(position: Int) {
        tabBarRenderer.showAddButton = (position == 0 || position == 1 || position == 3)
        if (position != 0 && position != 1 && position != 3) {
            undoRenderer.isVisible = false
        }
    }

    override fun onStart() {
        super.onStart()
        ThemeStore.get().subscribe(this)
    }

    override fun onStop() {
        super.onStop()
        ThemeStore.get().unsubscribe(this)
    }

    override fun onThemeChanged(t: ThemeTokens?) {
        if (t == null) return
        ThemeApplier.applyWindow(this, t)
        root.setBackgroundColor(t.background)
        backgroundHostView.invalidate()
        hostView.invalidate()
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

    fun showUndo(message: String, onUndo: () -> Unit) {
        undoRenderer.message = message
        undoRenderer.isVisible = true
        undoRenderer.onUndoClick = onUndo
        hostView.invalidate()
        
        hostView.removeCallbacks(hideUndoRunnable)
        hostView.postDelayed(hideUndoRunnable, 5000L)
    }

    private val hideUndoRunnable = Runnable {
        undoRenderer.isVisible = false
        hostView.invalidate()
    }
    
    fun hideUndo() {
        hostView.removeCallbacks(hideUndoRunnable)
        undoRenderer.isVisible = false
        hostView.invalidate()
    }

    companion object {
        const val ACTION_SHOW_TIMER: String      = "com.timely.msminutes.SHOW_TIMER"
        const val ACTION_SHOW_STOPWATCH: String  = "com.timely.msminutes.SHOW_STOPWATCH"
        const val ACTION_START_STOPWATCH: String = "com.timely.msminutes.START_STOPWATCH"
    }
}
