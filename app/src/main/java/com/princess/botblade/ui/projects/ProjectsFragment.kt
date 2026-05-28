package com.princess.botblade.ui.projects

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.MainActivity
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.ProjectScanResponse
import com.princess.botblade.data.repository.EditorRepository
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.ui.components.BladeButton
import com.princess.botblade.ui.components.BotBladeTokens
import com.princess.botblade.ui.components.SectionTitle
import com.princess.botblade.ui.components.StatusChip
import com.princess.botblade.ui.components.StatusTone
import com.princess.botblade.ui.components.WorkstationCard
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import java.text.DateFormat
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {
    private lateinit var repo: LocalProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repo = LocalProjectRepository(requireContext())
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                ProjectsScreen { (activity as? MainActivity)?.showEditorForProject(it.name) }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun ProjectsScreen(onOpen: (LocalProjectRepository.LocalProjectSummary) -> Unit) {
        var showWizard by remember { mutableStateOf(false) }
        var step by remember { mutableIntStateOf(1) }
        var name by remember { mutableStateOf("") }
        var selected by remember { mutableStateOf(Sources.first()) }
        var projects by remember { mutableStateOf(repo.listProjects()) }
        var menuProject by remember { mutableStateOf<LocalProjectRepository.LocalProjectSummary?>(null) }
        var banner by remember { mutableStateOf("Import, create, repair, and open bot workspaces from one forge floor.") }
        var showGitModal by remember { mutableStateOf(false) }
        var gitRepoUrl by remember { mutableStateOf("") }
        var gitBranch by remember { mutableStateOf("") }
        var pendingFolderLane by rememberSaveable { mutableStateOf(SourceLane.OPEN_FOLDER.name) }
        var createActions by remember { mutableStateOf<List<PostCreateAction>>(emptyList()) }
        var onboardingSummary by remember { mutableStateOf<String?>(null) }
        val editorRepo = remember { EditorRepository() }
        val scope = rememberCoroutineScope()
        val validName = name.isNotBlank() && name.matches(Regex("^[a-zA-Z0-9 _-]+$"))
        val available = projects.none { it.name.equals(name.trim(), ignoreCase = true) }
        val canCreate = validName && available && selected.lane == SourceLane.STARTER_TEMPLATE && selected.templateDir != null

        fun openWizard() {
            showWizard = true
            step = 1
            name = ""
            selected = Sources.first()
            gitRepoUrl = ""
            gitBranch = ""
        }

        fun openCreatedProject(result: LocalProjectRepository.ProjectImportResult) {
            projects = repo.listProjects()
            banner = result.message
            onOpen(result.project)
        }

        fun runZipImport(uri: Uri?) {
            if (uri == null) {
                banner = "ZIP import canceled."
                return
            }
            showWizard = false
            banner = "Import ZIP: registering archive as a managed project…"
            scope.launch { openCreatedProject(repo.importFromZip(uri)) }
        }

        fun runFolderAction(uri: Uri?, lane: SourceLane) {
            if (uri == null) {
                banner = if (lane == SourceLane.OPEN_FOLDER) "Folder selection canceled." else "Repair workspace selection canceled."
                return
            }
            showWizard = false
            banner = when (lane) {
                SourceLane.OPEN_FOLDER -> "Open Folder: registering workspace root…"
                SourceLane.REPAIR_EXISTING -> "Repair Existing: creating repair shell…"
                else -> banner
            }
            scope.launch {
                when (lane) {
                    SourceLane.OPEN_FOLDER -> openCreatedProject(repo.registerWorkspaceFolder(uri))
                    SourceLane.REPAIR_EXISTING -> openCreatedProject(repo.repairWorkspace(uri))
                    else -> Unit
                }
            }
        }

        val zipPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument(), ::runZipImport)
        val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            runFolderAction(uri, SourceLane.valueOf(pendingFolderLane))
        }

        LaunchedEffect(Unit) {
            val prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getBoolean(OPEN_ADD_PROJECT, false)) {
                prefs.edit().remove(OPEN_ADD_PROJECT).apply()
                banner = "Choose a starter template to create your bot workspace."
                openWizard()
            }
        }

        Scaffold(
            containerColor = BotBlack,
            floatingActionButton = {
                FloatingActionButton(containerColor = HotPink, contentColor = BotBlack, onClick = ::openWizard) {
                    Text("+", fontWeight = FontWeight.Bold)
                }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(BotBlack).padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { Hero(banner, ::openWizard) }
                item { FlowRail() }
                item { SectionTitle("Workspaces") }
                if (projects.isEmpty()) {
                    item { EmptyState(::openWizard) }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(
                            project = project,
                            onOpen = onOpen,
                            onStatusClick = { selectedProject ->
                                banner = "${selectedProject.name} status · ${selectedProject.status}. Runtime/build details are available in Ops Deck."
                            },
                            onForgeReadyClick = { selectedProject ->
                                banner = "Starting Blade Pack scan for ${selectedProject.name}. Opening Forge Editor scan surface."
                                onOpen(selectedProject)
                            },
                            onOpenClick = onOpen,
                            onMenu = { menuProject = project },
                        )
                    }
                }
            }
        }

        menuProject?.let { project ->
            DropdownMenu(expanded = true, onDismissRequest = { menuProject = null }) {
                DropdownMenuItem(
                    text = { Text("Open in Forge Editor") },
                    onClick = {
                        menuProject = null
                        onOpen(project)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Quick rename") },
                    onClick = {
                        val renamed = repo.renameProject(project.id, nextRenamedName(project.name, projects))
                        if (renamed) {
                            projects = repo.listProjects()
                            banner = "Renamed ${project.name}."
                        }
                        menuProject = null
                    },
                )
                DropdownMenuItem(
                    text = { Text("Delete") },
                    onClick = {
                        repo.deleteProject(project.id)
                        projects = repo.listProjects()
                        banner = "Deleted ${project.name}."
                        menuProject = null
                    },
                )
            }
        }

        if (showWizard) {
            ModalBottomSheet(onDismissRequest = { showWizard = false }, containerColor = Panel) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Add Project", color = BabyBlue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Pick a source lane and continue through source-specific actions.", color = Muted)
                    when (step) {
                        1 -> {
                            Sources.forEach { source ->
                                FilterChip(
                                    selected = selected == source,
                                    onClick = { selected = source },
                                    label = {
                                        Column(Modifier.padding(4.dp)) {
                                            Text(source.title, fontWeight = FontWeight.Bold)
                                            Text(source.detail)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            BladeButton(
                                text = "Continue",
                                onClick = {
                                    when (selected.lane) {
                                        SourceLane.STARTER_TEMPLATE -> step = 2
                                        SourceLane.IMPORT_GIT -> showGitModal = true
                                        SourceLane.IMPORT_ZIP -> zipPicker.launch(arrayOf("application/zip", "application/octet-stream"))
                                        SourceLane.OPEN_FOLDER, SourceLane.REPAIR_EXISTING -> {
                                            pendingFolderLane = selected.lane.name
                                            folderPicker.launch(null)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                        2 -> {
                            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Project name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Text(
                                when {
                                    name.isBlank() -> "Use letters, numbers, spaces, dashes, or underscores."
                                    !validName -> "Project names can use letters, numbers, spaces, dashes, and underscores."
                                    !available -> "That project already exists."
                                    else -> "Ready to forge ${name.trim()}."
                                },
                                color = if (validName && available) BabyBlue else Muted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
                                BladeButton("Review", onClick = { step = 3 }, enabled = canCreate, modifier = Modifier.weight(1f))
                            }
                        }
                        else -> {
                            ReviewCard(name.trim(), selected)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) { Text("Back") }
                                BladeButton(
                                    text = "Create + Open",
                                    onClick = {
                                        val projectRoot = repo.createProject(name.trim(), selected.templateDir ?: return@BladeButton)
                                        val projectId = projectRoot.name
                                        projects = repo.listProjects()
                                        showWizard = false
                                        createActions = listOf(
                                            PostCreateAction("Project created", "Created ${projectRoot.name} from ${selected.title}."),
                                            PostCreateAction("Run scan now", "Initial project scan queued and auto-run attempted."),
                                            PostCreateAction("Open secrets", "Open Secrets to complete optional checklist for tokens and API keys."),
                                            PostCreateAction("Start build", "Start first build to validate template and runtime."),
                                        )
                                        onboardingSummary = null
                                        banner = "Created ${projectRoot.name} from ${selected.title}. Running onboarding scan…"
                                        projects.firstOrNull { it.id == projectId }?.let(onOpen)
                                        scope.launch {
                                            val scanResult = runCatching { editorRepo.scanProject(projectId) }.getOrNull()
                                            onboardingSummary = scanResult.toScanSummary()
                                            banner = when (scanResult) {
                                                is ApiResult.Success -> "${projectRoot.name}: ${scanResult.data.matches.size} scan matches, recommended ${scanResult.data.recommendedPackId}."
                                                is ApiResult.Error -> "${projectRoot.name}: scan endpoint unavailable (${scanResult.message})."
                                                ApiResult.Loading -> "${projectRoot.name}: scan in progress…"
                                                null -> "${projectRoot.name}: scan endpoint unavailable."
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }

        if (createActions.isNotEmpty()) {
            AlertDialog(
                onDismissRequest = { createActions = emptyList() },
                title = { Text("Post-create actions") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        createActions.forEach { action ->
                            Text("• ${action.title}: ${action.detail}", color = OnSurface)
                        }
                        onboardingSummary?.let { Text(it, color = Muted) }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { createActions = emptyList() }) { Text("Done") }
                },
            )
        }

        if (showGitModal) {
            ModalBottomSheet(onDismissRequest = { showGitModal = false }, containerColor = Panel) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Import from Git", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = gitRepoUrl,
                        onValueChange = { gitRepoUrl = it },
                        label = { Text("Repository URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = gitBranch,
                        onValueChange = { gitBranch = it },
                        label = { Text("Branch (optional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = { showGitModal = false }, modifier = Modifier.weight(1f)) { Text("Cancel") }
                        BladeButton(
                            text = "Import",
                            onClick = {
                                banner = "Import from Git: registering repository as a managed project…"
                                scope.launch {
                                    val result = repo.importFromGit(gitRepoUrl.trim(), gitBranch.trim().ifBlank { null })
                                    showGitModal = false
                                    showWizard = false
                                    openCreatedProject(result)
                                }
                            },
                            enabled = gitRepoUrl.trim().startsWith("http"),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
    }

    @Composable private fun Hero(banner: String, onAdd: () -> Unit) {
        WorkstationCard(accent = HotPink) {
            Text("Projects", color = BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Your forge floor for importing, creating, repairing, scanning, and opening bot workspaces.", color = OnSurface)
            Text(banner, color = Muted)
            BladeButton("Add bot project", onClick = onAdd, modifier = Modifier.fillMaxWidth().padding(top = 2.dp))
        }
    }

    @Composable private fun FlowRail() {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Workstation flow")
            listOf(
                "Create starter templates, register Git/ZIP/folder sources, or create a repair shell.",
                "Every source lane creates a managed BotBlade project and opens it in Forge Editor.",
                "After opening, run Scan, add secrets, build, and deploy from the workstation flow.",
            ).forEachIndexed { index, line ->
                WorkstationCard(accent = if (index % 2 == 0) BabyBlue else HotPink) {
                    Text(line, color = OnSurface)
                }
            }
        }
    }

    @Composable private fun EmptyState(onAdd: () -> Unit) {
        WorkstationCard(accent = BabyBlue) {
            Text("No bots in the forge yet", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Start with a starter template, Git source, ZIP archive, folder registration, or repair shell.", color = Muted)
            BladeButton("Create first project", onClick = onAdd, modifier = Modifier.fillMaxWidth())
        }
    }

    @Composable private fun ProjectCard(
        project: LocalProjectRepository.LocalProjectSummary,
        onOpen: (LocalProjectRepository.LocalProjectSummary) -> Unit,
        onStatusClick: (LocalProjectRepository.LocalProjectSummary) -> Unit,
        onForgeReadyClick: (LocalProjectRepository.LocalProjectSummary) -> Unit,
        onOpenClick: (LocalProjectRepository.LocalProjectSummary) -> Unit,
        onMenu: () -> Unit,
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = { onOpen(project) }, onLongClick = onMenu),
            colors = CardDefaults.cardColors(containerColor = Panel),
            border = BorderStroke(1.dp, Stroke),
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(project.name, color = BabyBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Last modified: ${DateFormat.getDateTimeInstance().format(project.lastModified)}", color = Muted)
                    }
                    TextButton(onClick = onMenu) { Text("⋯", color = HotPink) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusChip(project.status, StatusTone.Info)
                    StatusChip("Forge-ready", StatusTone.Success)
                    AssistChip(onClick = { onStatusClick(project) }, label = { Text("Status") })
                    AssistChip(onClick = { onForgeReadyClick(project) }, label = { Text("Scan") })
                    AssistChip(onClick = { onOpenClick(project) }, label = { Text("Open") })
                }
                Text("Tap to open editor. Long press or tap ⋯ for project actions.", color = Muted)
            }
        }
    }

    @Composable private fun ReviewCard(projectName: String, source: Source) {
        WorkstationCard(accent = BabyBlue) {
            Text("Review forge order", color = BabyBlue, fontWeight = FontWeight.Bold)
            Text("Name: $projectName", color = OnSurface)
            Text("Source: ${source.title}", color = OnSurface)
            Text("First action: create workspace, open Forge Editor, trigger scan, then continue with secrets and build.", color = Muted)
        }
    }

    private fun nextRenamedName(currentName: String, projects: List<LocalProjectRepository.LocalProjectSummary>): String {
        val existing = projects.map { it.name.lowercase() }.toSet()
        var index = 1
        var candidate = "$currentName-renamed"
        while (candidate.lowercase() in existing) {
            index += 1
            candidate = "$currentName-renamed-$index"
        }
        return candidate
    }

    private enum class SourceLane { STARTER_TEMPLATE, IMPORT_GIT, IMPORT_ZIP, OPEN_FOLDER, REPAIR_EXISTING }

    private data class Source(val title: String, val detail: String, val lane: SourceLane, val templateDir: String?)

    private data class PostCreateAction(val title: String, val detail: String)

    private fun ApiResult<ProjectScanResponse>?.toScanSummary(): String = when (this) {
        is ApiResult.Success -> {
            val top = data.matches.firstOrNull()?.let { " Top match ${it.name} (${it.confidence})." } ?: ""
            "Scan complete: ${data.matches.size} match(es), recommended pack ${data.recommendedPackId}.${top}"
        }
        is ApiResult.Error -> "Scan unavailable: ${message}"
        ApiResult.Loading -> "Scan in progress..."
        null -> "Scan skipped: endpoint unavailable."
    }

    private companion object {
        const val PREFS = "botblade_workstation_flow"
        const val OPEN_ADD_PROJECT = "open_add_project"
        val BotBlack = BotBladeTokens.Black
        val Panel = BotBladeTokens.Panel
        val Stroke = BotBladeTokens.Stroke
        val BabyBlue = BotBladeTokens.BabyBlue
        val HotPink = BotBladeTokens.HotPink
        val OnSurface = ColorToken.OnSurface
        val Muted = BotBladeTokens.Muted
        val Sources = listOf(
            Source("Discord TypeScript Starter", "slash-command bot with generated TypeScript project files", SourceLane.STARTER_TEMPLATE, "project_templates/simple_echo_bot"),
            Source("Moderation Toolkit", "moderation-oriented starter ready for rules and actions", SourceLane.STARTER_TEMPLATE, "project_templates/moderation_bot"),
            Source("Blank Workspace", "clean bot workspace for custom experiments", SourceLane.STARTER_TEMPLATE, "project_templates/blank_project"),
            Source("Import from Git", "register a Git repository as a BotBlade project", SourceLane.IMPORT_GIT, null),
            Source("Import ZIP", "register a ZIP archive as a managed BotBlade project", SourceLane.IMPORT_ZIP, null),
            Source("Open Folder", "register an Android workspace folder as a BotBlade project", SourceLane.OPEN_FOLDER, null),
            Source("Repair Existing", "create metadata and repair notes for an existing workspace", SourceLane.REPAIR_EXISTING, null),
        )
    }

    private object ColorToken {
        val OnSurface = androidx.compose.ui.graphics.Color(0xFFEEF7FF)
    }
}
