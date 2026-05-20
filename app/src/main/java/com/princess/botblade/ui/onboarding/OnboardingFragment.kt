package com.princess.botblade.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.MainActivity
import com.princess.botblade.ui.theme.BotBladeTheme
import kotlinx.coroutines.launch

private val android.content.Context.dataStore by preferencesDataStore("onboarding")

class OnboardingFragment : Fragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: android.os.Bundle?) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme { OnboardingPager { finish() } } }
    }

    private fun finish() { viewLifecycleOwner.lifecycleScope.launch { requireContext().dataStore.edit { it[booleanPreferencesKey("onboarding_complete")] = true }; (activity as? MainActivity)?.finishOnboarding() } }
}

@Composable
private fun OnboardingPager(onFinish: () -> Unit) {
    val pages = listOf(Icons.Default.Dashboard to "Monitor bots", Icons.Default.Build to "Manage projects", Icons.Default.RocketLaunch to "Deploy faster")
    var index by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(pages[index].first, contentDescription = null)
        Text(pages[index].second)
        Text("Build and control your bot operations")
        Text("Everything in one mobile workspace")
        Button(onClick = { if (index == pages.lastIndex) onFinish() else index++ }, modifier = Modifier.padding(top = 20.dp)) { Text(if (index == pages.lastIndex) "Finish" else "Next") }
    }
}
