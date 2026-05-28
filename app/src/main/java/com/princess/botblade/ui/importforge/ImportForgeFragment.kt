package com.princess.botblade.ui.importforge

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.princess.botblade.R

class ImportForgeFragment : Fragment() {
    private val viewModel: ImportForgeViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent { ImportForgeRoute(viewModel) }
    }
}

@Composable
private fun ImportForgeRoute(viewModel: ImportForgeViewModel) {
    fun materializedWorkspacePath(prefix: String, source: String): String {
        val suffix = source.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "workspace" }
        return "$prefix/$suffix"
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var gitUrl by remember { mutableStateOf("") }
    var browserUrl by remember { mutableStateOf(DiscoveryLinks.first().url) }
    var currentBrowserUrl by remember { mutableStateOf(browserUrl) }
    var browserStatus by remember { mutableStateOf("GitHub browser is scoped to github.com pages.") }
    var webView by remember { mutableStateOf<WebView?>(null) }

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
                Text("GitHub browser", style = MaterialTheme.typography.titleMedium)
                Text("Search GitHub inside BotBlade, open a repository page, then use its current URL as the Git import source. Main-frame navigation is limited to GitHub over HTTPS.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = { webView?.goBack() }, modifier = Modifier.weight(1f)) { Text("Back") }
                    OutlinedButton(onClick = { browserUrl = DiscoveryLinks.first().url }, modifier = Modifier.weight(1f)) { Text("Home") }
                }
                Button(onClick = { gitUrl = currentBrowserUrl }, modifier = Modifier.fillMaxWidth()) { Text("Use current GitHub URL") }
                Text(currentBrowserUrl, style = MaterialTheme.typography.bodySmall)
                Text(browserStatus, style = MaterialTheme.typography.bodySmall)
                GitHubBrowser(
                    url = browserUrl,
                    onWebViewReady = { webView = it },
                    onUrlChanged = { currentBrowserUrl = it },
                    onBlocked = { browserStatus = "Blocked non-GitHub navigation: $it" },
                )
                DiscoveryLinks.forEach { link ->
                    Button(onClick = { browserUrl = link.url }, modifier = Modifier.fillMaxWidth()) { Text("Search: ${link.label}") }
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun GitHubBrowser(
    url: String,
    onWebViewReady: (WebView) -> Unit,
    onUrlChanged: (String) -> Unit,
    onBlocked: (String) -> Unit,
) {
    AndroidView(
        modifier = Modifier.fillMaxWidth().height(520.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                webViewClient = object : WebViewClient() {
                    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                        val nextUrl = request.url.toString()
                        if (!isAllowedGitHubUrl(nextUrl)) {
                            onBlocked(nextUrl)
                            return true
                        }
                        return false
                    }

                    override fun onPageFinished(view: WebView, loadedUrl: String) {
                        onUrlChanged(loadedUrl)
                    }
                }
                loadUrl(url)
                onWebViewReady(this)
            }
        },
        update = { view ->
            if (view.url != url && isAllowedGitHubUrl(url)) view.loadUrl(url)
        },
    )
}

private fun isAllowedGitHubUrl(url: String?): Boolean {
    val uri = runCatching { android.net.Uri.parse(url ?: return false) }.getOrNull() ?: return false
    val scheme = uri.scheme ?: return false
    val host = uri.host ?: return false
    return scheme == "https" && (host == "github.com" || host == "www.github.com" || host == "gist.github.com")
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
