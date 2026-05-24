package com.princess.botblade.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import com.google.android.material.card.MaterialCardView
import com.princess.botblade.R

class WorkstationCardView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : MaterialCardView(context, attrs) {
    private val accentColor: Int
    private val accentWidth: Float
    private val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    init {
        val values = context.obtainStyledAttributes(attrs, R.styleable.WorkstationCardView)
        accentColor = values.getColor(R.styleable.WorkstationCardView_accentColor, context.getColor(R.color.botblade_hot_pink))
        accentWidth = values.getDimension(R.styleable.WorkstationCardView_accentWidth, 6.dp.toFloat())
        values.recycle()
        setCardBackgroundColor(context.getColor(R.color.botblade_panel))
        strokeColor = context.getColor(R.color.botblade_panel_stroke)
        strokeWidth = 1.dp
        radius = 22.dp.toFloat()
        cardElevation = 0f
        accentPaint.color = accentColor
    }

    override fun dispatchDraw(canvas: Canvas) {
        canvas.drawRoundRect(0f, 0f, accentWidth, height.toFloat(), accentWidth, accentWidth, accentPaint)
        super.dispatchDraw(canvas)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
