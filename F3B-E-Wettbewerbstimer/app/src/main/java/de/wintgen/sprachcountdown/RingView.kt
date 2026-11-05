package de.wintgen.sprachcountdown

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.core.content.ContextCompat

/** Kreisförmiger Fortschrittsring mit weichem Farbübergang. */
class RingView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val density = resources.displayMetrics.density
    private val stroke = 14f * density

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        color = ContextCompat.getColor(context, R.color.track)
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke * 2.2f
        strokeCap = Paint.Cap.ROUND
    }
    private val arcPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = stroke
        strokeCap = Paint.Cap.ROUND
    }

    private val rect = RectF()

    /** 0.0 … 1.0 – Anteil der verbleibenden Zeit */
    var progress = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    private var currentColor = ContextCompat.getColor(context, R.color.phase_calm)
    private var colorAnimator: ValueAnimator? = null

    init {
        applyColor(currentColor)
    }

    fun setRingColor(target: Int) {
        if (target == currentColor) return
        colorAnimator?.cancel()
        colorAnimator = ValueAnimator.ofObject(ArgbEvaluator(), currentColor, target).apply {
            duration = 450
            addUpdateListener { applyColor(it.animatedValue as Int) }
            start()
        }
        currentColor = target
    }

    private fun applyColor(c: Int) {
        arcPaint.color = c
        // halbtransparenter „Glow" unter dem Bogen
        glowPaint.color = (c and 0x00FFFFFF) or (0x22 shl 24)
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        setMeasuredDimension(w, w) // immer quadratisch
    }

    override fun onDraw(canvas: Canvas) {
        val inset = glowPaint.strokeWidth / 2f
        rect.set(inset, inset, width - inset, height - inset)
        canvas.drawOval(rect, trackPaint)
        val sweep = 360f * progress
        if (sweep > 0f) {
            canvas.drawArc(rect, -90f, sweep, false, glowPaint)
            canvas.drawArc(rect, -90f, sweep, false, arcPaint)
        }
    }
}
