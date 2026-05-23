// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.components  // line 7: executes this statement as part of this file's behavior

import androidx.compose.animation.core.FastOutSlowInEasing  // line 9: executes this statement as part of this file's behavior
import androidx.compose.animation.core.RepeatMode  // line 10: executes this statement as part of this file's behavior
import androidx.compose.animation.core.animateFloat  // line 11: executes this statement as part of this file's behavior
import androidx.compose.animation.core.infiniteRepeatable  // line 12: executes this statement as part of this file's behavior
import androidx.compose.animation.core.rememberInfiniteTransition  // line 13: executes this statement as part of this file's behavior
import androidx.compose.animation.core.tween  // line 14: executes this statement as part of this file's behavior
import androidx.compose.foundation.background  // line 15: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Box  // line 16: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Column  // line 17: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Row  // line 18: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Spacer  // line 19: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxWidth  // line 20: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.height  // line 21: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.padding  // line 22: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.size  // line 23: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.width  // line 24: executes this statement as part of this file's behavior
import androidx.compose.foundation.shape.RoundedCornerShape  // line 25: executes this statement as part of this file's behavior
import androidx.compose.material3.Card  // line 26: executes this statement as part of this file's behavior
import androidx.compose.material3.MaterialTheme  // line 27: executes this statement as part of this file's behavior
import androidx.compose.runtime.Composable  // line 28: executes this statement as part of this file's behavior
import androidx.compose.ui.Modifier  // line 29: executes this statement as part of this file's behavior
import androidx.compose.ui.draw.alpha  // line 30: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.Dp  // line 31: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.dp  // line 32: executes this statement as part of this file's behavior

@Composable  // line 34: executes this statement as part of this file's behavior
fun SkeletonBox(width: Dp, height: Dp, modifier: Modifier = Modifier) {  // line 35: executes this statement as part of this file's behavior
    val transition = rememberInfiniteTransition(label = "skeleton")  // line 36: executes this statement as part of this file's behavior
    val alpha = transition.animateFloat(  // line 37: executes this statement as part of this file's behavior
        initialValue = 0.35f,  // line 38: executes this statement as part of this file's behavior
        targetValue = 0.9f,  // line 39: executes this statement as part of this file's behavior
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),  // line 40: executes this statement as part of this file's behavior
        label = "alpha",  // line 41: executes this statement as part of this file's behavior
    )  // line 42: executes this statement as part of this file's behavior
    Box(  // line 43: executes this statement as part of this file's behavior
        modifier = modifier  // line 44: executes this statement as part of this file's behavior
            .width(width)  // line 45: executes this statement as part of this file's behavior
            .height(height)  // line 46: executes this statement as part of this file's behavior
            .alpha(alpha.value)  // line 47: executes this statement as part of this file's behavior
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),  // line 48: executes this statement as part of this file's behavior
    )  // line 49: executes this statement as part of this file's behavior
}  // line 50: executes this statement as part of this file's behavior

@Composable  // line 52: executes this statement as part of this file's behavior
fun DashboardCardSkeleton() = Card(Modifier.fillMaxWidth().padding(16.dp)) { SkeletonBox(280.dp, 120.dp, Modifier.padding(16.dp)) }  // line 53: executes this statement as part of this file's behavior

@Composable  // line 55: executes this statement as part of this file's behavior
fun ProjectListItemSkeleton() = Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {  // line 56: executes this statement as part of this file's behavior
    Column(Modifier.padding(16.dp)) {  // line 57: executes this statement as part of this file's behavior
        SkeletonBox(180.dp, 24.dp)  // line 58: executes this statement as part of this file's behavior
        Spacer(Modifier.height(10.dp))  // line 59: executes this statement as part of this file's behavior
        SkeletonBox(90.dp, 22.dp)  // line 60: executes this statement as part of this file's behavior
        Spacer(Modifier.height(10.dp))  // line 61: executes this statement as part of this file's behavior
        SkeletonBox(130.dp, 14.dp)  // line 62: executes this statement as part of this file's behavior
    }  // line 63: executes this statement as part of this file's behavior
}  // line 64: executes this statement as part of this file's behavior

@Composable  // line 66: executes this statement as part of this file's behavior
fun DeploymentListItemSkeleton() = Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {  // line 67: executes this statement as part of this file's behavior
    Row(Modifier.padding(16.dp)) {  // line 68: executes this statement as part of this file's behavior
        SkeletonBox(42.dp, 42.dp)  // line 69: executes this statement as part of this file's behavior
        Spacer(Modifier.width(12.dp))  // line 70: executes this statement as part of this file's behavior
        Column { SkeletonBox(170.dp, 20.dp); Spacer(Modifier.height(8.dp)); SkeletonBox(110.dp, 14.dp) }  // line 71: executes this statement as part of this file's behavior
    }  // line 72: executes this statement as part of this file's behavior
}  // line 73: executes this statement as part of this file's behavior
