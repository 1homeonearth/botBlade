package com.princess.botblade.ui.importforge

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.WorkstationCard
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class ImportForgeFragment : Fragment() {
    private val viewModel: ImportForgeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent { ImportForgeRoute(viewModel = viewModel, openExternal = { url -> openExternalUrl(requireContext(), url) }) }
    }
}

@Composable
private fun ImportForgeRoute(viewModel: ImportForgeViewModel, openExternal: (String) -> Unit) {
    fun materializedWorkspacePath(prefix: String, source: String): String {
        val suffix = source.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "workspace" }
        return "$prefix/$suffix"
    }

    val state by viewModel.uiState.collectAsState()
    var query by remember { mutableStateOf("discord.js slash commands") }
    var gitUrl by remember { mutableStateOf(DiscoveryRepositories.first().url) }
    var selectedRepo by remember { mutableStateOf(DiscoveryRepositories.first()) }
    var message by remember { mutableStateOf("Pick a repository card or paste a Git URL. BotBlade will import the selected source into a managed workspace.") }

    fun searchUrl(): String = "https://github.com/search?type=repositories&q=${query.urlEncoded()}"

    fun importSelected(url: String = gitUrl) {
        val source = normalizeGitHubSource(url)
        if (source == null) {
            message = "Paste a GitHub repository URL or owner/repo before importing."
            return
        }
        gitUrl = source
        viewModel.startImport(sourceType = "git", source = source, workspacePath = materializedWorkspacePath("imports/git", source))
        message = "Import started for $source."
    }

    val visibleRepositories = remember(query, selectedRepo) {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) DiscoveryRepositories else DiscoveryRepositories.filter { repo ->
            repo.ownerRepo.lowercase().contains(normalized) ||
                repo.description.lowercase().contains(normalized) ||
                repo.tags.any { it.lowercase().contains(normalized) } ||
                repo.lane.lowercase().contains(normalized)
        }.ifEmpty { DiscoveryRepositories.filter { it.lane == selectedRepo.lane } }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            WorkstationCard(accent = BotBladeTokens.HotPink) {
                Text("Import Forge", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Text("Move through Source, Detect, Configure, then Build, Run & Script preview without executing imported code during analysis.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 14.dp)) {
                    items(ImportForgeStep.entries.toList()) { step ->
                        StatusChip(step.title, if (step == state.step) StatusTone.Success else StatusTone.Neutral)
                    }
                }
            }
        }

        item { SectionTitle("Source") }
        item {
            WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                Text("Choose an import source", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Git URLs, ZIP archives, SAF folders, workflow JSON, and first-party templates share the same preview-first wizard.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 12.dp)) {
                    state.sourceOptions.forEach { option ->
                        SourceOptionCard(
                            option = option,
                            selected = option.type == state.selectedSourceType,
                            onSelect = {
                                viewModel.selectSource(option.type)
                                message = "Selected ${option.title}. Continue through detection and configuration before saving a profile."
                            },
                        )
                    }
                }
            }
        }

        item {
            WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                Text("Search workbench", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Search by framework, bot type, language, or paste a GitHub repository URL directly.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text("Search GitHub repositories") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    items(DiscoveryLanes) { lane ->
                        OutlinedButton(onClick = {
                            query = lane.query
                            selectedRepo = DiscoveryRepositories.firstOrNull { it.lane == lane.title } ?: selectedRepo
                            gitUrl = selectedRepo.url
                            message = "${lane.title}: ${lane.detail}"
                        }) { Text(lane.title) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    BladeButton("Search GitHub", icon = Icons.Default.Search, onClick = { openExternal(searchUrl()) }, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { openExternal(gitUrl) }, modifier = Modifier.weight(1f)) { Text("Open selected") }
                }
            }
        }

        item {
            WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                Text("Git URL source", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(message, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                OutlinedTextField(
                    value = gitUrl,
                    onValueChange = { gitUrl = it },
                    label = { Text("Git repository URL or owner/repo") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                    BladeButton("Import repository", icon = Icons.Default.Code, onClick = { importSelected() }, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = {
                        gitUrl = selectedRepo.url
                        message = "Selected ${selectedRepo.ownerRepo}."
                    }, modifier = Modifier.weight(1f)) { Text("Use card") }
                }
            }
        }

        item { SectionTitle("Recommended repositories") }
        items(visibleRepositories, key = { it.ownerRepo }) { repo ->
            RepositoryDiscoveryCard(
                repo = repo,
                selected = repo.ownerRepo == selectedRepo.ownerRepo,
                onSelect = {
                    selectedRepo = repo
                    gitUrl = repo.url
                    message = "Selected ${repo.ownerRepo}. Review the repository, then import when ready."
                },
                onImport = {
                    selectedRepo = repo
                    gitUrl = repo.url
                    importSelected(repo.url)
                },
                onOpen = { openExternal(repo.url) },
            )
        }

        item { SectionTitle("Detect") }
        item {
            WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                Text("Recommended Blade Pack", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(state.recommendedBladePack.name, color = Color.White, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                Text("Confidence score: ${state.recommendedBladePack.confidenceScore}%", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 4.dp))
                Text("Matched evidence", color = BotBladeTokens.BabyBlue, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                state.recommendedBladePack.matchedEvidence.forEach { evidence ->
                    Text("• $evidence", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 4.dp))
                }
                OutlinedButton(onClick = { viewModel.continueToConfigure() }, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("Configure") }
            }
        }

        item { SectionTitle("Configure") }
        item {
            WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                Text("Secrets", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Only secret references and configured flags are shown here. Secret values are never displayed.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                SecretGroup("Required", state.requiredSecrets)
                SecretGroup("Optional", state.optionalSecrets)
                Text("Command plan", color = BotBladeTokens.BabyBlue, modifier = Modifier.padding(top = 14.dp), fontWeight = FontWeight.Bold)
                state.commandPlan.forEach { group ->
                    Text(group.source, color = Color.White, modifier = Modifier.padding(top = 8.dp), fontWeight = FontWeight.Bold)
                    group.commands.forEach { command -> Text("• $command", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 3.dp)) }
                }
                BladeButton("Preview script profile", icon = Icons.Default.Code, onClick = { viewModel.previewScriptProfile() }, modifier = Modifier.fillMaxWidth().padding(top = 14.dp))
            }
        }

        item { SectionTitle("Repair cards") }
        items(state.repairCards) { card ->
            WorkstationCard(accent = BotBladeTokens.Danger) {
                Text(card.title, color = BotBladeTokens.Danger, fontWeight = FontWeight.Bold)
                Text(card.evidence, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                Text(card.safeAction, color = Color.White, modifier = Modifier.padding(top = 6.dp))
            }
        }

        item { SectionTitle("Build, Run & Script preview") }
        item {
            WorkstationCard(accent = BotBladeTokens.HotPink) {
                Text("Script-profile preview grouped by source", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Review build, validation, and runtime-profile commands before saving. This phase previews profiles and does not execute commands.", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
                state.scriptProfilePreview.forEach { group ->
                    Text(group.source, color = Color.White, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
                    group.profiles.forEach { profile -> Text("• $profile", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 3.dp)) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
                    BladeButton("Preview script profile", icon = Icons.Default.Code, onClick = { viewModel.previewScriptProfile() }, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { viewModel.saveProfile() }, modifier = Modifier.weight(1f)) { Text("Save profile") }
                }
            }
        }

        item { SectionTitle("Import timeline") }
        if (state.timelineEvents.isEmpty()) {
            item {
                WorkstationCard(accent = BotBladeTokens.Stroke) {
                    Text("No import started yet", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    Text("Choose a source to begin detection.", color = BotBladeTokens.Muted)
                }
            }
        } else {
            items(state.timelineEvents) { event ->
                Surface(
                    color = BotBladeTokens.Panel,
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BotBladeTokens.Stroke),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(event, color = BotBladeTokens.Muted, modifier = Modifier.padding(14.dp))
                }
            }
        }
        state.blockedPolicyMessage?.let { policy ->
            item {
                WorkstationCard(accent = BotBladeTokens.Danger) {
                    Text("Import policy block", color = BotBladeTokens.Danger, fontWeight = FontWeight.Bold)
                    Text(policy, color = BotBladeTokens.Muted)
                }
            }
        }
    }
}


@Composable
private fun SourceOptionCard(
    option: ImportForgeSourceOption,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        color = BotBladeTokens.Panel,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, if (selected) BotBladeTokens.HotPink else BotBladeTokens.Stroke),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                StatusChip(option.title, if (selected) StatusTone.Success else StatusTone.Info)
                if (!option.enabled) StatusChip("SAF disabled", StatusTone.Warning)
            }
            Text(option.detail, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 8.dp))
            OutlinedButton(onClick = onSelect, enabled = option.enabled, modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
                Text(if (selected) "Selected source" else "Select source")
            }
        }
    }
}

@Composable
private fun SecretGroup(title: String, secrets: List<ImportForgeSecretPreview>) {
    Text(title, color = BotBladeTokens.BabyBlue, modifier = Modifier.padding(top = 12.dp), fontWeight = FontWeight.Bold)
    if (secrets.isEmpty()) {
        Text("None", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 4.dp))
    } else {
        secrets.forEach { secret ->
            val flag = if (secret.configured) "configured=true" else "configured=false"
            Text("• ${secret.name} ($flag)", color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun RepositoryDiscoveryCard(
    repo: DiscoveryRepository,
    selected: Boolean,
    onSelect: () -> Unit,
    onImport: () -> Unit,
    onOpen: () -> Unit,
) {
    WorkstationCard(accent = if (selected) BotBladeTokens.HotPink else repo.accent) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            StatusChip(repo.lane, StatusTone.Info)
            StatusChip(repo.language, StatusTone.Neutral)
        }
        Text(repo.ownerRepo, color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 12.dp))
        Text(repo.description, color = BotBladeTokens.Muted, modifier = Modifier.padding(top = 6.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
            items(repo.tags) { tag -> StatusChip(tag, StatusTone.Neutral) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 14.dp)) {
            BladeButton("Import", icon = Icons.Default.Star, onClick = onImport, modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onSelect, modifier = Modifier.weight(1f)) { Text(if (selected) "Selected" else "Select") }
        }
        OutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            androidx.compose.material3.Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Open on GitHub")
        }
    }
}

private fun normalizeGitHubSource(value: String): String? {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return null
    if (trimmed.startsWith("https://github.com/") || trimmed.startsWith("git@github.com:")) return trimmed
    val ownerRepo = trimmed.trim('/').split('/')
    return if (ownerRepo.size == 2 && ownerRepo.all { it.isNotBlank() }) "https://github.com/${ownerRepo[0]}/${ownerRepo[1]}" else null
}

private fun String.urlEncoded(): String = URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

private fun openExternalUrl(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

private data class DiscoveryLane(val title: String, val detail: String, val query: String)

private data class DiscoveryRepository(
    val ownerRepo: String,
    val description: String,
    val url: String,
    val lane: String,
    val language: String,
    val tags: List<String>,
    val accent: Color,
)

private val DiscoveryLanes = listOf(
    DiscoveryLane("Discord", "Slash commands, moderation, music, tickets, and bot starters.", "discord.js slash commands bot language:TypeScript"),
    DiscoveryLane("Telegram", "Telegraf and Telegram automation repositories.", "telegraf telegram bot language:TypeScript"),
    DiscoveryLane("Slack", "Slack Bolt apps, event handlers, and workflow bots.", "slack bolt app language:TypeScript"),
    DiscoveryLane("Workflow", "n8n workflow JSON and automation templates.", "n8n workflow json"),
    DiscoveryLane("Worker", "Webhook workers and tiny HTTP services.", "webhook worker fastify language:TypeScript"),
    DiscoveryLane("Python", "Python bots with requirements or pyproject metadata.", "python automation bot requirements.txt"),
)

private val DiscoveryRepositories = listOf(
    DiscoveryRepository(
        ownerRepo = "discordjs/guide",
        description = "Official Discord.js guide repository. Strong reference material for slash commands, command handling, and project structure.",
        url = "https://github.com/discordjs/guide",
        lane = "Discord",
        language = "TypeScript",
        tags = listOf("discord.js", "slash commands", "reference"),
        accent = BotBladeTokens.BabyBlue,
    ),
    DiscoveryRepository(
        ownerRepo = "Sayrix/Ticket-Bot",
        description = "Discord ticket bot with buttons, slash commands, select menus, modals, and transcript workflows.",
        url = "https://github.com/Sayrix/Ticket-Bot",
        lane = "Discord",
        language = "JavaScript",
        tags = listOf("tickets", "buttons", "moderation"),
        accent = BotBladeTokens.HotPink,
    ),
    DiscoveryRepository(
        ownerRepo = "telegraf/telegraf",
        description = "Telegram bot framework repository. Useful for BotBlade detector and template checks around Telegraf bots.",
        url = "https://github.com/telegraf/telegraf",
        lane = "Telegram",
        language = "TypeScript",
        tags = listOf("telegram", "framework", "bot api"),
        accent = BotBladeTokens.BabyBlue,
    ),
    DiscoveryRepository(
        ownerRepo = "slackapi/bolt-js",
        description = "Slack Bolt for JavaScript. Good reference for Slack app manifests, events, commands, and OAuth-shaped setup.",
        url = "https://github.com/slackapi/bolt-js",
        lane = "Slack",
        language = "TypeScript",
        tags = listOf("slack", "events", "oauth"),
        accent = BotBladeTokens.GlitterGold,
    ),
    DiscoveryRepository(
        ownerRepo = "n8n-io/n8n",
        description = "Workflow automation platform repository. Use as a reference source for workflow JSON and credential metadata import patterns.",
        url = "https://github.com/n8n-io/n8n",
        lane = "Workflow",
        language = "TypeScript",
        tags = listOf("workflow", "json", "automation"),
        accent = BotBladeTokens.BabyBlue,
    ),
    DiscoveryRepository(
        ownerRepo = "fastapi/full-stack-fastapi-template",
        description = "Python web automation template with modern project structure. Useful for generic Python detector and deployment testing.",
        url = "https://github.com/fastapi/full-stack-fastapi-template",
        lane = "Python",
        language = "Python",
        tags = listOf("python", "api", "template"),
        accent = BotBladeTokens.HotPink,
    ),
)
