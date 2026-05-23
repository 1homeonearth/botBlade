// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.onboarding  // line 7: executes this statement as part of this file's behavior

import android.Manifest  // line 9: executes this statement as part of this file's behavior
import android.content.pm.PackageManager  // line 10: executes this statement as part of this file's behavior
import android.os.Build  // line 11: executes this statement as part of this file's behavior
import androidx.activity.compose.rememberLauncherForActivityResult  // line 12: executes this statement as part of this file's behavior
import androidx.activity.result.contract.ActivityResultContracts  // line 13: executes this statement as part of this file's behavior
import androidx.compose.foundation.ExperimentalFoundationApi  // line 14: executes this statement as part of this file's behavior
import androidx.compose.foundation.background  // line 15: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Arrangement  // line 16: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Box  // line 17: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Column  // line 18: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Row  // line 19: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Spacer  // line 20: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxSize  // line 21: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxWidth  // line 22: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.widthIn  // line 23: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.height  // line 24: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.padding  // line 25: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.size  // line 26: executes this statement as part of this file's behavior
import androidx.compose.foundation.pager.HorizontalPager  // line 27: executes this statement as part of this file's behavior
import androidx.compose.foundation.pager.rememberPagerState  // line 28: executes this statement as part of this file's behavior
import androidx.compose.foundation.shape.CircleShape  // line 29: executes this statement as part of this file's behavior
import androidx.compose.foundation.shape.RoundedCornerShape  // line 30: executes this statement as part of this file's behavior
import androidx.compose.material.icons.Icons  // line 31: executes this statement as part of this file's behavior
import androidx.compose.material.icons.filled.Build  // line 32: executes this statement as part of this file's behavior
import androidx.compose.material.icons.filled.Dashboard  // line 33: executes this statement as part of this file's behavior
import androidx.compose.material.icons.filled.Notifications  // line 34: executes this statement as part of this file's behavior
import androidx.compose.material.icons.filled.RocketLaunch  // line 35: executes this statement as part of this file's behavior
import androidx.compose.material3.Button  // line 36: executes this statement as part of this file's behavior
import androidx.compose.material3.Icon  // line 37: executes this statement as part of this file's behavior
import androidx.compose.material3.MaterialTheme  // line 38: executes this statement as part of this file's behavior
import androidx.compose.material3.Surface  // line 39: executes this statement as part of this file's behavior
import androidx.compose.material3.Text  // line 40: executes this statement as part of this file's behavior
import androidx.compose.runtime.Composable  // line 41: executes this statement as part of this file's behavior
import androidx.compose.runtime.rememberCoroutineScope  // line 42: executes this statement as part of this file's behavior
import androidx.compose.ui.Alignment  // line 43: executes this statement as part of this file's behavior
import androidx.compose.ui.Modifier  // line 44: executes this statement as part of this file's behavior
import androidx.compose.ui.graphics.vector.ImageVector  // line 45: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.ComposeView  // line 46: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.LocalContext  // line 47: executes this statement as part of this file's behavior
import androidx.compose.ui.text.style.TextAlign  // line 48: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.dp  // line 49: executes this statement as part of this file's behavior
import androidx.core.content.ContextCompat  // line 50: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 51: executes this statement as part of this file's behavior
import com.princess.botblade.MainActivity  // line 52: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.BotBladeTheme  // line 53: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 54: executes this statement as part of this file's behavior
import java.util.concurrent.atomic.AtomicBoolean  // line 55: executes this statement as part of this file's behavior

class OnboardingFragment : Fragment() {  // line 57: executes this statement as part of this file's behavior
    private val finishTriggered = AtomicBoolean(false)  // line 58: executes this statement as part of this file's behavior
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: android.os.Bundle?) = ComposeView(requireContext()).apply {  // line 59: executes this statement as part of this file's behavior
        setContent { BotBladeTheme { OnboardingPager { finishOnboardingFlow() } } }  // line 60: executes this statement as part of this file's behavior
    }  // line 61: executes this statement as part of this file's behavior

    private fun finishOnboardingFlow() {  // line 63: executes this statement as part of this file's behavior
        if (!isAdded || !finishTriggered.compareAndSet(false, true)) return  // line 64: executes this statement as part of this file's behavior
        (activity as? MainActivity)?.finishOnboarding()  // line 65: executes this statement as part of this file's behavior
    }  // line 66: executes this statement as part of this file's behavior
}  // line 67: executes this statement as part of this file's behavior

private data class OnboardingPage(val icon: ImageVector, val title: String, val description: String)  // line 69: executes this statement as part of this file's behavior

@OptIn(ExperimentalFoundationApi::class)  // line 71: executes this statement as part of this file's behavior
@Composable  // line 72: executes this statement as part of this file's behavior
private fun OnboardingPager(onFinish: () -> Unit) {  // line 73: executes this statement as part of this file's behavior
    val pages = listOf(  // line 74: executes this statement as part of this file's behavior
        OnboardingPage(Icons.Default.Build, "Import or build bots", "Bring in a bot project or start a new one."),  // line 75: executes this statement as part of this file's behavior
        OnboardingPage(Icons.Default.Dashboard, "Monitor bots", "Watch logs, builds, and runtime health."),  // line 76: executes this statement as part of this file's behavior
        OnboardingPage(Icons.Default.Notifications, "Stay informed", "Get build status and deployment updates."),  // line 77: executes this statement as part of this file's behavior
        OnboardingPage(Icons.Default.RocketLaunch, "Deploy faster", "Run checks and ship updates with confidence."),  // line 78: executes this statement as part of this file's behavior
    )  // line 79: executes this statement as part of this file's behavior
    val totalPages = pages.size + 1  // line 80: executes this statement as part of this file's behavior
    val pagerState = rememberPagerState(pageCount = { totalPages })  // line 81: executes this statement as part of this file's behavior
    val scope = rememberCoroutineScope()  // line 82: executes this statement as part of this file's behavior
    val context = LocalContext.current  // line 83: executes this statement as part of this file's behavior
    val isPermissionPage = pagerState.currentPage == pages.size  // line 84: executes this statement as part of this file's behavior
    val isLastPage = isPermissionPage  // line 85: executes this statement as part of this file's behavior
    val requestPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }  // line 86: executes this statement as part of this file's behavior

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {  // line 88: executes this statement as part of this file's behavior
        Column(  // line 89: executes this statement as part of this file's behavior
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 24.dp),  // line 90: executes this statement as part of this file's behavior
            horizontalAlignment = Alignment.CenterHorizontally,  // line 91: executes this statement as part of this file's behavior
            verticalArrangement = Arrangement.Center,  // line 92: executes this statement as part of this file's behavior
        ) {  // line 93: executes this statement as part of this file's behavior
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxWidth().widthIn(max = 640.dp)) { pageIndex ->  // line 94: executes this statement as part of this file's behavior
                val isPermissionCard = pageIndex == pages.size  // line 95: executes this statement as part of this file's behavior
                Box(  // line 96: executes this statement as part of this file's behavior
                    modifier = Modifier.fillMaxWidth().background(if (isPermissionCard) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(28.dp)).padding(horizontal = 24.dp, vertical = 28.dp),  // line 97: executes this statement as part of this file's behavior
                    contentAlignment = Alignment.Center,  // line 98: executes this statement as part of this file's behavior
                ) {  // line 99: executes this statement as part of this file's behavior
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {  // line 100: executes this statement as part of this file's behavior
                        if (!isPermissionCard) {  // line 101: executes this statement as part of this file's behavior
                            val page = pages[pageIndex]  // line 102: executes this statement as part of this file's behavior
                            Icon(imageVector = page.icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))  // line 103: executes this statement as part of this file's behavior
                            Text(text = page.title, style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurface)  // line 104: executes this statement as part of this file's behavior
                            Text(text = page.description, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)  // line 105: executes this statement as part of this file's behavior
                        } else {  // line 106: executes this statement as part of this file's behavior
                            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))  // line 107: executes this statement as part of this file's behavior
                            Text(text = "Notifications (optional)", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)  // line 108: executes this statement as part of this file's behavior
                            Text(text = "Enable notifications to get build and deployment updates. You can continue without granting permission.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onPrimaryContainer)  // line 109: executes this statement as part of this file's behavior
                            if (Build.VERSION.SDK_INT >= 33) {  // line 110: executes this statement as part of this file's behavior
                                Button(onClick = {  // line 111: executes this statement as part of this file's behavior
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {  // line 112: executes this statement as part of this file's behavior
                                        requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)  // line 113: executes this statement as part of this file's behavior
                                    }  // line 114: executes this statement as part of this file's behavior
                                }) { Text("Grant notification permission") }  // line 115: executes this statement as part of this file's behavior
                            }  // line 116: executes this statement as part of this file's behavior
                        }  // line 117: executes this statement as part of this file's behavior
                    }  // line 118: executes this statement as part of this file's behavior
                }  // line 119: executes this statement as part of this file's behavior
            }  // line 120: executes this statement as part of this file's behavior
            Spacer(modifier = Modifier.height(20.dp))  // line 121: executes this statement as part of this file's behavior
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {  // line 122: executes this statement as part of this file's behavior
                repeat(pages.size) { index ->  // line 123: executes this statement as part of this file's behavior
                    val isSelected = pagerState.currentPage == index  // line 124: executes this statement as part of this file's behavior
                    Box(modifier = Modifier.padding(horizontal = 4.dp).size(if (isSelected) 10.dp else 8.dp).background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f), CircleShape))  // line 125: executes this statement as part of this file's behavior
                }  // line 126: executes this statement as part of this file's behavior
            }  // line 127: executes this statement as part of this file's behavior
            Spacer(modifier = Modifier.height(16.dp))  // line 128: executes this statement as part of this file's behavior
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {  // line 129: executes this statement as part of this file's behavior
                Button(  // line 130: executes this statement as part of this file's behavior
                    onClick = { if (isLastPage) onFinish() else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) } },  // line 131: executes this statement as part of this file's behavior
                ) {  // line 132: executes this statement as part of this file's behavior
                    Text(if (isLastPage) "Finish" else "Continue")  // line 133: executes this statement as part of this file's behavior
                }  // line 134: executes this statement as part of this file's behavior
            }  // line 135: executes this statement as part of this file's behavior
        }  // line 136: executes this statement as part of this file's behavior
    }  // line 137: executes this statement as part of this file's behavior
}  // line 138: executes this statement as part of this file's behavior
