package com.princess.botblade.ui.settings

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import com.princess.botblade.R

class FeatureDeckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
        setPadding(0, 12.dp, 0, 4.dp)
        render()
    }

    private fun render() {
        removeAllViews()
        addView(sectionTitle("Forge roadmap"))
        addView(sectionCopy("BotBlade is becoming a mobile forge for importing, repairing, editing, running, and shipping bots. These lanes make the finished product visible while the backend catches up."))

        BotBladeFeatureDeck.cards.forEach { card ->
            addView(featureCard(card))
        }
    }

    private fun sectionTitle(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
        textSize = 22f
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 16.dp
        }
    }

    private fun sectionCopy(text: String): TextView = TextView(context).apply {
        this.text = text
        setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
        textSize = 14f
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 6.dp
            bottomMargin = 8.dp
        }
    }

    private fun featureCard(card: BotBladeFeatureCard): MaterialCardView = MaterialCardView(context).apply {
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
            text = card.title
            setTextColor(resources.getColor(R.color.botblade_hot_pink, context.theme))
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })

        body.addView(TextView(context).apply {
            text = card.status
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 13f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 4.dp, 0, 0)
        })

        body.addView(TextView(context).apply {
            text = card.detail
            setTextColor(resources.getColor(R.color.botblade_on_surface, context.theme))
            textSize = 14f
            setPadding(0, 6.dp, 0, 0)
        })

        addView(body)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
