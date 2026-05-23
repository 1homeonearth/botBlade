package com.princess.botblade.ui.dashboard

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.princess.botblade.R

class CommercialHomeDeckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, 12.dp)
        render()
    }

    private fun render() {
        addView(hero())
        addView(sectionTitle("Primary flow"))
        CommercialHomeDeck.primaryActions.forEach { addView(laneCard(it)) }
        addView(sectionTitle("Workspace health"))
        CommercialHomeDeck.workspaceHealth.forEach { addView(laneCard(it)) }
    }

    private fun hero(): MaterialCardView = MaterialCardView(context).apply {
        setCardBackgroundColor(resources.getColor(R.color.botblade_panel_raised, context.theme))
        strokeColor = resources.getColor(R.color.botblade_hot_pink, context.theme)
        strokeWidth = 1.dp
        radius = 24.dp.toFloat()
        cardElevation = 0f
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(18.dp, 18.dp, 18.dp, 18.dp)
        }
        body.addView(TextView(context).apply {
            text = "BotBlade"
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(context).apply {
            text = "A mobile forge for importing, repairing, editing, running, deploying, and auditing bots."
            setTextColor(resources.getColor(R.color.botblade_on_surface, context.theme))
            textSize = 15f
            setPadding(0, 8.dp, 0, 0)
        })
        body.addView(TextView(context).apply {
            text = "Commercial shell pattern: command center, active workspace, health strip, primary actions, diagnostics, and release rail."
            setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
            textSize = 13f
            setPadding(0, 10.dp, 0, 0)
        })
        addView(body)
    }

    private fun sectionTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(resources.getColor(R.color.botblade_hot_pink, context.theme))
        textSize = 18f
        typeface = Typeface.DEFAULT_BOLD
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 18.dp
        }
    }

    private fun laneCard(lane: CommercialHomeLane): MaterialCardView = MaterialCardView(context).apply {
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
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(context).apply {
            text = lane.tag
            setTextColor(resources.getColor(R.color.botblade_hot_pink, context.theme))
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
