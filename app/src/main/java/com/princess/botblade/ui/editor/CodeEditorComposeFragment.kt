package com.princess.botblade.ui.editor

import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.MainActivity
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.ProjectFileContent
import com.princess.botblade.data.model.ProjectFileSummary
import com.princess.botblade.data.repository.EditorRepository
import com.princess.botblade.data.store.ActiveProjectStore
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.FlowLane
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.TerminalView
import com.princess.botblade.ui.components.WorkstationCard
import com.princess.botblade.ui.shell.BotBladeDestination
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import kotlinx.coroutines.launch

class CodeEditorComposeFragment : Fragment() {
    private val repository = EditorRepository()

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        val activeProjectStore = ActiveProjectStore(requireContext())
        val projectId = activeProjectStore.getActiveProjectId()
        val projectName = activeProjectStore.getActiveProjectName()
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                EditorScreen(projectId = projectId, projectName = projectName)
            }
        }
    }

    @Composable
    private fun EditorScreen(projectId: String?, projectName: String?) {
        val scope = rememberCoroutineScope()
        var status by remember { mutableStateOf(if (projectId == null) "Select a project before editing." else "Editor ready.") }
        var files by remember { mutableStateOf<List<ProjectFileSummary>>(emptyList()) }
        var query by remember { mutableStateOf("") }
        var selectedFile by remember { mutableStateOf<ProjectFileContent?>(null) }
        var editorText by remember { mutableStateOf("") }
        var terminalLines by remember { mutableStateOf(listOf("Editor terminal ready.")) }
        var scanSummary by remember { mutableStateOf("Scan has not run yet.") }
        var expandedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
        val dirty = selectedFile?.editable == true && selectedFile?.content != editorText
        val visibleFiles = if (query.isBlank()) files else files.filter { it.path.contains(query, ignoreCase = true) }
        val fileTree = remember(visibleFiles) { buildEditorTree(visibleFiles) }
        val visibleTree = remember(fileTree, expandedFolders, query) { flattenEditorTree(fileTree, expandedFolders, forceExpanded = query.isNotBlank()) }

        fun appendTerminal(line: String) {
            terminalLines = (terminalLines + line).takeLast(250)
        }

        fun loadFiles() = scope.launch {
            val id = projectId ?: return@launch
            status = "Loading project files…"
            when (val result = repository.listFiles(id)) {
                is ApiResult.Success -> {
                    files = result.data.sortedBy { it.path }
                    expandedFolders = defaultExpandedFolders(result.data)
                    status = "Loaded ${result.data.size} file(s)."
                }
                is ApiResult.Error -> status = "File load failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun generateProject() = scope.launch {
            val id = projectId ?: return@launch
            status = "Generating project files…"
            when (val result = repository.generateProject(id)) {
                is ApiResult.Success -> {
                    files = result.data.sortedBy { it.path }
                    expandedFolders = defaultExpandedFolders(result.data)
                    status = "Generated ${result.data.size} file(s)."
                    appendTerminal(status)
                }
                is ApiResult.Error -> status = "Generate failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun openFile(path: String) = scope.launch {
            val id = projectId ?: return@launch
            status = "Opening $path…"
            when (val result = repository.getFile(id, path)) {
                is ApiResult.Success -> {
                    selectedFile = result.data
                    editorText = result.data.content
                    status = "Opened ${result.data.path}."
                }
                is ApiResult.Error -> status = "Open failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun saveFile() = scope.launch {
            val id = projectId ?: return@launch
            val file = selectedFile ?: return@launch
            status = "Saving ${file.path}…"
            when (val result = repository.saveFile(id, file.path, editorText)) {
                is ApiResult.Success -> {
                    selectedFile = result.data
                    editorText = result.data.content
                    status = "Saved ${result.data.path}."
                    appendTerminal(status)
                }
                is ApiResult.Error -> status = "Save failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun scanProject() = scope.launch {
            val id = projectId ?: return@launch
            status = "Scanning project…"
            when (val result = repository.scanProject(id)) {
                is ApiResult.Success -> {
                    val top = result.data.matches.firstOrNull { it.id == result.data.recommendedPackId } ?: result.data.matches.firstOrNull()
                    scanSummary = if (top == null) {
                        "No Blade Pack match found."
                    } else {
                        "${top.name}: ${top.confidence} confidence (${top.score}). Secrets: ${top.requiredSecrets.joinToString().ifBlank { "none" }}."
                    }
                    status = "Scan complete."
                    appendTerminal(scanSummary)
                }
                is ApiResult.Error -> status = "Scan failed: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        fun startBuild() = scope.launch {
            val id = projectId ?: return@launch
            status = "Starting build…"
            when (val result = repository.createBuild(id)) {
                is ApiResult.Success -> {
                    status = "Build ${result.data.buildId}: ${result.data.status}"
                    appendTerminal(status)
                    (activity as? MainActivity)?.openDestination(BotBladeDestination.Deployments)
                }
                is ApiResult.Error -> status = "Build failed to start: ${result.message}"
                ApiResult.Loading -> Unit
            }
        }

        LaunchedEffect(projectId) {
            if (projectId != null) loadFiles()
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(BotBladeTokens.Black)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                WorkstationCard(accent = BotBladeTokens.HotPink) {
                    Text("Forge Editor", color = BotBladeTokens.BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Project: ${projectName ?: projectId ?: "none selected"}", color = BotBladeTokens.Muted)
                    Text(status, color = BotBladeTokens.Muted)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        StatusChip("Files ${files.size}", if (files.isEmpty()) StatusTone.Warning else StatusTone.Success)
                        StatusChip("Dirty", if (dirty) StatusTone.Warning else StatusTone.Neutral)
                        StatusChip("Scan", if (scanSummary.startsWith("Scan has")) StatusTone.Neutral else StatusTone.Info)
                    }
                }
            }

            item {
                FlowLane("First check", "Scan, generate missing starter files, save edits, then build.", BotBladeTokens.BabyBlue) {
                    BladeButton("Scan", onClick = ::scanProject, enabled = projectId != null, modifier = Modifier.weight(1f))
                    BladeButton("Build", onClick = ::startBuild, enabled = projectId != null, modifier = Modifier.weight(1f))
                }
            }

            item {
                WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                    Text("Project explorer", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("Filter files") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        BladeButton("Reload", onClick = ::loadFiles, enabled = projectId != null, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = ::generateProject, enabled = projectId != null, modifier = Modifier.weight(1f)) { Text("Generate") }
                    }
                }
            }

            if (visibleTree.isNotEmpty()) {
                item { SectionTitle("File tree") }
                items(visibleTree.take(120), key = { "${it.node.type}:${it.node.path}" }) { visibleNode ->
                    val node = visibleNode.node
                    WorkstationCard(accent = if (node.path == selectedFile?.path) BotBladeTokens.Success else if (node.type == EditorNodeType.Directory) BotBladeTokens.GlitterGold else BotBladeTokens.BabyBlue) {
                        val prefix = "  ".repeat(visibleNode.depth)
                        val marker = if (node.type == EditorNodeType.Directory) {
                            if (node.path in expandedFolders || query.isNotBlank()) "▾" else "▸"
                        } else {
                            "•"
                        }
                        Text("$prefix$marker ${node.name}", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                        if (node.type == EditorNodeType.File) {
                            val file = node.file
                            Text("$prefix${if (file?.editable == true) "editable" else "preview only"} • ${file?.size ?: 0} bytes", color = BotBladeTokens.Muted)
                        } else {
                            Text("$prefix${node.children.size} item(s)", color = BotBladeTokens.Muted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                            if (node.type == EditorNodeType.Directory) {
                                AssistChip(
                                    onClick = {
                                        expandedFolders = if (node.path in expandedFolders) expandedFolders - node.path else expandedFolders + node.path
                                    },
                                    label = { Text(if (node.path in expandedFolders || query.isNotBlank()) "Collapse" else "Expand") },
                                )
                            } else {
                                AssistChip(onClick = { openFile(node.path) }, label = { Text("Open") })
                            }
                        }
                    }
                }
            }

            item { SectionTitle("Open file") }
            item {
                WorkstationCard(accent = if (dirty) BotBladeTokens.GlitterGold else BotBladeTokens.HotPink) {
                    Text(selectedFile?.path ?: "No file open", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    Text(selectedFile?.let { "Updated: ${it.updatedAt} • ${it.size} bytes" } ?: "Open a file from the project explorer.", color = BotBladeTokens.Muted)
                    OutlinedTextField(
                        value = editorText,
                        onValueChange = { editorText = it },
                        enabled = selectedFile?.editable == true,
                        label = { Text(if (selectedFile?.editable == true) "Code" else "Preview") },
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 260.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 10.dp)) {
                        BladeButton("Save", onClick = ::saveFile, enabled = dirty, modifier = Modifier.weight(1f))
                        OutlinedButton(onClick = { selectedFile?.let { editorText = it.content } }, enabled = dirty, modifier = Modifier.weight(1f)) { Text("Revert") }
                    }
                }
            }

            item {
                WorkstationCard(accent = BotBladeTokens.BabyBlue) {
                    Text("Problems / scan", color = BotBladeTokens.BabyBlue, fontWeight = FontWeight.Bold)
                    Text(scanSummary, color = BotBladeTokens.Muted)
                }
            }

            item { TerminalView(title = "Editor output", lines = terminalLines) }
        }
    }
}

private enum class EditorNodeType { Directory, File }

private data class EditorTreeNode(
    val name: String,
    val path: String,
    val type: EditorNodeType,
    val file: ProjectFileSummary? = null,
    val children: List<EditorTreeNode> = emptyList(),
)

private data class VisibleEditorNode(val node: EditorTreeNode, val depth: Int)

private data class MutableEditorTreeNode(
    val name: String,
    val path: String,
    val type: EditorNodeType,
    var file: ProjectFileSummary? = null,
    val children: MutableList<MutableEditorTreeNode> = mutableListOf(),
)

private fun buildEditorTree(files: List<ProjectFileSummary>): List<EditorTreeNode> {
    val roots = mutableListOf<MutableEditorTreeNode>()
    files.forEach { file ->
        val segments = file.path.replace('\\', '/').trim('/').split('/').filter { it.isNotBlank() }
        var children = roots
        var currentPath = ""
        segments.forEachIndexed { index, segment ->
            currentPath = if (currentPath.isBlank()) segment else "$currentPath/$segment"
            val isFile = index == segments.lastIndex
            val existing = children.firstOrNull { it.name == segment }
            if (isFile) {
                if (existing == null) {
                    children += MutableEditorTreeNode(segment, currentPath, EditorNodeType.File, file)
                } else {
                    existing.file = file
                }
            } else {
                val directory = existing ?: MutableEditorTreeNode(segment, currentPath, EditorNodeType.Directory).also { children += it }
                children = directory.children
            }
        }
    }
    return roots.map { it.freeze() }.sortedWith(editorTreeComparator)
}

private fun MutableEditorTreeNode.freeze(): EditorTreeNode = EditorTreeNode(
    name = name,
    path = path,
    type = type,
    file = file,
    children = children.map { it.freeze() }.sortedWith(editorTreeComparator),
)

private val editorTreeComparator = compareBy<EditorTreeNode> { if (it.type == EditorNodeType.Directory) 0 else 1 }.thenBy { it.name.lowercase() }

private fun flattenEditorTree(nodes: List<EditorTreeNode>, expandedFolders: Set<String>, forceExpanded: Boolean = false, depth: Int = 0): List<VisibleEditorNode> = nodes.flatMap { node ->
    val current = VisibleEditorNode(node, depth)
    if (node.type == EditorNodeType.Directory && (forceExpanded || node.path in expandedFolders)) listOf(current) + flattenEditorTree(node.children, expandedFolders, forceExpanded, depth + 1) else listOf(current)
}

private fun defaultExpandedFolders(files: List<ProjectFileSummary>): Set<String> = files
    .flatMap { file ->
        val segments = file.path.replace('\\', '/').trim('/').split('/').filter { it.isNotBlank() }
        segments.dropLast(1).scan("") { prefix, segment -> if (prefix.isBlank()) segment else "$prefix/$segment" }.drop(1)
    }
    .take(40)
    .toSet()
