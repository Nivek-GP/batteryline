package dev.kevin.batteryline

import android.view.RoundedCorner
import android.view.WindowInsets
import android.view.WindowManager
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** Medidas de la pantalla en su rotación actual, tal como las ve el overlay. */
class Screen(windowManager: WindowManager) {

    private val metrics = windowManager.currentWindowMetrics
    private val insets: WindowInsets = metrics.windowInsets

    val width: Int = metrics.bounds.width()
    val height: Int = metrics.bounds.height()
    val landscape: Boolean = width > height

    /**
     * Alto de la status bar aunque esté oculta (apps a pantalla completa); es el
     * límite para el grosor y la posición de la línea.
     */
    val statusBarHeight: Int =
        insets.getInsetsIgnoringVisibility(WindowInsets.Type.statusBars()).top.takeIf { it > 0 } ?: FALLBACK_STATUS_BAR

    /** Margen máximo que ofrecen los sliders de bordes. */
    val maxMargin: Int = minOf(width, height) / 4

    /**
     * Márgenes izquierdo y derecho que dejan la línea dentro de la zona visible de las
     * esquinas redondeadas, medidos a la altura del centro de la línea. Null si el
     * sistema no informa del radio de las esquinas.
     */
    fun autoMargins(offset: Int, thickness: Int): Pair<Int, Int>? {
        val left = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)
        val right = insets.getRoundedCorner(RoundedCorner.POSITION_TOP_RIGHT)
        if (left == null && right == null) return null
        val y = offset + thickness / 2f
        return Pair(
            left?.let { margin(it.center.x - chord(it, y)) } ?: 0,
            right?.let { margin(width - (it.center.x + chord(it, y))) } ?: 0,
        )
    }

    /** +1 px para que el antialiasing de la esquina no se coma el extremo de la línea. */
    private fun margin(raw: Float): Int = if (raw <= 0f) 0 else (ceil(raw).toInt() + 1).coerceAtMost(maxMargin)

    /** Media cuerda horizontal del arco de la esquina a la altura [y]. */
    private fun chord(corner: RoundedCorner, y: Float): Float {
        val r = corner.radius.toFloat()
        val dy = corner.center.y - y
        if (dy <= 0f) return r
        return sqrt(max(0f, r * r - dy * dy))
    }

    private companion object {
        const val FALLBACK_STATUS_BAR = 63
    }
}
