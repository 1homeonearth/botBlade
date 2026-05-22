package com.princess.botblade.ui.onboarding

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.MainActivity
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

private val Context.dataStore by preferencesDataStore("onboarding")

class OnboardingFragment : Fragment() {
    private val finishTriggered = AtomicBoolean(false)

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                OnboardingPager(onFinish = ::finishOnboardingFlow)
            }
        }
    }

    private fun finishOnboardingFlow() {
        if (!isAdded || !finishTriggered.compareAndSet(false, true)) return
        val host = activity as? MainActivity ?: return
        val appContext = requireContext().applicationContext
        host.lifecycleScope.launch {
            runCatching {
                appContext.dataStore.edit { prefs -> prefs[booleanPreferencesKey("onboarding_complete")] = true }
            }
            runCatching { host.finishOnboarding() }
                .onFailure { finishTriggered.set(false) }
        }
    }
}

private data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingPager(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Default.Build, "Import or build bots", "Bring in a bot project or start a new one."),
        OnboardingPage(Icons.Default.Dashboard, "Monitor bots", "Watch logs, builds, and runtime health."),
        OnboardingPage(Icons.Default.Folder, "Manage projects", "Edit files and keep each project organized."),
        OnboardingPage(Icons.Default.Notifications, "Allow notifications", "Get build status and deployment updates."),
        OnboardingPage(Icons.Default.RocketLaunch, "Deploy faster", "Run checks and ship updates with confidence."),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isLastPage = pagerState.currentPage == pages.lastIndex
    var permissionStatus by remember { mutableStateOf("Optional") }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionStatus = if (granted) "Allowed" else "Not allowed"
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Spacer(modifier = Modifier.height(120.dp))
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth()) { pageIndex ->
                val page = pages[pageIndex]
                Box(
                    modifier = Modifier.fillMaxWidth().height(290.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(30.dp))
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Icon(imageVector = page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(38.dp))
                        Text(text = page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)
                        Text(text = page.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (pageIndex == 3) {
                            Button(onClick = {
                                if (Build.VERSION.SDK_INT < 33) {
                                    permissionStatus = "Already allowed on this Android version"
                                } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                                    permissionStatus = "Already allowed"
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }) { Text("Grant notification permission") }
                            Text(permissionStatus, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier.padding(horizontal = 4.dp).size(if (isSelected) 10.dp else 8.dp)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f), CircleShape),
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { if (isLastPage) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (isLastPage) "Finish" else "Next") }
        }
    }
}
