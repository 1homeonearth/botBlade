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
import androidx.compose.foundation.lazy.LazyRow
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
        var favoriteRepoKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
        var favoriteSnippetIds by rememberSaveable { mutableStateOf(listOf<String>()) }
        var snippetCategory by rememberSaveable { mutableStateOf(SnippetCategories.first()) }
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
                banner = when (lane) {
                    SourceLane.OPEN_FOLDER -> "Folder selection canceled."
                    SourceLane.OPEN_ROOT_FOLDER -> "Root workspace selection canceled."
                    SourceLane.REPAIR_EXISTING -> "Repair workspace selection canceled."
                    SourceLane.REPAIR_ROOT -> "Root repair selection canceled."
                    else -> "Folder action canceled."
                }
                return
            }
            showWizard = false
            banner = when (lane) {
                SourceLane.OPEN_FOLDER -> "Open Folder: registering workspace root…"
                SourceLane.OPEN_ROOT_FOLDER -> "Root Workspace: registering top-level anchor…"
                SourceLane.REPAIR_EXISTING -> "Repair Existing: creating repair shell…"
                SourceLane.REPAIR_ROOT -> "Root Repair: creating root-aware repair shell…"
                else -> banner
            }
            scope.launch {
                when (lane) {
                    SourceLane.OPEN_FOLDER -> openCreatedProject(repo.registerWorkspaceFolder(uri))
                    SourceLane.OPEN_ROOT_FOLDER -> openCreatedProject(repo.registerRootWorkspace(uri))
                    SourceLane.REPAIR_EXISTING -> openCreatedProject(repo.repairWorkspace(uri))
                    SourceLane.REPAIR_ROOT -> openCreatedProject(repo.repairRootWorkspace(uri))
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
                item {
                    DiscoveryGrove(
                        favoriteRepoKeys = favoriteRepoKeys,
                        onToggleFavorite = { key ->
                            favoriteRepoKeys = if (key in favoriteRepoKeys) favoriteRepoKeys - key else favoriteRepoKeys + key
                        },
                        onStageImport = { repo ->
                            gitRepoUrl = repo.url
                            gitBranch = repo.branch.orEmpty()
                            showGitModal = true
                            banner = "Staged ${repo.ownerRepo} from the ${repo.category} tree. Review branch and import when ready."
                        },
                    )
                }
                item {
                    SnippetMixer(
                        selectedCategory = snippetCategory,
                        favoriteSnippetIds = favoriteSnippetIds,
                        onSelectCategory = { snippetCategory = it },
                        onToggleFavorite = { snippet ->
                            favoriteSnippetIds = if (snippet.id in favoriteSnippetIds) favoriteSnippetIds - snippet.id else favoriteSnippetIds + snippet.id
                            banner = "${snippet.title} is ready for ${snippet.category} mix-and-match notes. Ask AI to adapt it after import."
                        },
                    )
                }
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
                                        SourceLane.OPEN_FOLDER, SourceLane.OPEN_ROOT_FOLDER, SourceLane.REPAIR_EXISTING, SourceLane.REPAIR_ROOT -> {
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

    @Composable private fun DiscoveryGrove(
        favoriteRepoKeys: List<String>,
        onToggleFavorite: (String) -> Unit,
        onStageImport: (RecommendedRepo) -> Unit,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("GitHub discovery grove")
            WorkstationCard(accent = BotBladeTokens.GlitterGold) {
                Text("Massive recommended repo trees", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Browse a big seeded forest of bot, workflow, AI-agent, template, and utility repos. Favorite finds, stage an import, then mix sources only after BotBlade scans them as untrusted code.", color = Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    items(RepoCategories, key = { it.title }) { category ->
                        StatusChip("${category.title} · ${category.repos.size}", StatusTone.Info)
                    }
                    item { StatusChip("Favorites · ${favoriteRepoKeys.size}", StatusTone.Success) }
                }
            }
            RepoCategories.forEach { category ->
                WorkstationCard(accent = category.accent) {
                    Text(category.title, color = BabyBlue, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(category.detail, color = Muted)
                    category.repos.forEach { repo ->
                        val key = repo.ownerRepo
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                            colors = CardDefaults.cardColors(containerColor = BotBlack),
                            border = BorderStroke(1.dp, Stroke),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f)) {
                                        Text(repo.ownerRepo, color = OnSurface, fontWeight = FontWeight.Bold)
                                        Text(repo.description, color = Muted)
                                    }
                                    Text(if (key in favoriteRepoKeys) "★" else "☆", color = HotPink, fontWeight = FontWeight.Bold)
                                }
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    items(repo.tags) { tag -> StatusChip(tag, StatusTone.Neutral) }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                    OutlinedButton(onClick = { onToggleFavorite(key) }, modifier = Modifier.weight(1f)) {
                                        Text(if (key in favoriteRepoKeys) "Favorited" else "Favorite")
                                    }
                                    BladeButton("Stage import", onClick = { onStageImport(repo) }, modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable private fun SnippetMixer(
        selectedCategory: String,
        favoriteSnippetIds: List<String>,
        onSelectCategory: (String) -> Unit,
        onToggleFavorite: (SnippetSeed) -> Unit,
    ) {
        val snippets = SnippetSeeds.filter { it.category == selectedCategory }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Snippet mixboard")
            WorkstationCard(accent = HotPink) {
                Text("Favorite tiny GitHub-page ideas", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Save little code patterns by category before import: command handlers, webhook guards, workflow steps, prompts, env contracts, and deploy snippets. AI help is intentionally a guided adaptation step, not blind copy-paste.", color = Muted)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 10.dp)) {
                    items(SnippetCategories) { category ->
                        FilterChip(selected = selectedCategory == category, onClick = { onSelectCategory(category) }, label = { Text(category) })
                    }
                }
                snippets.forEach { snippet ->
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        colors = CardDefaults.cardColors(containerColor = BotBlack),
                        border = BorderStroke(1.dp, Stroke),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(snippet.title, color = OnSurface, fontWeight = FontWeight.Bold)
                                    Text(snippet.detail, color = Muted)
                                }
                                Text(if (snippet.id in favoriteSnippetIds) "★" else "☆", color = HotPink, fontWeight = FontWeight.Bold)
                            }
                            Text("Source hint: ${snippet.sourceHint}", color = Muted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                OutlinedButton(onClick = { onToggleFavorite(snippet) }, modifier = Modifier.weight(1f)) {
                                    Text(if (snippet.id in favoriteSnippetIds) "Saved" else "Save snippet")
                                }
                                OutlinedButton(onClick = { onToggleFavorite(snippet) }, modifier = Modifier.weight(1f)) {
                                    Text("AI adapt")
                                }
                            }
                        }
                    }
                }
                Text("Saved snippets: ${favoriteSnippetIds.size}. Categories stay local to the project flow until a future snippet vault is wired.", color = Muted, modifier = Modifier.padding(top = 10.dp))
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

    private enum class SourceLane { STARTER_TEMPLATE, IMPORT_GIT, IMPORT_ZIP, OPEN_FOLDER, OPEN_ROOT_FOLDER, REPAIR_EXISTING, REPAIR_ROOT }

    private data class Source(val title: String, val detail: String, val lane: SourceLane, val templateDir: String?)

    private data class RecommendedRepo(
        val ownerRepo: String,
        val description: String,
        val url: String,
        val category: String,
        val branch: String? = null,
        val tags: List<String>,
    )

    private data class RepoCategory(val title: String, val detail: String, val accent: androidx.compose.ui.graphics.Color, val repos: List<RecommendedRepo>)

    private data class SnippetSeed(val id: String, val category: String, val title: String, val detail: String, val sourceHint: String)

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

        val SnippetCategories = listOf("Commands", "Webhooks", "Workflows", "AI Prompts", "Secrets", "Deploy")
        val SnippetSeeds = listOf(
            SnippetSeed("cmd-slash-router", "Commands", "Slash command router", "Capture a minimal command registry shape that can be adapted after scan.", "discord.js guide command handling pages"),
            SnippetSeed("cmd-permission-gate", "Commands", "Permission gate", "Save an admin/mod role guard pattern for later AI-assisted rewriting.", "Discord moderation bot examples"),
            SnippetSeed("cmd-telegraf-scene", "Commands", "Telegraf scene step", "Track a conversational wizard step without importing a whole framework.", "telegraf/telegraf examples"),
            SnippetSeed("hook-signature", "Webhooks", "Webhook signature verifier", "Favorite request verification notes for Slack, GitHub, Stripe, and generic HMAC flows.", "platform webhook docs and sample repos"),
            SnippetSeed("hook-retry", "Webhooks", "Retry-safe handler", "Keep idempotency, queue, and replay notes for webhook workers.", "Fastify/Express worker examples"),
            SnippetSeed("flow-n8n-github", "Workflows", "GitHub issue triage flow", "Mark workflow JSON ideas that turn repo events into notifications or tickets.", "n8n workflow collections"),
            SnippetSeed("flow-approval", "Workflows", "Human approval step", "Save approval-loop ideas before adapting them into BotBlade tasks.", "Activepieces and n8n templates"),
            SnippetSeed("ai-system-card", "AI Prompts", "System prompt card", "Store prompt role, safety rails, and expected JSON output as a reusable idea.", "agent template repositories"),
            SnippetSeed("ai-rag-tool", "AI Prompts", "RAG tool call", "Favorite retrieval/tool-call snippets for later AI-assisted extraction.", "LangChain/LlamaIndex examples"),
            SnippetSeed("sec-env-contract", "Secrets", "Environment contract", "Capture env var names and secret-reference metadata without copying values.", "README and .env.example files"),
            SnippetSeed("sec-redaction", "Secrets", "Redaction helper", "Keep masking and audit-log ideas as a small adaptation note.", "security utility snippets"),
            SnippetSeed("deploy-health", "Deploy", "Health check probe", "Save tiny status endpoint/deploy check ideas for later generated project hardening.", "worker and API templates"),
        )
        val RepoCategories = listOf(
            RepoCategory("Awesome bot-builder lists", "Large catalogs to seed safe import discovery without vendoring upstream projects.", BotBladeTokens.GlitterGold, listOf(
                RecommendedRepo("sindresorhus/awesome", "Master awesome-list index for finding focused bot, workflow, AI, and security lists.", "https://github.com/sindresorhus/awesome", "Awesome", tags = listOf("awesome-list", "index", "discovery")),
                RecommendedRepo("sorrycc/awesome-javascript", "JavaScript ecosystem index for Node bot dependencies and utility choices.", "https://github.com/sorrycc/awesome-javascript", "Awesome", tags = listOf("javascript", "node", "catalog")),
                RecommendedRepo("vinta/awesome-python", "Python ecosystem index for bot, automation, security, and data tooling.", "https://github.com/vinta/awesome-python", "Awesome", tags = listOf("python", "catalog", "libraries")),
                RecommendedRepo("avelino/awesome-go", "Go catalog for future tiny webhook workers, scanners, and single-binary utilities.", "https://github.com/avelino/awesome-go", "Awesome", tags = listOf("go", "workers", "catalog")),
                RecommendedRepo("trimstray/the-book-of-secret-knowledge", "Broad operator reference for networking, CLI, web, and security learning paths.", "https://github.com/trimstray/the-book-of-secret-knowledge", "Awesome", tags = listOf("reference", "ops", "security")),
                RecommendedRepo("meirwah/awesome-incident-response", "Incident-response catalog for future blue-team repair cards and runbooks.", "https://github.com/meirwah/awesome-incident-response", "Awesome", tags = listOf("incident-response", "blue-team", "runbooks")),
                RecommendedRepo("lorien/awesome-web-scraping", "Scraping and automation reference for authorized data collection projects.", "https://github.com/lorien/awesome-web-scraping", "Awesome", tags = listOf("scraping", "automation", "catalog")),
                RecommendedRepo("mfornos/awesome-microservices", "Microservice patterns for webhook bots, queues, and API workers.", "https://github.com/mfornos/awesome-microservices", "Awesome", tags = listOf("microservices", "workers", "patterns")),
            )),
            RepoCategory("Discord command forests", "Slash commands, frameworks, ticketing, moderation, music, and multipurpose bot code.", BabyBlue, listOf(
                RecommendedRepo("discordjs/discord.js", "Core Discord API library and examples for modern JavaScript and TypeScript bots.", "https://github.com/discordjs/discord.js", "Discord", tags = listOf("library", "slash", "gateway")),
                RecommendedRepo("discordjs/guide", "Official guide source with command handling and project-structure examples.", "https://github.com/discordjs/guide", "Discord", tags = listOf("guide", "commands", "reference")),
                RecommendedRepo("sapphiredev/framework", "Advanced Discord bot framework layered on discord.js with TypeScript-first patterns.", "https://github.com/sapphiredev/framework", "Discord", tags = listOf("framework", "typescript", "commands")),
                RecommendedRepo("discordeno/discordeno", "Discord API library with modular gateway/rest patterns and TypeScript support.", "https://github.com/discordeno/discordeno", "Discord", tags = listOf("typescript", "gateway", "library")),
                RecommendedRepo("abalabahaha/eris", "Established Node Discord library for alternate gateway and sharding ideas.", "https://github.com/abalabahaha/eris", "Discord", tags = listOf("library", "gateway", "sharding")),
                RecommendedRepo("discord-player/discord-player", "Music playback framework useful for queue and audio-command structure ideas.", "https://github.com/discord-player/discord-player", "Discord", tags = listOf("music", "audio", "queues")),
                RecommendedRepo("saiteja-madha/discord-js-bot", "Multipurpose Discord bot covering moderation, music, tickets, and utility features.", "https://github.com/saiteja-madha/discord-js-bot", "Discord", tags = listOf("multipurpose", "moderation", "music")),
                RecommendedRepo("discord-tickets/bot", "Self-hosted ticket-management bot reference for support workflows.", "https://github.com/discord-tickets/bot", "Discord", tags = listOf("tickets", "support", "self-hosted")),
                RecommendedRepo("Androz2091/discord-music-bot", "Music bot project useful for queue, player, and command organization ideas.", "https://github.com/Androz2091/discord-music-bot", "Discord", tags = listOf("music", "queue", "player")),
                RecommendedRepo("AnIdiotsGuide/discordjs-bot-guide", "Community-written bot guide with practical snippets and setup notes.", "https://github.com/AnIdiotsGuide/discordjs-bot-guide", "Discord", tags = listOf("guide", "examples", "learning")),
            )),
            RepoCategory("Telegram and chat agents", "Telegraf, grammY, aiogram, Botkit, and Telegram bot templates across Node and Python.", HotPink, listOf(
                RecommendedRepo("telegraf/telegraf", "Telegram bot framework with middleware and context patterns.", "https://github.com/telegraf/telegraf", "Telegram", tags = listOf("telegram", "middleware", "typescript")),
                RecommendedRepo("grammyjs/grammY", "Modern Telegram Bot API framework with plugins and conversations.", "https://github.com/grammyjs/grammY", "Telegram", tags = listOf("telegram", "plugins", "conversations")),
                RecommendedRepo("python-telegram-bot/python-telegram-bot", "Popular Python Telegram bot framework for async handlers and examples.", "https://github.com/python-telegram-bot/python-telegram-bot", "Telegram", tags = listOf("python", "telegram", "async")),
                RecommendedRepo("aiogram/aiogram", "Async Python Telegram framework with router and FSM patterns.", "https://github.com/aiogram/aiogram", "Telegram", tags = listOf("python", "fsm", "async")),
                RecommendedRepo("yagop/node-telegram-bot-api", "Node Telegram Bot API library with straightforward polling/webhook examples.", "https://github.com/yagop/node-telegram-bot-api", "Telegram", tags = listOf("telegram", "node", "webhooks")),
                RecommendedRepo("tdlib/telegram-bot-api", "Official Telegram Bot API server for self-hosted API compatibility research.", "https://github.com/tdlib/telegram-bot-api", "Telegram", tags = listOf("bot-api", "server", "reference")),
                RecommendedRepo("howdyai/botkit", "Cross-platform conversation toolkit reference for dialog and adapter structure.", "https://github.com/howdyai/botkit", "Telegram", tags = listOf("conversation", "adapters", "legacy")),
                RecommendedRepo("TediCross/TediCross", "Telegram-to-Discord bridge showing multi-platform chat relay patterns.", "https://github.com/TediCross/TediCross", "Telegram", tags = listOf("bridge", "discord", "telegram")),
                RecommendedRepo("erkcet/awesome-telegram-bots", "Fresh awesome list for Telegram bot frameworks, libraries, Mini Apps, and examples.", "https://github.com/erkcet/awesome-telegram-bots", "Telegram", tags = listOf("awesome-list", "telegram", "examples")),
                RecommendedRepo("telegram-bot-sdk/awesome-telegram-bots", "Curated Telegram bot examples and resources for broad idea discovery.", "https://github.com/telegram-bot-sdk/awesome-telegram-bots", "Telegram", tags = listOf("awesome-list", "bots", "resources")),
            )),
            RepoCategory("Slack, Teams, and collaboration", "Slack Bolt apps, Teams samples, chatops frameworks, approvals, notifications, and meeting bots.", BotBladeTokens.GlitterGold, listOf(
                RecommendedRepo("slackapi/bolt-js", "Slack Bolt for JavaScript with event, command, OAuth, and receiver patterns.", "https://github.com/slackapi/bolt-js", "Slack", tags = listOf("slack", "bolt", "oauth")),
                RecommendedRepo("slackapi/node-slack-sdk", "Slack Web API and platform SDK reference for focused integrations.", "https://github.com/slackapi/node-slack-sdk", "Slack", tags = listOf("sdk", "web-api", "events")),
                RecommendedRepo("slackapi/bolt-python", "Python Bolt app examples useful for alternate runtime detection.", "https://github.com/slackapi/bolt-python", "Slack", tags = listOf("python", "bolt", "events")),
                RecommendedRepo("microsoft/BotBuilder-Samples", "Enterprise bot framework samples for future Blade Pack/template ideas.", "https://github.com/microsoft/BotBuilder-Samples", "Slack", tags = listOf("samples", "enterprise", "adapters")),
                RecommendedRepo("microsoft/botframework-sdk", "Bot Framework SDK source for Teams and enterprise adapter planning.", "https://github.com/microsoft/botframework-sdk", "Slack", tags = listOf("bot-framework", "teams", "sdk")),
                RecommendedRepo("microsoftgraph/msgraph-sdk-javascript", "Microsoft Graph SDK source for Teams-oriented future integration patterns.", "https://github.com/microsoftgraph/msgraph-sdk-javascript", "Slack", tags = listOf("teams", "graph", "sdk")),
                RecommendedRepo("errbotio/errbot", "ChatOps bot framework for adapter and plugin architecture ideas.", "https://github.com/errbotio/errbot", "Slack", tags = listOf("chatops", "plugins", "python")),
                RecommendedRepo("Vexa-ai/vexa", "Self-hostable meeting transcription bot API for Meet, Teams, and Zoom workflows.", "https://github.com/Vexa-ai/vexa", "Slack", tags = listOf("meeting-bot", "transcripts", "self-hosted")),
            )),
            RepoCategory("Session and private messengers", "Session is early enough that BotBlade should track SDKs, docs, and likely write first-party starter templates.", BabyBlue, listOf(
                RecommendedRepo("sessionjs/client", "Session.js client library for programmatic Session messenger usage and bot experiments.", "https://github.com/sessionjs/client", "Session", tags = listOf("session", "sdk", "typescript")),
                RecommendedRepo("sessionjs/docs", "Session.js documentation mirror for future BotBlade Session starter planning.", "https://github.com/sessionjs/docs", "Session", tags = listOf("docs", "session", "reference")),
                RecommendedRepo("VityaSchel/session-nodejs-bot", "Session Node.js bot mirror; useful as a compatibility signal while BotBlade designs its own starter.", "https://github.com/VityaSchel/session-nodejs-bot", "Session", tags = listOf("session", "bot", "node")),
                RecommendedRepo("VityaSchel/session-random-chat-bot", "Session chatbot mirror to inspect static project shape and detector clues.", "https://github.com/VityaSchel/session-random-chat-bot", "Session", tags = listOf("session", "chatbot", "reference")),
                RecommendedRepo("session-foundation/session-docs", "Official Session docs mirror for protocol, account, and privacy model research.", "https://github.com/session-foundation/session-docs", "Session", tags = listOf("session", "docs", "privacy")),
                RecommendedRepo("oxen-io/session-desktop", "Session desktop app source for reference-only UX and protocol research; do not vendor.", "https://github.com/oxen-io/session-desktop", "Session", tags = listOf("reference-only", "desktop", "privacy")),
                RecommendedRepo("signalapp/libsignal", "Private messaging crypto library reference for threat-model vocabulary, not direct bot import.", "https://github.com/signalapp/libsignal", "Session", tags = listOf("crypto", "privacy", "reference")),
                RecommendedRepo("matrix-org/matrix-js-sdk", "Matrix JavaScript SDK for federated chat adapter ideas and import contrast.", "https://github.com/matrix-org/matrix-js-sdk", "Session", tags = listOf("matrix", "chat", "sdk")),
            )),
            RepoCategory("Workflow and automation treasure", "n8n, Activepieces, Node-RED, Huginn, workflow JSON, and integration-piece references.", BabyBlue, listOf(
                RecommendedRepo("activepieces/activepieces", "Modern integration-piece architecture reference for BotBlade workflow adapters.", "https://github.com/activepieces/activepieces", "Workflow", tags = listOf("pieces", "automation", "integrations")),
                RecommendedRepo("n8n-io/n8n", "Workflow automation repository; import/reference-first because of license posture.", "https://github.com/n8n-io/n8n", "Workflow", tags = listOf("workflow", "json", "reference")),
                RecommendedRepo("Zie619/n8n-workflows", "Large searchable n8n workflow collection for JSON import experiments.", "https://github.com/Zie619/n8n-workflows", "Workflow", tags = listOf("n8n", "templates", "json")),
                RecommendedRepo("pxw3504k-web/awesome-n8n-workflows", "Large community n8n workflow collection for discovery and JSON import experiments.", "https://github.com/pxw3504k-web/awesome-n8n-workflows", "Workflow", tags = listOf("n8n", "templates", "json")),
                RecommendedRepo("enescingoz/awesome-n8n-templates", "Curated n8n templates for AI, productivity, Telegram, Slack, and more.", "https://github.com/enescingoz/awesome-n8n-templates", "Workflow", tags = listOf("n8n", "templates", "ai")),
                RecommendedRepo("node-red/node-red", "Legacy visual workflow reference for nodes, flows, and local editor ergonomics.", "https://github.com/node-red/node-red", "Workflow", tags = listOf("flows", "nodes", "legacy")),
                RecommendedRepo("huginn/huginn", "Legacy agent/workflow compatibility reference with event-driven automations.", "https://github.com/huginn/huginn", "Workflow", tags = listOf("agents", "events", "legacy")),
                RecommendedRepo("apache/airflow", "Workflow scheduling reference for DAG vocabulary and future import warnings.", "https://github.com/apache/airflow", "Workflow", tags = listOf("dag", "scheduling", "python")),
            )),
            RepoCategory("AI agent workbenches", "Agent templates, tool calling, RAG starters, eval harnesses, and prompt workflow repos.", HotPink, listOf(
                RecommendedRepo("langchain-ai/langchainjs", "JavaScript LangChain source for tools, agents, retrievers, and examples.", "https://github.com/langchain-ai/langchainjs", "AI Agents", tags = listOf("agents", "tools", "rag")),
                RecommendedRepo("langchain-ai/langgraphjs", "Graph-style agent orchestration patterns for durable task planning.", "https://github.com/langchain-ai/langgraphjs", "AI Agents", tags = listOf("graphs", "agents", "orchestration")),
                RecommendedRepo("openai/openai-node", "Official OpenAI Node SDK examples and type shapes for future adapters.", "https://github.com/openai/openai-node", "AI Agents", tags = listOf("openai", "sdk", "typescript")),
                RecommendedRepo("openai/openai-cookbook", "Cookbook examples for structured outputs, tools, RAG, and evaluation ideas.", "https://github.com/openai/openai-cookbook", "AI Agents", tags = listOf("cookbook", "patterns", "examples")),
                RecommendedRepo("run-llama/LlamaIndexTS", "TypeScript data-agent and indexing framework reference.", "https://github.com/run-llama/LlamaIndexTS", "AI Agents", tags = listOf("rag", "indexing", "typescript")),
                RecommendedRepo("crewAIInc/crewAI", "Python multi-agent orchestration reference for future pack detectors.", "https://github.com/crewAIInc/crewAI", "AI Agents", tags = listOf("multi-agent", "python", "tasks")),
                RecommendedRepo("microsoft/autogen", "Multi-agent conversation framework for adapter and orchestration research.", "https://github.com/microsoft/autogen", "AI Agents", tags = listOf("multi-agent", "python", "orchestration")),
                RecommendedRepo("microsoft/semantic-kernel", "Agent/plugin framework reference for skills, planners, and connectors.", "https://github.com/microsoft/semantic-kernel", "AI Agents", tags = listOf("plugins", "agents", "sdk")),
                RecommendedRepo("modelcontextprotocol/typescript-sdk", "MCP TypeScript SDK for future tool/agent connector planning.", "https://github.com/modelcontextprotocol/typescript-sdk", "AI Agents", tags = listOf("mcp", "tools", "typescript")),
            )),
            RepoCategory("Webhooks, workers, and APIs", "Tiny services, webhook receivers, queue workers, serverless functions, and templates.", BabyBlue, listOf(
                RecommendedRepo("fastify/fastify", "Fast Node.js framework with plugin and lifecycle patterns for webhook workers.", "https://github.com/fastify/fastify", "Workers", tags = listOf("fastify", "api", "plugins")),
                RecommendedRepo("expressjs/express", "Minimal web framework reference for common bot dashboards and webhooks.", "https://github.com/expressjs/express", "Workers", tags = listOf("express", "webhooks", "api")),
                RecommendedRepo("honojs/hono", "Tiny web framework for edge/serverless bot webhook handlers.", "https://github.com/honojs/hono", "Workers", tags = listOf("edge", "worker", "typescript")),
                RecommendedRepo("cloudflare/workers-sdk", "Workers tooling reference for deploy adapters and edge bot endpoints.", "https://github.com/cloudflare/workers-sdk", "Workers", tags = listOf("cloudflare", "edge", "deploy")),
                RecommendedRepo("vercel/examples", "Deployment examples for serverless handlers and framework adapters.", "https://github.com/vercel/examples", "Workers", tags = listOf("serverless", "examples", "deploy")),
                RecommendedRepo("fastapi/full-stack-fastapi-template", "Python API template with modern structure for generic Python imports.", "https://github.com/fastapi/full-stack-fastapi-template", "Workers", tags = listOf("python", "api", "template")),
                RecommendedRepo("nestjs/nest", "Structured Node backend framework with modules, guards, and dependency injection.", "https://github.com/nestjs/nest", "Workers", tags = listOf("node", "framework", "modules")),
                RecommendedRepo("microsoft/TypeScript-Node-Starter", "Classic TypeScript Node starter for generated worker layout comparisons.", "https://github.com/microsoft/TypeScript-Node-Starter", "Workers", tags = listOf("typescript", "starter", "node")),
            )),
            RepoCategory("DevOps and repo robots", "GitHub apps, issue bots, release helpers, CI notifiers, and maintenance automation.", BotBladeTokens.GlitterGold, listOf(
                RecommendedRepo("probot/probot", "GitHub App framework for issue, PR, and repo automation bots.", "https://github.com/probot/probot", "DevOps", tags = listOf("github-app", "automation", "webhooks")),
                RecommendedRepo("actions/toolkit", "GitHub Actions toolkit source for workflow automation concepts.", "https://github.com/actions/toolkit", "DevOps", tags = listOf("actions", "toolkit", "ci")),
                RecommendedRepo("semantic-release/semantic-release", "Automated release workflow reference for changelog and version bots.", "https://github.com/semantic-release/semantic-release", "DevOps", tags = listOf("release", "automation", "ci")),
                RecommendedRepo("renovatebot/renovate", "Dependency update bot source for policy-heavy automation design.", "https://github.com/renovatebot/renovate", "DevOps", tags = listOf("dependencies", "policy", "bot")),
                RecommendedRepo("dependabot/dependabot-core", "Dependency update engine reference for manifest scanning ideas.", "https://github.com/dependabot/dependabot-core", "DevOps", tags = listOf("dependencies", "security", "scanner")),
                RecommendedRepo("github/docs", "GitHub docs source for workflow, auth, and integration copy references.", "https://github.com/github/docs", "DevOps", tags = listOf("docs", "github", "workflows")),
                RecommendedRepo("googleapis/release-please", "Release PR automation reference for changelog and versioning flows.", "https://github.com/googleapis/release-please", "DevOps", tags = listOf("release", "automation", "changelog")),
                RecommendedRepo("argoproj/argo-workflows", "Workflow orchestration reference for build/deploy job concepts.", "https://github.com/argoproj/argo-workflows", "DevOps", tags = listOf("workflows", "kubernetes", "deploy")),
            )),
            RepoCategory("Ducky scripts and authorized labs", "BadUSB/HID payload libraries are import-only references for defensive training, lab testing, and detector design.", HotPink, listOf(
                RecommendedRepo("hak5/usbrubberducky-payloads", "Official USB Rubber Ducky payload library; inspect only in authorized labs.", "https://github.com/hak5/usbrubberducky-payloads", "Ducky", tags = listOf("ducky-script", "authorized-lab", "reference")),
                RecommendedRepo("hak5/bashbunny-payloads", "Official Bash Bunny payload repository for payload-shape detection and lab awareness.", "https://github.com/hak5/bashbunny-payloads", "Ducky", tags = listOf("badusb", "authorized-lab", "reference")),
                RecommendedRepo("hak5/omg-payloads", "Official O.MG payload repository for defensive review and metadata-only import experiments.", "https://github.com/hak5/omg-payloads", "Ducky", tags = listOf("hid", "authorized-lab", "metadata-only")),
                RecommendedRepo("hak5/sharkjack-payloads", "Official Shark Jack payload repository for network-lab detector research.", "https://github.com/hak5/sharkjack-payloads", "Ducky", tags = listOf("network-lab", "authorized", "reference")),
                RecommendedRepo("hak5/packetsquirrel-payloads", "Official Packet Squirrel payload repository for controlled network lab references.", "https://github.com/hak5/packetsquirrel-payloads", "Ducky", tags = listOf("network", "authorized-lab", "payloads")),
                RecommendedRepo("I-Am-Jakoby/Flipper-Zero-BadUSB", "Community BadUSB payload collection for lab-only static scanning and warnings.", "https://github.com/I-Am-Jakoby/Flipper-Zero-BadUSB", "Ducky", tags = listOf("flipper", "badusb", "lab-only")),
            )),
            RepoCategory("OSINT and threat intel", "Public-source intelligence, asset discovery, and threat-intel repos for authorized investigations and blue-team workflows.", BabyBlue, listOf(
                RecommendedRepo("jivoi/awesome-osint", "Curated OSINT tools and resources across search, social, threat intel, and research.", "https://github.com/jivoi/awesome-osint", "OSINT", tags = listOf("awesome-list", "osint", "threat-intel")),
                RecommendedRepo("lockfale/OSINT-Framework", "Interactive OSINT framework source for organizing public-source investigation links.", "https://github.com/lockfale/OSINT-Framework", "OSINT", tags = listOf("framework", "osint", "links")),
                RecommendedRepo("sherlock-project/sherlock", "Username search tool useful for authorized identity and brand-protection investigations.", "https://github.com/sherlock-project/sherlock", "OSINT", tags = listOf("username", "investigation", "python")),
                RecommendedRepo("soxoj/maigret", "Username enumeration tool with report output for authorized OSINT workflows.", "https://github.com/soxoj/maigret", "OSINT", tags = listOf("username", "reports", "osint")),
                RecommendedRepo("megadose/holehe", "Email-account presence checker for authorized exposure reviews.", "https://github.com/megadose/holehe", "OSINT", tags = listOf("email", "exposure", "authorized")),
                RecommendedRepo("lanmaster53/recon-ng", "Recon framework for controlled OSINT and reconnaissance workflows.", "https://github.com/lanmaster53/recon-ng", "OSINT", tags = listOf("recon", "framework", "authorized")),
                RecommendedRepo("projectdiscovery/subfinder", "Subdomain discovery tool for owned-domain asset inventory.", "https://github.com/projectdiscovery/subfinder", "OSINT", tags = listOf("subdomains", "asset-inventory", "go")),
                RecommendedRepo("owasp-amass/amass", "OWASP Amass attack-surface mapping for authorized external asset discovery.", "https://github.com/owasp-amass/amass", "OSINT", tags = listOf("attack-surface", "owasp", "authorized")),
            )),
            RepoCategory("Cybersecurity red-team references", "Offensive-security references stay metadata-only and should be used only in owned or explicitly authorized environments.", HotPink, listOf(
                RecommendedRepo("infosecn1nja/Red-Teaming-Toolkit", "Red-team tool catalog for authorized lab planning and detector taxonomy.", "https://github.com/infosecn1nja/Red-Teaming-Toolkit", "Red Team", tags = listOf("red-team", "awesome-list", "authorized")),
                RecommendedRepo("swisskyrepo/PayloadsAllTheThings", "Web security payload/reference catalog for authorized appsec labs and parser tests.", "https://github.com/swisskyrepo/PayloadsAllTheThings", "Red Team", tags = listOf("appsec", "payloads", "authorized")),
                RecommendedRepo("OWASP/wstg", "OWASP Web Security Testing Guide for ethical, scoped testing workflows.", "https://github.com/OWASP/wstg", "Red Team", tags = listOf("owasp", "testing-guide", "appsec")),
                RecommendedRepo("mitre/caldera", "Adversary-emulation platform reference for policy-gated lab workflows.", "https://github.com/mitre/caldera", "Red Team", tags = listOf("adversary-emulation", "lab", "mitre")),
                RecommendedRepo("redcanaryco/atomic-red-team", "Small atomic security tests for controlled detection engineering labs.", "https://github.com/redcanaryco/atomic-red-team", "Red Team", tags = listOf("atomic-tests", "detection", "authorized")),
                RecommendedRepo("carlospolop/PEASS-ng", "Privilege-escalation audit scripts; import only for authorized lab review.", "https://github.com/carlospolop/PEASS-ng", "Red Team", tags = listOf("audit", "privilege", "authorized")),
                RecommendedRepo("projectdiscovery/nuclei", "Template-driven scanner for owned assets and CI security checks.", "https://github.com/projectdiscovery/nuclei", "Red Team", tags = listOf("scanner", "templates", "authorized")),
                RecommendedRepo("Orange-Cyberdefense/arsenal", "Command notebook reference for training labs; do not auto-execute imports.", "https://github.com/Orange-Cyberdefense/arsenal", "Red Team", tags = listOf("notebook", "training", "metadata-only")),
            )),
            RepoCategory("Cybersecurity blue-team defense", "Defensive detection, hardening, DFIR, SIEM content, and audit tooling for BotBlade repair-card inspiration.", BabyBlue, listOf(
                RecommendedRepo("fabacab/awesome-cybersecurity-blueteam", "Curated blue-team tools and resources for detection and defensive operations.", "https://github.com/fabacab/awesome-cybersecurity-blueteam", "Blue Team", tags = listOf("awesome-list", "blue-team", "defense")),
                RecommendedRepo("SigmaHQ/sigma", "Generic SIEM detection rule format and public rules for detection engineering.", "https://github.com/SigmaHQ/sigma", "Blue Team", tags = listOf("sigma", "detections", "siem")),
                RecommendedRepo("Yara-Rules/rules", "YARA rule collection for malware-analysis and file-scanning research.", "https://github.com/Yara-Rules/rules", "Blue Team", tags = listOf("yara", "malware", "rules")),
                RecommendedRepo("Neo23x0/Loki", "IOC scanner reference for defensive host triage and rule-driven scanning.", "https://github.com/Neo23x0/Loki", "Blue Team", tags = listOf("ioc", "scanner", "dfir")),
                RecommendedRepo("Velocidex/velociraptor", "Endpoint visibility and DFIR platform reference for incident workflows.", "https://github.com/Velocidex/velociraptor", "Blue Team", tags = listOf("dfir", "endpoint", "visibility")),
                RecommendedRepo("wazuh/wazuh", "Open-source security platform reference for agents, alerts, and hardening checks.", "https://github.com/wazuh/wazuh", "Blue Team", tags = listOf("siem", "hardening", "alerts")),
                RecommendedRepo("osquery/osquery", "SQL-powered endpoint visibility reference for inventory and posture checks.", "https://github.com/osquery/osquery", "Blue Team", tags = listOf("endpoint", "inventory", "sql")),
                RecommendedRepo("Security-Onion-Solutions/securityonion", "Defensive monitoring distribution reference for SOC workflows and detections.", "https://github.com/Security-Onion-Solutions/securityonion", "Blue Team", tags = listOf("soc", "monitoring", "detections")),
            )),
            RepoCategory("Root-aware Android and terminals", "Root and terminal references are benefits-driven options: better local diagnostics, repo ownership fixes, package managers, and logs, but only with explicit user consent and policy gates.", BotBladeTokens.GlitterGold, listOf(
                RecommendedRepo("termux/termux-app", "External app integration reference for terminal and package-manager workflows; do not vendor GPL app code.", "https://github.com/termux/termux-app", "Root", tags = listOf("terminal", "external-app", "reference-only")),
                RecommendedRepo("termux/termux-api", "Termux API add-on reference for future intent-mediated Android integrations.", "https://github.com/termux/termux-api", "Root", tags = listOf("android", "intents", "reference")),
                RecommendedRepo("topjohnwu/Magisk", "Root ecosystem reference for understanding rooted-device capabilities and risk prompts.", "https://github.com/topjohnwu/Magisk", "Root", tags = listOf("root", "android", "reference-only")),
                RecommendedRepo("RikkaApps/Shizuku", "Android privileged API bridge reference for consent-gated local power-user flows.", "https://github.com/RikkaApps/Shizuku", "Root", tags = listOf("android", "privileged", "consent")),
                RecommendedRepo("topjohnwu/libsu", "Android root-shell library reference for future policy-gated root adapters, not current execution.", "https://github.com/topjohnwu/libsu", "Root", tags = listOf("root", "shell", "policy-gated")),
                RecommendedRepo("junrar/junrar", "Archive handling reference for root/backups and import edge-case planning.", "https://github.com/junrar/junrar", "Root", tags = listOf("archives", "backup", "reference")),
            )),
        )
        val Sources = listOf(
            Source("Discord TypeScript Starter", "slash-command bot with generated TypeScript project files", SourceLane.STARTER_TEMPLATE, "project_templates/simple_echo_bot"),
            Source("Moderation Toolkit", "moderation-oriented starter ready for rules and actions", SourceLane.STARTER_TEMPLATE, "project_templates/moderation_bot"),
            Source("Blank Workspace", "clean bot workspace for custom experiments", SourceLane.STARTER_TEMPLATE, "project_templates/blank_project"),
            Source("Import from Git", "register a Git repository as a BotBlade project", SourceLane.IMPORT_GIT, null),
            Source("Import ZIP", "register a ZIP archive as a managed BotBlade project", SourceLane.IMPORT_ZIP, null),
            Source("Open Folder", "register an Android workspace folder as a BotBlade project", SourceLane.OPEN_FOLDER, null),
            Source("Root Workspace", "anchor the selected repo/workspace root for monorepos, Git alignment, backups, and clearer audit boundaries", SourceLane.OPEN_ROOT_FOLDER, null),
            Source("Repair Existing", "create metadata and repair notes for an existing workspace", SourceLane.REPAIR_EXISTING, null),
            Source("Root Repair", "create a root-aware repair shell without device-root privileges or command execution", SourceLane.REPAIR_ROOT, null),
        )
    }

    private object ColorToken {
        val OnSurface = androidx.compose.ui.graphics.Color(0xFFEEF7FF)
    }
}
