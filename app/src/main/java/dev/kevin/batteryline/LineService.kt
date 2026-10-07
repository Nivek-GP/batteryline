package dev.kevin.batteryline

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.os.BatteryManager
import android.os.IBinder
import android.os.PowerManager
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * Mantiene la línea en pantalla. Es un servicio en primer plano para que el sistema no lo
 * cierre; no hace polling: solo reacciona a los broadcasts de batería y de pantalla.
 */
class LineService : Service() {

    private lateinit var prefs: LinePrefs
    private lateinit var overlay: OverlayController
    private lateinit var power: PowerManager

    /** Último nivel real conocido; -1 hasta recibir el primero. */
    private var level = -1

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_BATTERY_CHANGED -> {
                    // Este broadcast llega también por cambios de voltaje o temperatura: se ignoran.
                    val newLevel = levelOf(intent) ?: return
                    if (newLevel == level) return
                    val first = level < 0
                    level = newLevel
                    if (Preview.level == null) overlay.render(level, animate = !first && power.isInteractive)
                }
                // Con la pantalla apagada no se anima; al encenderla se muestra el nivel actual tal cual.
                Intent.ACTION_SCREEN_ON -> render(animate = false)
            }
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (LinePrefs.affectsLayout(key)) overlay.layout()
        render(animate = true, fast = true)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = LinePrefs(this)
        power = getSystemService(PowerManager::class.java)
        startForeground(NOTIFICATION_ID, notification())

        if (!Settings.canDrawOverlays(this)) {
            prefs.enabled = false
            stopSelf()
            return
        }

        overlay = OverlayController(this, prefs)
        overlay.show()

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        // ACTION_BATTERY_CHANGED es sticky: el registro devuelve el nivel actual al momento.
        ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            ?.let { levelOf(it) }
            ?.let { level = it }
        render(animate = false)

        prefs.prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        Preview.listener = { render(animate = true, fast = true) }
        LineTileService.refresh(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        if (::overlay.isInitialized) {
            Preview.listener = null
            prefs.prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
            unregisterReceiver(receiver)
            overlay.hide()
        }
        LineTileService.refresh(this)
        super.onDestroy()
    }

    private fun render(animate: Boolean, fast: Boolean = false) {
        val shown = Preview.level ?: level
        if (shown >= 0) overlay.render(shown, animate, fast)
    }

    private fun levelOf(intent: Intent): Int? {
        val raw = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
        if (raw < 0 || scale <= 0) return null
        return raw * 100 / scale
    }

    /**
     * Prioridad mínima: sin sonido ni icono en la status bar. La app no pide permiso de
     * notificaciones, así que en Android 13+ ni siquiera aparece en el panel.
     */
    private fun notification(): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, getString(R.string.channel_name), NotificationManager.IMPORTANCE_MIN).apply {
                setShowBadge(false)
            },
        )
        val open = PendingIntent.getActivity(
            this,
            0,
            Intent(this, SettingsActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(getString(R.string.notification_title))
            .setContentIntent(open)
            .setOngoing(true)
            .setShowWhen(false)
            .build()
    }

    companion object {
        private const val CHANNEL_ID = "line"
        private const val NOTIFICATION_ID = 1

        /** Arranca o detiene el servicio según el ajuste y el permiso de overlay. */
        fun sync(context: Context) {
            val intent = Intent(context, LineService::class.java)
            if (LinePrefs(context).enabled && Settings.canDrawOverlays(context)) {
                context.startForegroundService(intent)
            } else {
                context.stopService(intent)
            }
        }
    }
}
