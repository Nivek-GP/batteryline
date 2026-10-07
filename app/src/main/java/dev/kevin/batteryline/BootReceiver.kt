package dev.kevin.batteryline

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Vuelve a mostrar la línea al encender el dispositivo y tras actualizar la app. */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> if (LinePrefs(context).startOnBoot) LineService.sync(context)
            Intent.ACTION_MY_PACKAGE_REPLACED -> LineService.sync(context)
        }
    }
}
