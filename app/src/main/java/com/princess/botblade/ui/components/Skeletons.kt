package com.princess.botblade.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun SkeletonBox(width: Dp, height: Dp, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha = transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "alpha",
    )
    Box(
        modifier = modifier
            .width(width)
            .height(height)
            .alpha(alpha.value)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp)),
    )
}

@Composable
fun DashboardCardSkeleton() = Card(Modifier.fillMaxWidth().padding(16.dp)) { SkeletonBox(280.dp, 120.dp, Modifier.padding(16.dp)) }

@Composable
fun ProjectListItemSkeleton() = Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Column(Modifier.padding(16.dp)) {
        SkeletonBox(180.dp, 24.dp)
        Spacer(Modifier.height(10.dp))
        SkeletonBox(90.dp, 22.dp)
        Spacer(Modifier.height(10.dp))
        SkeletonBox(130.dp, 14.dp)
    }
}

@Composable
fun DeploymentListItemSkeleton() = Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
    Row(Modifier.padding(16.dp)) {
        SkeletonBox(42.dp, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column { SkeletonBox(170.dp, 20.dp); Spacer(Modifier.height(8.dp)); SkeletonBox(110.dp, 14.dp) }
    }
}
