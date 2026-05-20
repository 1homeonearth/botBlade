package com.princess.botblade.ui.onboarding

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.princess.botblade.MainActivity
import com.princess.botblade.ui.theme.BotBladeTheme
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore("onboarding")

class OnboardingFragment : Fragment() {
    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme { OnboardingPager { finishOnboardingFlow() } } }
    }

    private fun finishOnboardingFlow() {
        val appContext = context?.applicationContext ?: return
        viewLifecycleOwner.lifecycleScope.launch {
            appContext.dataStore.edit { prefs -> prefs[booleanPreferencesKey("onboarding_complete")] = true }
            (activity as? MainActivity)?.finishOnboarding()
        }
    }
}

private data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)

@Composable
private fun OnboardingPager(onFinish: () -> Unit) {
    val pages = listOf(
        OnboardingPage(Icons.Default.Dashboard, "Monitor bots", "Watch builds, logs, and runtime health from one workspace."),
        OnboardingPage(Icons.Default.Build, "Manage projects", "Edit project files with clear structure and repeatable workflow tools."),
        OnboardingPage(Icons.Default.RocketLaunch, "Deploy faster", "Ship updates confidently with guided checks and quick actions."),
    )
    var index by remember { mutableStateOf(0) }
    val page = pages[index]

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
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
            Spacer(modifier = Modifier.height(28.dp))
            Button(
                onClick = { if (index == pages.lastIndex) onFinish() else index++ },
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                modifier = Modifier.fillMaxWidth(0.5f),
            ) {
                Text(if (index == pages.lastIndex) "Finish" else "Next", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
