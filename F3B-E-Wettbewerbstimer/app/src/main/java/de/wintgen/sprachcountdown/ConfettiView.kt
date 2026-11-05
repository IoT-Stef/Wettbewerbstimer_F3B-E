package de.wintgen.sprachcountdown

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.os.SystemClock
import android.view.View
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * Durchsichtige Ebene über dem Bildschirm, die Konfetti regnen lässt.
 * Fängt keine Berührungen ab – die Buttons darunter bleiben bedienbar.
 */
class ConfettiView(context: Context) : View(context) {

    private class Piece(
        var x: Float, var y: Float,
        var vx: Float, var vy: Float,
        var angle: Float, val spin: Float,
        val w: Float, val h: Float,
        val color: Int, val round: Boolean,
        val swayPhase: Float, val swaySpeed: Float
    )

    private val colors = intArrayOf(
        0xFF4ADE80.toInt(), 0xFFFBBF24.toInt(), 0xFFF87171.toInt(),
        0xFF60A5FA.toInt(), 0xFFC084FC.toInt(), 0xFFF472B6.toInt(), 0xFFFFFFFF.toInt()
    )
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pieces = mutableListOf<Piece>()
    private val density = resources.displayMetrics.density
    private var lastFrame = 0L
    private var startTime = 0L

    companion object {
        private const val DURATION_MS = 4500L
    }

    init {
        isClickable = false
        isFocusable = false
    }

    /** Startet einen Konfetti-Regen: zwei „Kanonen" unten + Regen von oben. */
    fun burst() {
        if (width == 0 || height == 0) {
            post { burst() }
            return
        }
        pieces.clear()
        val w = width.toFloat()
        val h = height.toFloat()
        // Kanonen links und rechts unten, schräg nach oben
        for (side in 0..1) {
            repeat(70) {
                val dir = if (side == 0) 1f else -1f
                val speed = Random.nextFloat() * 0.9f + 0.9f          // in Bildschirmhöhen pro s
                val a = Math.toRadians((60 + Random.nextFloat() * 25).toDouble())
                pieces += newPiece(
                    x = if (side == 0) 0f else w,
                    y = h * 0.85f,
                    vx = dir * (cos(a) * speed * h * 0.55f).toFloat(),
                    vy = -(sin(a) * speed * h).toFloat()
                )
            }
        }
        // Regen von oben
        repeat(90) {
            pieces += newPiece(
                x = Random.nextFloat() * w,
                y = -Random.nextFloat() * h * 0.6f,
                vx = (Random.nextFloat() - 0.5f) * 80 * density,
                vy = Random.nextFloat() * 120 * density
            )
        }
        startTime = SystemClock.uptimeMillis()
        lastFrame = startTime
        alpha = 1f
        visibility = VISIBLE
        postInvalidateOnAnimation()
    }

    private fun newPiece(x: Float, y: Float, vx: Float, vy: Float): Piece {
        val size = (Random.nextFloat() * 5 + 6) * density
        return Piece(
            x, y, vx, vy,
            angle = Random.nextFloat() * 360,
            spin = (Random.nextFloat() - 0.5f) * 720,
            w = size, h = size * (0.4f + Random.nextFloat() * 0.4f),
            color = colors[Random.nextInt(colors.size)],
            round = Random.nextInt(5) == 0,
            swayPhase = Random.nextFloat() * 6.28f,
            swaySpeed = 2f + Random.nextFloat() * 3f
        )
    }

    override fun onDraw(canvas: Canvas) {
        if (pieces.isEmpty()) return
        val now = SystemClock.uptimeMillis()
        val dt = ((now - lastFrame).coerceAtMost(40L)) / 1000f
        lastFrame = now
        val elapsed = now - startTime
        val gravity = 900 * density
        val t = elapsed / 1000f

        // In der letzten Sekunde ausblenden
        val fade = ((DURATION_MS - elapsed) / 1000f).coerceIn(0f, 1f)

        for (p in pieces) {
            p.vy += gravity * dt
            // Luftwiderstand: Konfetti segelt langsam
            p.vx *= (1 - 1.8f * dt)
            p.vy = p.vy.coerceAtMost(260 * density)
            p.x += (p.vx + sin(t * p.swaySpeed + p.swayPhase) * 40 * density) * dt
            p.y += p.vy * dt
            p.angle += p.spin * dt

            paint.color = p.color
            paint.alpha = (255 * fade).toInt()
            canvas.save()
            canvas.translate(p.x, p.y)
            canvas.rotate(p.angle)
            if (p.round) {
                canvas.drawCircle(0f, 0f, p.h * 0.7f, paint)
            } else {
                // Breite schwankt → Eindruck von Drehen im Raum
                val flip = kotlin.math.abs(cos(t * p.swaySpeed * 1.7f + p.swayPhase))
                canvas.drawRect(-p.w / 2 * flip, -p.h / 2, p.w / 2 * flip, p.h / 2, paint)
            }
            canvas.restore()
        }

        if (elapsed < DURATION_MS) {
            postInvalidateOnAnimation()
        } else {
            pieces.clear()
            invalidate()
        }
    }

    fun clear() {
        pieces.clear()
        invalidate()
    }
}
