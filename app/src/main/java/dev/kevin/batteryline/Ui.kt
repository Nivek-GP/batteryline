package dev.kevin.batteryline

import android.app.Activity
import android.content.Context
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlin.math.roundToInt

/** Barras del sistema transparentes con iconos claros; el contenido se dibuja detrás. */
fun Activity.enableDapEdgeToEdge() {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    WindowCompat.getInsetsController(window, window.decorView).run {
        isAppearanceLightStatusBars = false
        isAppearanceLightNavigationBars = false
    }
}

/** Suma los insets de las barras del sistema al padding original de la vista. */
fun View.padForSystemBars(top: Boolean = true, bottom: Boolean = true) {
    val start = paddingLeft
    val initialTop = paddingTop
    val end = paddingRight
    val initialBottom = paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(this) { view, insets ->
        val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
        view.setPadding(
            start + bars.left,
            initialTop + if (top) bars.top else 0,
            end + bars.right,
            initialBottom + if (bottom) bars.bottom else 0,
        )
        insets
    }
}

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

fun View.fadeIn(duration: Long = 150) {
    alpha = 0f
    animate().alpha(1f).setDuration(duration).start()
}
