package com.princess.botblade.ui.onboarding

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import java.util.concurrent.atomic.AtomicBoolean
import com.princess.botblade.MainActivity
import com.princess.botblade.ui.theme.BotBladeTheme
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("onboarding")

class OnboardingFragment : Fragment() {
    private val finishTriggered = AtomicBoolean(false)
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme { OnboardingPager { finishOnboardingFlow() } } }
    }

    private fun finishOnboardingFlow() {
        if (!isAdded || !finishTriggered.compareAndSet(false, true)) return
        val host = activity as? MainActivity ?: return
        val appContext = requireContext().applicationContext
        // Write completion flag on the Activity scope, then navigate.
        // Do NOT re-check isAdded inside the coroutine — the outer check
        // already validated safe state, and we're on the Activity's scope.
        host.lifecycleScope.launch {
            try {
                appContext.dataStore.edit { prefs ->
                    prefs[booleanPreferencesKey("onboarding_complete")] = true
                }
            } catch (e: Exception) {
                android.util.Log.e("OnboardingFragment", "DataStore write failed", e)
                // Continue to navigation even if persistence fails —
                // a failed write is recoverable; a stuck screen is not.
            }
            try {
                host.finishOnboarding()
            } catch (e: Exception) {
                android.util.Log.e("OnboardingFragment", "finishOnboarding failed", e)
                // If finishOnboarding throws, show a message rather than silently doing nothing.
                android.widget.Toast.makeText(
                    appContext,
                    "Tap again or restart the app to continue",
                    android.widget.Toast.LENGTH_LONG
                ).show()
                // Reset the guard so the user can try again
                finishTriggered.set(false)
            }
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
        OnboardingPage(Icons.Default.Build, "Manage projects", "Edit files and keep each project organized."),
        OnboardingPage(Icons.Default.RocketLaunch, "Deploy faster", "Run checks and ship updates with confidence."),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { pageIndex ->
                val page = pages[pageIndex]
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp))
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(36.dp),
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = page.title,
                            style = MaterialTheme.typography.headlineMedium,
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = page.description,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "botblade://workspace",
                            color = MaterialTheme.colorScheme.primary,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                repeat(pages.size) { index ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(if (isSelected) 10.dp else 8.dp)
                            .background(
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                shape = CircleShape,
                            ),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(0.5f),
            ) {
                Text(
                    if (isLastPage) "Finish" else "Next",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }
    }
}

