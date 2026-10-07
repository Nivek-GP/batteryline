package dev.kevin.batteryline

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/** Extremo de la línea que queda fijo cuando baja la batería. */
enum class Anchor { LEFT, CENTER, RIGHT }

/** Ajustes de la línea; el servicio los escucha y aplica cada cambio en vivo. */
class LinePrefs(context: Context) {

    val prefs: SharedPreferences = context.getSharedPreferences("line", Context.MODE_PRIVATE)

    var enabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) = prefs.edit { putBoolean(KEY_ENABLED, value) }

    /** Grosor en px; el overlay lo limita a la altura de la status bar. */
    var thickness: Int
        get() = prefs.getInt(KEY_THICKNESS, 1)
        set(value) = prefs.edit { putInt(KEY_THICKNESS, value) }

    /** Distancia en px desde el borde superior de la pantalla. */
    var offset: Int
        get() = prefs.getInt(KEY_OFFSET, 0)
        set(value) = prefs.edit { putInt(KEY_OFFSET, value) }

    /** Opacidad en %. */
    var opacity: Int
        get() = prefs.getInt(KEY_OPACITY, 100)
        set(value) = prefs.edit { putInt(KEY_OPACITY, value) }

    /** Nivel de batería que se dibuja como línea completa (p. ej. 80 con el límite de carga activado). */
    var fullAt: Int
        get() = prefs.getInt(KEY_FULL_AT, 100)
        set(value) = prefs.edit { putInt(KEY_FULL_AT, value) }

    var anchor: Anchor
        get() = prefs.getString(KEY_ANCHOR, null)?.let { runCatching { Anchor.valueOf(it) }.getOrNull() } ?: Anchor.RIGHT
        set(value) = prefs.edit { putString(KEY_ANCHOR, value.name) }

    var lowEnabled: Boolean
        get() = prefs.getBoolean(KEY_LOW_ENABLED, true)
        set(value) = prefs.edit { putBoolean(KEY_LOW_ENABLED, value) }

    /** Por debajo de este nivel la línea se pinta de rojo. */
    var lowThreshold: Int
        get() = prefs.getInt(KEY_LOW_THRESHOLD, 15)
        set(value) = prefs.edit { putInt(KEY_LOW_THRESHOLD, value) }

    var startOnBoot: Boolean
        get() = prefs.getBoolean(KEY_START_ON_BOOT, true)
        set(value) = prefs.edit { putBoolean(KEY_START_ON_BOOT, value) }

    /** Margen en px que se deja libre en el borde izquierdo; cada orientación tiene el suyo. */
    fun marginLeft(landscape: Boolean): Int = prefs.getInt(marginKey(landscape, "left"), 0)

    fun marginRight(landscape: Boolean): Int = prefs.getInt(marginKey(landscape, "right"), 0)

    fun setMargins(landscape: Boolean, left: Int, right: Int) = prefs.edit {
        putInt(marginKey(landscape, "left"), left)
        putInt(marginKey(landscape, "right"), right)
    }

    fun setMarginLeft(landscape: Boolean, value: Int) = prefs.edit { putInt(marginKey(landscape, "left"), value) }

    fun setMarginRight(landscape: Boolean, value: Int) = prefs.edit { putInt(marginKey(landscape, "right"), value) }

    private fun marginKey(landscape: Boolean, side: String) =
        "margin_${if (landscape) "landscape" else "portrait"}_$side"

    companion object {
        const val KEY_ENABLED = "enabled"
        const val KEY_THICKNESS = "thickness"
        const val KEY_OFFSET = "offset"
        const val KEY_OPACITY = "opacity"
        const val KEY_FULL_AT = "full_at"
        const val KEY_ANCHOR = "anchor"
        const val KEY_LOW_ENABLED = "low_enabled"
        const val KEY_LOW_THRESHOLD = "low_threshold"
        const val KEY_START_ON_BOOT = "start_on_boot"

        /** Claves que cambian el tamaño o la posición de la ventana (el resto solo redibuja). */
        fun affectsLayout(key: String?) =
            key == null || key == KEY_THICKNESS || key == KEY_OFFSET || key.startsWith("margin_")
    }
}

/**
 * Nivel simulado desde la pantalla de ajustes para ver la línea a cualquier nivel
 * mientras se ajusta. Vive solo en memoria: al salir de los ajustes vuelve el nivel real.
 */
object Preview {

    var level: Int? = null
        set(value) {
            field = value
            listener?.invoke()
        }

    var listener: (() -> Unit)? = null
}
