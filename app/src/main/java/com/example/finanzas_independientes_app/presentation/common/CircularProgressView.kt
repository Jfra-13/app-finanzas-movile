package com.example.finanzas_independientes_app.presentation.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.graphics.ColorUtils
import com.google.android.material.color.MaterialColors

/**
 * Full-circle progress ring with rounded caps, drawn on a Canvas. Replaces the
 * ring-shape ProgressBar hack, whose arc ends were always square (Android's
 * ring drawable has no round-cap option). Colors come from theme tokens
 * (onPrimary over the blue weekly card) so it adapts to light/dark.
 */
class CircularProgressView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val ringColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnPrimary)

    private val strokeWidth = dp(12f)

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@CircularProgressView.strokeWidth
        color = ColorUtils.setAlphaComponent(ringColor, 77) // ~30%
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@CircularProgressView.strokeWidth
        color = ringColor
    }

    private val arcRect = RectF()

    /** 0f..1f — animated sweep of the progress arc. */
    private var progress = 0f
    private var animator: ValueAnimator? = null

    /** Animate the ring to [fraction] (0..1). */
    fun setProgress(fraction: Float) {
        val target = fraction.coerceIn(0f, 1f)
        animator?.cancel()
        animator = ValueAnimator.ofFloat(progress, target).apply {
            duration = 650
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val size = minOf(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec),
        )
        setMeasuredDimension(size, size)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val inset = strokeWidth / 2f
        val size = minOf(w, h).toFloat()
        arcRect.set(inset, inset, size - inset, size - inset)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(arcRect, -90f, 360f, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(arcRect, -90f, 360f * progress, false, progressPaint)
        }
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
