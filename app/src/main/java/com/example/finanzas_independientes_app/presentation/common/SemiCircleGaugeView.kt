package com.example.finanzas_independientes_app.presentation.common

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.google.android.material.color.MaterialColors
import kotlin.math.min

/**
 * Banking-style semicircular gauge drawn on a Canvas: a grey track arc, a
 * blue progress arc with a rounded cap, the current amount auto-sized to fit
 * inside the bowl (so it can never overflow), and a small objective label
 * underneath. Replaces the two-rotated-ProgressBar hack that clipped and
 * duplicated the currency symbol.
 */
class SemiCircleGaugeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val progressColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorPrimary)
    private val trackColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorSurfaceVariant)
    private val objetivoColor =
        MaterialColors.getColor(this, com.google.android.material.R.attr.colorOnSurfaceVariant)

    private val strokeWidth = dp(14f)

    /** Currency symbol is rendered at this fraction of the surrounding text size. */
    private val symbolScale = 0.6f

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@SemiCircleGaugeView.strokeWidth
        color = trackColor
    }
    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = this@SemiCircleGaugeView.strokeWidth
        color = progressColor
    }
    private val amountPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = progressColor
        isFakeBoldText = true
        textAlign = Paint.Align.CENTER
    }
    private val objetivoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = objetivoColor
        textAlign = Paint.Align.CENTER
        textSize = dp(14f)
    }
    private val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = objetivoColor
        strokeWidth = dp(1f)
        alpha = 70
    }

    private val arcRect = RectF()

    /** 0f..1f — animated sweep of the progress arc. */
    private var progress = 0f
    private var animator: ValueAnimator? = null

    var amountText: String = ""
        set(value) {
            field = value
            invalidate()
        }
    var objetivoText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    /** Animate the arc to [fraction] (0..1). Tactile feedback, banking trust signal. */
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
        val w = MeasureSpec.getSize(widthMeasureSpec)
        // Arc bowl height is half the width; add room below for the objetivo label.
        val h = (w / 2f + strokeWidth + dp(20f)).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        val inset = strokeWidth / 2f
        // Square whose top half is the visible semicircle.
        arcRect.set(inset, inset, w - inset, w - inset)
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawArc(arcRect, 180f, 180f, false, trackPaint)
        if (progress > 0f) {
            canvas.drawArc(arcRect, 180f, 180f * progress, false, progressPaint)
        }

        val cx = arcRect.centerX()
        val dividerY = arcRect.centerY()
        // Fit the amount inside the arc's inner diameter so it never clips.
        val maxWidth = arcRect.width() - strokeWidth * 2 - dp(8f)
        amountPaint.textSize = fitTextSize(amountText, maxWidth, dp(28f))
        // Sit the amount just above the diameter — inside the bowl, slightly above centre.
        val amountBaseline = dividerY - dp(8f)
        drawWithSmallSymbol(canvas, amountText, cx, amountBaseline, amountPaint)

        // Divider on the diameter, separating the amount from the objetivo label.
        val dividerHalf = maxWidth / 2f
        canvas.drawLine(cx - dividerHalf, dividerY, cx + dividerHalf, dividerY, dividerPaint)

        if (objetivoText.isNotEmpty()) {
            val objY = dividerY + strokeWidth + dp(8f)
            drawWithSmallSymbol(canvas, objetivoText, cx, objY, objetivoPaint)
        }
    }

    /**
     * Draws [text] centred at [cx], rendering the "S/" currency token at
     * [symbolScale] of the base size while keeping its position in the string.
     * The whole line stays horizontally centred regardless of the mixed sizes.
     */
    private fun drawWithSmallSymbol(
        canvas: Canvas,
        text: String,
        cx: Float,
        baseline: Float,
        basePaint: Paint,
    ) {
        val idx = text.indexOf("S/")
        if (idx < 0) {
            canvas.drawText(text, cx, baseline, basePaint)
            return
        }

        val big = Paint(basePaint).apply { textAlign = Paint.Align.LEFT }
        val small = Paint(big).apply { textSize = basePaint.textSize * symbolScale }

        val before = text.substring(0, idx)
        val symbol = "S/"
        val after = text.substring(idx + symbol.length)

        val total = big.measureText(before) + small.measureText(symbol) + big.measureText(after)
        var x = cx - total / 2f
        canvas.drawText(before, x, baseline, big)
        x += big.measureText(before)
        canvas.drawText(symbol, x, baseline, small)
        x += small.measureText(symbol)
        canvas.drawText(after, x, baseline, big)
    }

    /** Largest text size (capped at [maxSize]) whose rendered width fits [maxWidth]. */
    private fun fitTextSize(text: String, maxWidth: Float, maxSize: Float): Float {
        if (text.isEmpty()) return maxSize
        var size = maxSize
        val probe = Paint(amountPaint)
        while (size > dp(10f)) {
            probe.textSize = size
            if (probe.measureText(text) <= maxWidth) break
            size -= 1f
        }
        return min(size, maxSize)
    }

    private fun dp(value: Float): Float = value * resources.displayMetrics.density
}
