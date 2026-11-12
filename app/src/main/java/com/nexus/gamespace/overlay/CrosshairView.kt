package com.nexus.gamespace.overlay

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.view.View
import com.nexus.gamespace.domain.CrosshairConfig
import kotlin.math.max

/**
 * Dibuja la mira: formas fijas (punto, cruz, círculo) en el centro de la vista. Es exclusivamente cosmética:
 * no lee la pantalla ni el juego y no recibe toques. La usan la ventana de superposición y la vista previa.
 */
class CrosshairView(context: Context) : View(context) {

    var config: CrosshairConfig = CrosshairConfig()
        set(value) {
            field = value
            requestLayout()
            invalidate()
        }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val d = resources.displayMetrics.density
        val side = ((config.sizeDp + config.thicknessDp * 2 + 4) * d).toInt().coerceAtLeast(1)
        setMeasuredDimension(side, side)
    }

    override fun onDraw(canvas: Canvas) {
        val d = resources.displayMetrics.density
        val cx = width / 2f
        val cy = height / 2f
        val radius = config.sizeDp / 2f * d
        val thickness = config.thicknessDp * d
        val gap = config.gapDp * d
        val parts = config.parts()

        val alpha = (config.alphaPercent / 100f * 255).toInt().coerceIn(0, 255)
        val rgb = (config.colorArgb and 0xFFFFFFL).toInt()
        paint.color = (alpha shl 24) or rgb

        if (parts.circle) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = thickness
            canvas.drawCircle(cx, cy, max(radius - thickness / 2f, 1f), paint)
        }
        if (parts.cross && gap < radius) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = thickness
            canvas.drawLine(cx - radius, cy, cx - gap, cy, paint)
            canvas.drawLine(cx + gap, cy, cx + radius, cy, paint)
            canvas.drawLine(cx, cy - radius, cx, cy - gap, paint)
            canvas.drawLine(cx, cy + gap, cx, cy + radius, paint)
        }
        if (parts.dot) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(cx, cy, max(thickness * 0.9f, 1.5f * d), paint)
        }
    }
}
