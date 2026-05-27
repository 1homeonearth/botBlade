package com.princess.botblade.releases

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.AttributeSet
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
    private val installButton = Button(context)
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
            text = "BotBlade checks GitHub releases and opens the newest APK. Android still asks before installing sideloaded updates."
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
        addView(installButton.apply {
            text = "Install latest APK"
            isEnabled = false
            setOnClickListener { installLatest() }
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
        else refreshKnownLatest()
    }

    private fun checkNow() {
        val owner = findViewTreeLifecycleOwner() ?: return
        statusText.text = "Checking GitHub releases..."
        checkButton.isEnabled = false
        owner.lifecycleScope.launch {
            runCatching { checker.checkLatestRelease() }
                .onSuccess { info ->
                    latest = info
                    if (info == null) {
                        checker.markChecked()
                        statusText.text = "BotBlade is current for this release channel."
                        openReleaseButton.isEnabled = false
                        installButton.isEnabled = false
                    } else {
                        checker.markChecked()
                        statusText.text = "Update available: ${info.tagName}${info.assetName?.let { " • $it" } ?: ""}"
                        openReleaseButton.isEnabled = true
                        installButton.isEnabled = canInstallPackageUpdates()
                    }
                }
                .onFailure { error ->
                    statusText.text = "Update check failed: ${error.message ?: "unknown error"}"
                    openReleaseButton.isEnabled = false
                    installButton.isEnabled = false
                }
            checkButton.isEnabled = true
        }
    }

    private fun refreshKnownLatest() {
        val owner = findViewTreeLifecycleOwner() ?: return
        owner.lifecycleScope.launch {
            runCatching { checker.latestReleaseForChannel() }
                .onSuccess { info ->
                    latest = info
                    openReleaseButton.isEnabled = info != null
                    installButton.isEnabled = info != null && canInstallPackageUpdates()
                }
        }
    }

    private fun installLatest() {
        if (!isOnline()) {
            statusText.text = "Install skipped: device appears offline."
            return
        }
        val info = latest ?: return
        val assetUrl = info.assetUrl?.takeIf { it.isNotBlank() }
        if (assetUrl == null) {
            statusText.text = "No APK asset URL found in latest release."
            return
        }
        statusText.text = "Opening latest APK for install…"
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(assetUrl)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun openLatest() {
        val info = latest ?: return
        val target = info.assetUrl?.takeIf { it.isNotBlank() } ?: info.pageUrl
        if (target.isBlank()) return
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(target)))
    }

    private fun canInstallPackageUpdates(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else true
    }

    private fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private val Int.dp: Int
        get() = (this * resources.displayMetrics.density).toInt()
}
