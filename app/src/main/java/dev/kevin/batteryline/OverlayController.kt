package dev.kevin.batteryline

import android.content.ComponentCallbacks
import android.content.Context
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Gravity
import android.view.WindowManager

/**
 * Ventana overlay con la línea. Queda por debajo de la ventana de la status bar, que es
 * transparente: la línea se ve en la franja de la barra, los iconos del sistema se dibujan
 * encima y el panel de notificaciones la tapa al bajarlo.
 */
class OverlayController(context: Context, private val prefs: LinePrefs) {

    // Contexto de ventana: recibe los cambios de rotación y da las medidas reales del display.
    private val windowContext: Context = context
        .createDisplayContext(context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY))
        .createWindowContext(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, null)
    private val windowManager = windowContext.getSystemService(WindowManager::class.java)
    private val displayManager = context.getSystemService(DisplayManager::class.java)
    private val view = LineView(windowContext)
    private var attached = false

    private val params = WindowManager.LayoutParams(
        0,
        0,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        // Sin FLAG_NOT_TOUCHABLE a propósito: con él Android 12+ fuerza la ventana al 80 % de
        // opacidad y la línea se vería gris. Los toques en esos pocos px ya los recoge la
        // status bar, que está encima; NOT_FOCUSABLE deja pasar el resto de la pantalla.
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.START
        // Sin esto el sistema empujaría la ventana por debajo de la status bar.
        fitInsetsTypes = 0
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        title = "BatteryLine"
    }

    private val configCallbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) = layout()

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }

    // Girar 180° no cambia la orientación pero sí la posición del cutout: se reubica igual.
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            if (displayId == Display.DEFAULT_DISPLAY) layout()
        }

        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
    }

    fun show() {
        if (attached) return
        computeParams()
        windowManager.addView(view, params)
        attached = true
        windowContext.registerComponentCallbacks(configCallbacks)
        displayManager.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
    }

    fun hide() {
        if (!attached) return
        windowContext.unregisterComponentCallbacks(configCallbacks)
        displayManager.unregisterDisplayListener(displayListener)
        windowManager.removeViewImmediate(view)
        attached = false
    }

    /** Recalcula tamaño y posición; solo toca la ventana si algo cambió. */
    fun layout() {
        if (!attached) return
        val before = intArrayOf(params.x, params.y, params.width, params.height)
        computeParams()
        if (!before.contentEquals(intArrayOf(params.x, params.y, params.width, params.height))) {
            windowManager.updateViewLayout(view, params)
        }
    }

    /**
     * Muestra [level]. Con [animate] el cambio se suaviza; [fast] acorta la animación
     * para que la vista previa siga al dedo.
     */
    fun render(level: Int, animate: Boolean, fast: Boolean = false) {
        val fraction = (level.toFloat() / prefs.fullAt.coerceAtLeast(1)).coerceIn(0f, 1f)
        val color = if (prefs.lowEnabled && level < prefs.lowThreshold) LineView.LOW_COLOR else LineView.NORMAL_COLOR
        view.setStyle(prefs.opacity, prefs.anchor)
        if (animate) view.animateTo(fraction, color, if (fast) FAST_MS else SMOOTH_MS) else view.snap(fraction, color)
    }

    private fun computeParams() {
        val screen = Screen(windowManager)
        val thickness = prefs.thickness.coerceIn(1, screen.statusBarHeight)
        val offset = prefs.offset.coerceIn(0, screen.statusBarHeight - thickness)
        val left = prefs.marginLeft
        val right = prefs.marginRight
        params.x = left
        params.y = offset
        params.width = (screen.width - left - right).coerceAtLeast(1)
        params.height = thickness
    }

    private companion object {
        const val SMOOTH_MS = 2500L
        const val FAST_MS = 250L
    }
}
