package dev.kevin.batteryline

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

/** Tile para encender y apagar la línea; mantenerlo pulsado abre los ajustes. */
class LineTileService : TileService() {

    override fun onTileAdded() = update()

    override fun onStartListening() = update()

    override fun onClick() {
        if (!Settings.canDrawOverlays(this)) {
            // Sin permiso no hay nada que encender: se abre la app para concederlo.
            val intent = Intent(this, SettingsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (isLocked) unlockAndRun { open(intent) } else open(intent)
            return
        }
        val prefs = LinePrefs(this)
        prefs.enabled = !prefs.enabled
        LineService.sync(this)
        update()
    }

    private fun update() {
        val tile = qsTile ?: return
        val on = LinePrefs(this).enabled && Settings.canDrawOverlays(this)
        tile.state = if (on) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.subtitle = getString(if (on) R.string.tile_on else R.string.tile_off)
        tile.updateTile()
    }

    @SuppressLint("StartActivityAndCollapseDeprecated")
    private fun open(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE))
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(intent)
        }
    }

    companion object {
        /** Pide a SystemUI que vuelva a llamar a onStartListening() para actualizar el tile. */
        fun refresh(context: Context) {
            TileService.requestListeningState(context, ComponentName(context, LineTileService::class.java))
        }
    }
}
