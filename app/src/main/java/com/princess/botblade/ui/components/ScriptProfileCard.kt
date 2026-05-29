package com.princess.botblade.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.princess.botblade.data.model.ScriptProfileSummary
import com.princess.botblade.util.SecretRedactor

@Composable
fun ScriptProfileCard(
    profile: ScriptProfileSummary,
    modifier: Modifier = Modifier,
    onDetailsClick: ((ScriptProfileSummary) -> Unit)? = null,
    onSaveClick: ((ScriptProfileSummary) -> Unit)? = null,
) {
    WorkstationCard(
        accent = if (profile.requiresConfirmation) BotBladeTokens.GlitterGold else BotBladeTokens.BabyBlue,
        modifier = modifier,
    ) {
        Text(
            text = profile.name,
            color = BotBladeTokens.BabyBlue,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!profile.description.isNullOrBlank()) {
            Text(
                text = profile.description,
                color = BotBladeTokens.Muted,
                modifier = Modifier.padding(top = 6.dp),
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            item { StatusChip("Runtime · ${profile.runtime.ifBlank { "unknown" }}", StatusTone.Info) }
            item { StatusChip("Source · ${profile.source.ifBlank { "unknown" }}", StatusTone.Neutral) }
            item {
                StatusChip(
                    label = if (profile.requiresConfirmation) "Confirmation required" else "No confirmation gate",
                    tone = if (profile.requiresConfirmation) StatusTone.Warning else StatusTone.Success,
                )
            }
            item {
                StatusChip(
                    "Secrets · ${profile.secretRefs.size} masked",
                    if (profile.secretRefs.isEmpty()) StatusTone.Neutral else StatusTone.Warning,
                )
            }
        }

        ScriptProfileField(label = "Command", value = profile.command.toCommandPreview())
        ScriptProfileField(label = "Working directory", value = profile.workingDirectory.ifBlank { "." })

        Column(modifier = Modifier.padding(top = 12.dp)) {
            Text("Tags", color = BotBladeTokens.HotPink, fontWeight = FontWeight.Bold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                if (profile.tags.isEmpty()) {
                    item { StatusChip("No tags", StatusTone.Neutral) }
                } else {
                    items(profile.tags) { tag ->
                        StatusChip(tag, StatusTone.Neutral)
                    }
                }
            }
        }

        if (onDetailsClick != null || onSaveClick != null) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            ) {
                if (onDetailsClick != null) {
                    AssistChip(onClick = { onDetailsClick(profile) }, label = { Text("Details") })
                }
                if (onSaveClick != null) {
                    AssistChip(onClick = { onSaveClick(profile) }, label = { Text("Save") })
                }
            }
        }
    }
}

@Composable
private fun ScriptProfileField(label: String, value: String) {
    Column(modifier = Modifier.padding(top = 12.dp)) {
        Text(label, color = BotBladeTokens.HotPink, fontWeight = FontWeight.Bold)
        Text(
            text = value,
            color = BotBladeTokens.Muted,
            modifier = Modifier.padding(top = 4.dp),
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private fun List<String>.toCommandPreview(): String {
    if (isEmpty()) return "No command configured"
    return SecretRedactor.redact(joinToString(" ") { token ->
        if (token.any { it.isWhitespace() }) "\"${token.replace("\"", "\\\"")}\"" else token
    })
}
