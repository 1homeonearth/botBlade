package com.princess.botblade.releases

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.material.switchmaterial.SwitchMaterial
import com.princess.botblade.R
import kotlinx.coroutines.launch

class AppUpgradePanelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : LinearLayout(context, attrs) {
    private val checker = AppUpgradeChecker(context)
    private val statusText = TextView(context)
    private val autoCheckSwitch = SwitchMaterial(context)
    private val checkButton = Button(context)
    private val openReleaseButton = Button(context)
    private var latest: AppUpgradeInfo? = null

    init {
        orientation = VERTICAL
        setPadding(0, 12.dp, 0, 8.dp)
        addView(TextView(context).apply {
            text = "App updates"
            setTextColor(resources.getColor(R.color.botblade_hot_pink, context.theme))
            textSize = 18f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        })
        addView(TextView(context).apply {
            text = "BotBlade can check GitHub releases and open the newest APK. Android still asks before installing sideloaded updates."
            setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
            textSize = 14f
            setPadding(0, 6.dp, 0, 4.dp)
        })
        addView(autoCheckSwitch.apply {
            text = "Check for release updates automatically"
            setTextColor(resources.getColor(R.color.botblade_on_surface, context.theme))
            isChecked = checker.autoCheckEnabled()
            setOnCheckedChangeListener { _, checked -> checker.setAutoCheckEnabled(checked) }
        })
        addView(statusText.apply {
            text = "No update check has run in this session."
            setTextColor(resources.getColor(R.color.botblade_on_surface_muted, context.theme))
            setPadding(0, 6.dp, 0, 6.dp)
        })
        addView(checkButton.apply {
            text = "Check latest release"
            setOnClickListener { checkNow() }
        })
        addView(openReleaseButton.apply {
            text = "Open latest release"
            isEnabled = false
            setOnClickListener { openLatest() }
        })
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (checker.shouldCheckNow()) checkNow()
    }

    private fun checkNow() {
        val owner = findViewTreeLifecycleOwner() ?: return
        statusText.text = "Checking GitHub releases..."
        checkButton.isEnabled = false
        owner.lifecycleScope.launch {
            runCatching { checker.checkLatestRelease() }
                .onSuccess { info ->
                    checker.markChecked()
                    latest = info
                    if (info == null) {
                        statusText.text = "BotBlade is current for this release channel."
                        openReleaseButton.isEnabled = false
                    } else {
                        statusText.text = "Update available: ${info.tagName}${info.assetName?.let { " • $it" } ?: ""}"
                        openReleaseButton.isEnabled = true
                    }
                }
                .onFailure { error ->
                    statusText.text = "Update check failed: ${error.message ?: "unknown error"}"
                    openReleaseButton.isEnabled = false
                }
            checkButton.isEnabled = true
        }
    }

    private fun openLatest() {
        val info = latest ?: return
        val target = info.assetUrl?.takeIf { it.isNotBlank() } ?: info.pageUrl
        if (target.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
