package com.princess.botblade.ui.importforge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princess.botblade.R

class ImportForgeFragment : Fragment() {
    private val viewModel: ImportForgeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent { ImportForgeRoute(viewModel, ::openUrl) }
    }

    private fun openUrl(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
    }
}

@Composable
private fun ImportForgeRoute(viewModel: ImportForgeViewModel, openUrl: (String) -> Unit) {
    fun materializedWorkspacePath(prefix: String, source: String): String {
        val suffix = source.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "workspace" }
        return "$prefix/$suffix"
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var gitUrl by remember { mutableStateOf("") }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.import_forge_title), style = MaterialTheme.typography.headlineMedium) }
        item {
            Card { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.import_forge_source_picker))
                OutlinedTextField(value = gitUrl, onValueChange = { gitUrl = it }, label = { Text(stringResource(R.string.import_forge_git_url)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = { if (gitUrl.startsWith("http")) viewModel.startImport(sourceType = "git", source = gitUrl, workspacePath = materializedWorkspacePath("imports/git", gitUrl)) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_forge_import_git)) }
                Button(onClick = { viewModel.startImport(sourceType = "zip", source = "/sdcard/Download/import.zip", workspacePath = "imports/zip") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_forge_import_zip)) }
                Button(onClick = { viewModel.startImport(sourceType = "folder", source = "/sdcard/Download/import-folder", workspacePath = "imports/folder") }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.import_forge_import_folder)) }
            } }
        }
        item {
            Card { Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("GitHub discovery", style = MaterialTheme.typography.titleMedium)
                Text("Browse targeted GitHub searches, copy a repository URL, then paste it above to import. Public browsing uses Android's browser intent.")
                DiscoveryLinks.forEach { link ->
                    Button(onClick = { openUrl(link.url) }, modifier = Modifier.fillMaxWidth()) { Text("Browse: ${link.label}") }
                    Text(link.detail, style = MaterialTheme.typography.bodySmall)
                }
            } }
        }
        item { Text(stringResource(R.string.import_forge_timeline)) }
        items(state.timelineEvents) { Text("• $it") }
        if (state.step == ImportForgeStep.PROFILE) item { Text(stringResource(R.string.import_forge_profile_summary)) }
        if (state.step == ImportForgeStep.MISSING_SECRETS) item { Text(stringResource(R.string.import_forge_missing_secrets)) }
        if (state.step == ImportForgeStep.REPAIR_CARDS) item { Text(stringResource(R.string.import_forge_repair_cards)) }
        state.blockedPolicyMessage?.let { m -> item { Text("Policy: $m") } }
    }
}

private data class DiscoveryLink(val label: String, val detail: String, val url: String)

private val DiscoveryLinks = listOf(
    DiscoveryLink("Discord.js", "Slash-command and Node repositories.", "https://github.com/search?type=repositories&q=discord.js+slash+commands+language%3ATypeScript"),
    DiscoveryLink("Telegram / Telegraf", "Telegram automation repositories.", "https://github.com/search?type=repositories&q=telegraf+telegram+language%3ATypeScript"),
    DiscoveryLink("Slack Bolt", "Slack app repositories.", "https://github.com/search?type=repositories&q=slack+bolt+language%3ATypeScript"),
    DiscoveryLink("n8n workflows", "Workflow JSON examples for import inspection.", "https://github.com/search?type=repositories&q=n8n+workflow+json"),
    DiscoveryLink("Webhook workers", "Small HTTP worker repositories.", "https://github.com/search?type=repositories&q=webhook+worker+fastify+language%3ATypeScript"),
    DiscoveryLink("Python automation", "Python repositories with requirements or pyproject metadata.", "https://github.com/search?type=repositories&q=python+automation+requirements.txt"),
)
