package com.princess.botblade.ui.editor

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.princess.botblade.R

class ForgeEditorDeckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
        setPadding(0, 12.dp, 0, 8.dp)
        render()
    }

    private fun render() {
        addView(TextView(context).apply {
            text = "Forge Editor"
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(context).apply {
            text = "Inspired by Monaco Editor, VS Code, CodeMirror 6, and Acode: models, command palette, diagnostics, language modes, bot map, and a container-only terminal."
            setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
            textSize = 14f
            setPadding(0, 6.dp, 0, 8.dp)
        })
        ForgeEditorDeck.lanes.forEach { lane -> addView(laneCard(lane)) }
    }

    private fun laneCard(lane: ForgeEditorLane): MaterialCardView = MaterialCardView(context).apply {
        setCardBackgroundColor(resources.getColor(R.color.botblade_panel, context.theme))
        strokeColor = resources.getColor(R.color.botblade_panel_stroke, context.theme)
        strokeWidth = 1.dp
        radius = 18.dp.toFloat()
        cardElevation = 0f
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10.dp
        }
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(16.dp, 14.dp, 16.dp, 14.dp)
        }
        body.addView(TextView(context).apply {
            text = lane.title
            setTextColor(resources.getColor(R.color.botblade_hot_pink, context.theme))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(context).apply {
            text = lane.status
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 13f
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 4.dp, 0, 0)
        })
        body.addView(TextView(context).apply {
            text = lane.detail
            setTextColor(resources.getColor(R.color.botblade_on_surface, context.theme))
            textSize = 14f
            setPadding(0, 6.dp, 0, 0)
        })
        addView(body)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
