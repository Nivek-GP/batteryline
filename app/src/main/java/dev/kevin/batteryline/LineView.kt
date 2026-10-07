package dev.kevin.batteryline

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import android.view.animation.DecelerateInterpolator

/**
 * La línea: un único rectángulo. Los cambios de nivel se animan para que el salto
 * entre porcentajes (varios px en pantallas anchas) no se note.
 */
@SuppressLint("ViewConstructor")
class LineView(context: Context) : View(context) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val argb = ArgbEvaluator()

    private var fraction = 0f
    private var color = NORMAL_COLOR
    private var alphaPercent = 100
    private var anchor = Anchor.RIGHT
    private var animator: ValueAnimator? = null

    /** Aplica el estado sin animación (primer dibujo o pantalla apagada). */
    fun snap(target: Float, targetColor: Int) {
        animator?.cancel()
        fraction = target
        color = targetColor
        invalidate()
    }

    /** Lleva la línea suavemente al nuevo largo y color, partiendo de lo que se ve ahora. */
    fun animateTo(target: Float, targetColor: Int, duration: Long) {
        if (target == fraction && targetColor == color) return
        animator?.cancel()
        val fromFraction = fraction
        val fromColor = color
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val t = it.animatedValue as Float
                fraction = fromFraction + (target - fromFraction) * t
                color = argb.evaluate(t, fromColor, targetColor) as Int
                invalidate()
            }
            start()
        }
    }

    fun setStyle(opacity: Int, anchor: Anchor) {
        if (opacity == alphaPercent && anchor == this.anchor) return
        alphaPercent = opacity
        this.anchor = anchor
        invalidate()
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val length = w * fraction
        if (length <= 0f) return
        val left = when (anchor) {
            Anchor.LEFT -> 0f
            Anchor.CENTER -> (w - length) / 2f
            Anchor.RIGHT -> w - length
        }
        paint.color = color
        paint.alpha = (paint.alpha * alphaPercent / 100f).toInt()
        canvas.drawRect(left, 0f, left + length, height.toFloat(), paint)
    }

    companion object {
        const val NORMAL_COLOR = 0xFFFFFFFF.toInt()

        /** Rojo oscuro, pero lo bastante vivo para distinguirse con 1 px sobre negro. */
        const val LOW_COLOR = 0xFFC62828.toInt()
    }
}
