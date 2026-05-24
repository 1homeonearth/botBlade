package com.princess.botblade.ui.dashboard

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.princess.botblade.R
import com.princess.botblade.ui.common.WorkstationCardView

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
        addView(activeWorkspaceCard())
    }

    private fun activeWorkspaceCard(): WorkstationCardView = WorkstationCardView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 28.dp
        }
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24.dp, 22.dp, 24.dp, 22.dp)
        }
        body.addView(TextView(context).apply {
            text = "Active workspace"
            setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
            textSize = 26f
            typeface = Typeface.DEFAULT_BOLD
        })
        body.addView(TextView(context).apply {
            text = "discord-moderator-pro • Discord.js • Node 22 • local Docker target"
            setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
            textSize = 16f
            setPadding(0, 8.dp, 0, 0)
        })
        val chips = LinearLayout(context).apply {
            orientation = HORIZONTAL
            setPadding(0, 24.dp, 0, 0)
        }
        chips.addView(chip("Backend healthy", R.color.botblade_success))
        chips.addView(chip("Secrets ready", R.color.botblade_baby_blue))
        chips.addView(chip("Build passing", R.color.botblade_success))
        body.addView(chips)
        addView(body)
    }

    private fun chip(label: String, colorRes: Int): TextView = TextView(context).apply {
        text = label
        setTextColor(resources.getColor(colorRes, context.theme))
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setBackgroundResource(R.drawable.botblade_chip_outline)
        setPadding(14.dp, 8.dp, 14.dp, 8.dp)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            marginEnd = 8.dp
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}