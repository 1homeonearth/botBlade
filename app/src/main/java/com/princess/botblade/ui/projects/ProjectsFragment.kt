package com.princess.botblade.ui.projects

import android.content.Context
import android.os.Bundle
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
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.MainActivity
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import java.text.DateFormat

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
        val validName = name.isNotBlank() && name.matches(Regex("^[a-zA-Z0-9 _-]+$"))
        val available = projects.none { it.name.equals(name.trim(), ignoreCase = true) }
        val canCreate = validName && available && selected.templateDir != null

        fun openWizard() {
            showWizard = true
            step = 1
            name = ""
            selected = Sources.first()
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
                    Text("Create from a starter now. Git, ZIP, folder, and repair lanes stay visible while their endpoints mature.", color = Muted)
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
                            Button(onClick = { step = 2 }, enabled = selected.templateDir != null, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                            if (selected.templateDir == null) Text("${selected.title} is staged. Pick a starter template to create a project now.", color = Muted)
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
                                Button(onClick = { step = 3 }, enabled = canCreate, modifier = Modifier.weight(1f)) { Text("Review") }
                            }
                        }
                        else -> {
                            ReviewCard(name.trim(), selected)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) { Text("Back") }
                                Button(
                                    onClick = {
                                        val created = name.trim()
                                        repo.createProject(created, selected.templateDir ?: return@Button)
                                        projects = repo.listProjects()
                                        showWizard = false
                                        banner = "Created $created from ${selected.title}."
                                        projects.firstOrNull { it.name == created }?.let(onOpen)
                                    },
                                    modifier = Modifier.weight(1f),
                                ) { Text("Create + Open") }
                            }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                }
            }
        }
    }

    @Composable private fun Hero(banner: String, onAdd: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = RaisedPanel), border = BorderStroke(1.dp, HotPink), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Projects", color = BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your forge floor for importing, creating, repairing, scanning, and opening bot workspaces.", color = OnSurface)
                Text(banner, color = Muted)
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add bot project") }
            }
        }
    }

    @Composable private fun FlowRail() {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Workstation flow")
            listOf(
                "Create from starter templates now. Git, ZIP, folder, and repair imports are staged in this same flow.",
                "Open the generated project in Forge Editor, then scan, configure secrets, build, and run.",
                "Use Ops Deck for deployments, runtime logs, and container terminal once the bot is running.",
            ).forEach { line ->
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Stroke)) {
                    Text(line, color = OnSurface, modifier = Modifier.fillMaxWidth().padding(14.dp))
                }
            }
        }
    }

    @Composable private fun EmptyState(onAdd: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Stroke)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No bots in the forge yet", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Start with a Discord TypeScript bot, moderation toolkit, or blank workspace.", color = Muted)
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Create first project") }
            }
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
                    AssistChip(onClick = { onStatusClick(project) }, label = { Text(project.status) })
                    AssistChip(onClick = { onForgeReadyClick(project) }, label = { Text("Forge-ready") })
                    AssistChip(onClick = { onOpenClick(project) }, label = { Text("Open") })
                }
                Text("Tap to open editor. Long press or tap ⋯ for project actions.", color = Muted)
            }
        }
    }

    @Composable private fun ReviewCard(projectName: String, source: Source) {
        Card(colors = CardDefaults.cardColors(containerColor = BotBlack), border = BorderStroke(1.dp, BabyBlue)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Review forge order", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Name: $projectName", color = OnSurface)
                Text("Source: ${source.title}", color = OnSurface)
                Text("First action: create workspace, open Forge Editor, run Blade Pack scan.", color = Muted)
            }
        }
    }

    @Composable private fun SectionTitle(text: String) { Text(text, color = HotPink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }

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

    private data class Source(val title: String, val detail: String, val templateDir: String?)

    private companion object {
        const val PREFS = "botblade_workstation_flow"
        const val OPEN_ADD_PROJECT = "open_add_project"
        val BotBlack = Color(0xFF05060A); val Panel = Color(0xFF101522); val RaisedPanel = Color(0xFF151D2E); val Stroke = Color(0xFF2F405F)
        val BabyBlue = Color(0xFF8FD8FF); val HotPink = Color(0xFFFF3EA5); val OnSurface = Color(0xFFEEF7FF); val Muted = Color(0xFFAAB8CC)
        val Sources = listOf(
            Source("Discord TypeScript Starter", "slash-command bot with generated TypeScript project files", "project_templates/simple_echo_bot"),
            Source("Moderation Toolkit", "moderation-oriented starter ready for rules and actions", "project_templates/moderation_bot"),
            Source("Blank Workspace", "clean bot workspace for custom imports and experiments", "project_templates/blank_project"),
            Source("Import from Git", "clone and repair a repository through Forge Sync", null),
            Source("Import ZIP", "unpack an archive, scan it, then repair missing project metadata", null),
            Source("Open Folder", "select an Android workspace folder and turn it into a BotBlade project", null),
            Source("Repair Existing", "scan loose files and generate missing BotBlade metadata", null),
        )
    }
}
