package com.princess.botblade.ui.dashboard

import android.content.Context
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.GridLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.princess.botblade.R
import com.princess.botblade.ui.common.WorkstationCardView

class BotWorkstationDeckView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, 12.dp)
        render()
    }

    private fun render() {
        addView(flowCard())
        addView(releasePathsCard())
    }

    private fun flowCard(): WorkstationCardView = WorkstationCardView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 28.dp
        }
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24.dp, 22.dp, 24.dp, 24.dp)
        }
        body.addView(title("A-to-Z flow"))
        body.addView(copy("Create/import → scan → configure secrets → edit → build → run → inspect → deploy → release"))
        val grid = GridLayout(context).apply {
            columnCount = 3
            rowCount = 3
            setPadding(0, 12.dp, 0, 0)
        }
        listOf(
            "01   Create",
            "02   Scan",
            "03   Vault",
            "04   Edit",
            "05   Build",
            "06   Run",
            "07   Inspect",
            "08   Deploy",
            "09   Release",
        ).forEach { label -> grid.addView(stepChip(label)) }
        body.addView(grid)
        addView(body)
    }

    private fun releasePathsCard(): WorkstationCardView = WorkstationCardView(context).apply {
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 28.dp
        }
        val body = LinearLayout(context).apply {
            orientation = VERTICAL
            setPadding(24.dp, 22.dp, 24.dp, 24.dp)
        }
        body.addView(title("Legitimate release paths"))
        body.addView(copy("Discord slash commands, Slack Bolt apps, webhook bots, Telegram, Matrix, Reddit, RSS, and local automation bots."))
        body.addView(releaseRow("Discord", "token + intents + command register", R.color.botblade_hot_pink))
        body.addView(releaseRow("Slack", "signing secret + app token + events", R.color.botblade_baby_blue))
        body.addView(releaseRow("Webhook", "endpoint + env + deploy target", R.color.botblade_neon_lavender))
        addView(body)
    }

    private fun title(value: String): TextView = TextView(context).apply {
        text = value
        setTextColor(resources.getColor(R.color.botblade_baby_blue, context.theme))
        textSize = 26f
        typeface = Typeface.DEFAULT_BOLD
    }

    private fun copy(value: String): TextView = TextView(context).apply {
        text = value
        setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
        textSize = 16f
        setPadding(0, 8.dp, 0, 0)
    }

    private fun stepChip(label: String): TextView = TextView(context).apply {
        text = label
        setTextColor(resources.getColor(R.color.botblade_on_surface, context.theme))
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setBackgroundResource(R.drawable.botblade_chip_outline)
        setPadding(16.dp, 14.dp, 16.dp, 14.dp)
        layoutParams = GridLayout.LayoutParams().apply {
            width = 0
            height = 58.dp
            columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            setMargins(6.dp, 8.dp, 6.dp, 8.dp)
        }
    }

    private fun releaseRow(platform: String, detail: String, colorRes: Int): TextView = TextView(context).apply {
        text = "$platform        $detail"
        setTextColor(resources.getColor(colorRes, context.theme))
        textSize = 15f
        typeface = Typeface.DEFAULT_BOLD
        setBackgroundResource(R.drawable.botblade_chip_outline)
        setPadding(16.dp, 12.dp, 16.dp, 12.dp)
        layoutParams = LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            topMargin = 10.dp
        }
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}