package com.timely.msminutes.service

import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.timely.msminutes.data.Prefs
import com.timely.msminutes.widget.WidgetNotifier

class StopwatchTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        updateTile()
    }

    override fun onClick() {
        val prefs = Prefs(this)
        if (prefs.isStopwatchRunning) {
            prefs.lastStopwatchElapsed += (System.currentTimeMillis() - prefs.stopwatchStartBase)
            prefs.isStopwatchRunning = false
            stopService(Intent(this, StopwatchService::class.java))
        } else {
            prefs.stopwatchStartBase = System.currentTimeMillis()
            prefs.isStopwatchRunning = true
            val intent = Intent(this, StopwatchService::class.java).apply {
                action = StopwatchService.ACTION_START
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        }
        WidgetNotifier.notifyUpdate(this)
        updateTile()
    }

    private fun updateTile() {
        val qsTile = qsTile ?: return
        val prefs = Prefs(this)
        val isRunning = prefs.isStopwatchRunning
        
        qsTile.state = if (isRunning) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        qsTile.label = if (isRunning) "Stopwatch (Running)" else "Start Stopwatch"
        qsTile.updateTile()
    }
}
