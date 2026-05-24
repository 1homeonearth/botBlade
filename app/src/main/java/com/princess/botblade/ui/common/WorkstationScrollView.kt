package com.princess.botblade.ui.common

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.widget.ScrollView
import com.princess.botblade.R

class WorkstationScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : ScrollView(context, attrs) {
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = context.getColor(R.color.botblade_grid_line)
        strokeWidth = 1f
    }

    init {
        setBackgroundColor(context.getColor(R.color.botblade_black))
        isFillViewport = true
    }

    override fun dispatchDraw(canvas: Canvas) {
        val step = 120.dp.toFloat()
        val top = scrollY.toFloat()
        val bottom = top + height
        var x = 48.dp.toFloat()
        while (x < width) {
            canvas.drawLine(x, top, x, bottom, gridPaint)
            x += step
        }
        var y = ((top / step).toInt() * step)
        while (y < bottom) {
            canvas.drawLine(36.dp.toFloat(), y, width - 36.dp.toFloat(), y, gridPaint)
            y += step
        }
        super.dispatchDraw(canvas)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
