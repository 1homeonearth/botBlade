package com.princess.botblade.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

object BotBladeTokens {
    val Black = Color(0xFF05060A)
    val Ink = Color(0xFF090B12)
    val Panel = Color(0xFF101522)
    val RaisedPanel = Color(0xFF151D2E)
    val Stroke = Color(0xFF2F405F)
    val BabyBlue = Color(0xFF8FD8FF)
    val HotPink = Color(0xFFFF3EA5)
    val GlitterGold = Color(0xFFFFD166)
    val Success = Color(0xFF7CFFB2)
    val Danger = Color(0xFFFF6B8A)
    val Muted = Color(0xFFAAB8CC)
}

enum class StatusTone { Neutral, Info, Success, Warning, Danger }

@Composable
fun WorkstationCard(
    modifier: Modifier = Modifier,
    accent: Color = BotBladeTokens.BabyBlue,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        color = BotBladeTokens.Panel,
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, BotBladeTokens.Stroke),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row {
            Surface(color = accent, modifier = Modifier.fillMaxWidth(0.02f)) {}
            Column(modifier = Modifier.padding(22.dp), content = content)
        }
    }
}

@Composable
fun StatusChip(
    label: String,
    tone: StatusTone = StatusTone.Neutral,
    modifier: Modifier = Modifier,
) {
    val contentColor = when (tone) {
        StatusTone.Neutral -> BotBladeTokens.Muted
        StatusTone.Info -> BotBladeTokens.BabyBlue
        StatusTone.Success -> BotBladeTokens.Success
        StatusTone.Warning -> BotBladeTokens.GlitterGold
        StatusTone.Danger -> BotBladeTokens.Danger
    }
    Surface(
        color = BotBladeTokens.Ink,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BotBladeTokens.Stroke),
        modifier = modifier,
    ) {
        Text(
            text = label,
            color = contentColor,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
fun BladeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = BotBladeTokens.HotPink,
            contentColor = BotBladeTokens.Black,
        ),
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(icon, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
        }
        Text(text, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun BladeOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit = { Text(text) },
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier,
        border = BorderStroke(1.dp, BotBladeTokens.BabyBlue),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = BotBladeTokens.HotPink,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier,
    )
}

@Composable
fun FlowLane(
    title: String,
    detail: String,
    accent: Color,
    modifier: Modifier = Modifier,
    buttons: @Composable RowScope.() -> Unit,
) {
    WorkstationCard(accent = accent, modifier = modifier) {
        Text(title, color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
        Text(detail, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
        ) {
            buttons()
        }
    }
}
