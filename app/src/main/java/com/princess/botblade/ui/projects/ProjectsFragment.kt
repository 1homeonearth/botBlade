package com.princess.botblade.ui.projects

import android.os.Bundle
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    private lateinit var localProjectRepository: LocalProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        localProjectRepository = LocalProjectRepository(requireContext())
    }

    override fun onCreateView(
        inflater: android.view.LayoutInflater,
        container: android.view.ViewGroup?,
        savedInstanceState: Bundle?,
    ) = ComposeView(requireContext()).apply {
        setContent {
            BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) {
                ProjectsScreen(::openProject)
            }
        }
    }

    private fun openProject(project: LocalProjectRepository.LocalProjectSummary) {
        (activity as? MainActivity)?.showEditorForProject(project.name)
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
    @Composable
    private fun ProjectsScreen(onOpen: (LocalProjectRepository.LocalProjectSummary) -> Unit) {
        var showWizard by remember { mutableStateOf(false) }
        var step by remember { mutableIntStateOf(1) }
        var projectName by remember { mutableStateOf("") }
        var selectedSource by remember { mutableStateOf(ProjectSources.first()) }
        var projects by remember { mutableStateOf(localProjectRepository.listProjects()) }
        var menuProject by remember { mutableStateOf<LocalProjectRepository.LocalProjectSummary?>(null) }
        var banner by remember { mutableStateOf("Import, create, repair, and open bot workspaces from one forge floor.") }
        val projectNameValid = projectName.isNotBlank() && projectName.matches(Regex("^[a-zA-Z0-9 _-]+$"))
        val projectNameAvailable = projects.none { it.name.equals(projectName.trim(), ignoreCase = true) }
        val canCreate = projectNameValid && projectNameAvailable && selectedSource.templateAssetDir != null

        Scaffold(
            containerColor = BotBlack,
            floatingActionButton = {
                FloatingActionButton(
                    containerColor = HotPink,
                    contentColor = BotBlack,
                    onClick = {
                        showWizard = true
                        step = 1
                        projectName = ""
                        selectedSource = ProjectSources.first()
                    },
                ) { Text("+", fontWeight = FontWeight.Bold) }
            },
        ) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().background(BotBlack).padding(padding).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { ProjectsHero(banner = banner, onAdd = { showWizard = true; step = 1 }) }
                item { ProjectActionRail() }
                item { SectionTitle("Workspaces") }
                if (projects.isEmpty()) {
                    item { EmptyProjectState(onAdd = { showWizard = true; step = 1 }) }
                } else {
                    items(projects, key = { it.id }) { project ->
                        ProjectCard(project = project, onOpen = { onOpen(project) }, onMenu = { menuProject = project })
                    }
                }
            }
        }

        menuProject?.let { project ->
            DropdownMenu(expanded = true, onDismissRequest = { menuProject = null }) {
                DropdownMenuItem(text = { Text("Open in Forge Editor") }, onClick = {
                    onOpen(project)
                    menuProject = null
                })
                DropdownMenuItem(text = { Text("Quick rename") }, onClick = {
                    val renamed = localProjectRepository.renameProject(project.id, "${project.name}-renamed")
                    if (renamed) {
                        projects = localProjectRepository.listProjects()
                        banner = "Renamed ${project.name}."
                    }
                    menuProject = null
                })
                DropdownMenuItem(text = { Text("Delete") }, onClick = {
                    localProjectRepository.deleteProject(project.id)
                    projects = localProjectRepository.listProjects()
                    banner = "Deleted ${project.name}."
                    menuProject = null
                })
            }
        }

        if (showWizard) {
            ModalBottomSheet(onDismissRequest = { showWizard = false }, containerColor = Panel) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Add Project", color = BabyBlue, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Create a starter now or choose a commercial import lane. Git, ZIP, folder, and repair lanes stay visible while their import endpoints mature.", color = Muted)
                    when (step) {
                        1 -> {
                            AddSourceList(selectedSource = selectedSource, onSelect = { selectedSource = it })
                            Button(onClick = { step = 2 }, enabled = selectedSource.templateAssetDir != null, modifier = Modifier.fillMaxWidth()) { Text("Continue") }
                            if (selectedSource.templateAssetDir == null) {
                                Text("${selectedSource.title} is staged as a product lane. Use a starter template now, then import/repair plugs into this exact surface.", color = Muted)
                            }
                        }
                        2 -> {
                            OutlinedTextField(
                                value = projectName,
                                onValueChange = { projectName = it },
                                label = { Text("Project name") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Text(
                                text = when {
                                    projectName.isBlank() -> "Use letters, numbers, spaces, dashes, or underscores."
                                    !projectNameValid -> "Project names can use letters, numbers, spaces, dashes, and underscores."
                                    !projectNameAvailable -> "That project already exists."
                                    else -> "Ready to forge ${projectName.trim()}."
                                },
                                color = if (projectNameValid && projectNameAvailable) BabyBlue else Muted,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 1 }, modifier = Modifier.weight(1f)) { Text("Back") }
                                Button(onClick = { step = 3 }, enabled = canCreate, modifier = Modifier.weight(1f)) { Text("Review") }
                            }
                        }
                        else -> {
                            ReviewCard(projectName = projectName.trim(), source = selectedSource)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = { step = 2 }, modifier = Modifier.weight(1f)) { Text("Back") }
                                Button(
                                    onClick = {
                                        val createdName = projectName.trim()
                                        val templateDir = selectedSource.templateAssetDir ?: return@Button
                                        localProjectRepository.createProject(createdName, templateDir)
                                        projects = localProjectRepository.listProjects()
                                        showWizard = false
                                        banner = "Created $createdName from ${selectedSource.title}."
                                        (activity as? MainActivity)?.showEditorForProject(createdName)
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

    @Composable
    private fun ProjectsHero(banner: String, onAdd: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = RaisedPanel), border = BorderStroke(1.dp, HotPink), shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Projects", color = BabyBlue, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("Your forge floor for importing, creating, repairing, scanning, and opening bot workspaces.", color = OnSurface)
                Text(banner, color = Muted)
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Add bot project") }
            }
        }
    }

    @Composable
    private fun ProjectActionRail() {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionTitle("Commercial workflow")
            listOf(
                "Import from Git, ZIP, folder, upstream snapshot, or Blade Pack template.",
                "Run First Check to detect runtime, package manager, secrets, scripts, entrypoints, and repair hints.",
                "Open in Forge Editor for files, diagnostics, Bot Map, builds, logs, and container terminal.",
            ).forEach { line ->
                Surface(color = Panel, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, Stroke)) {
                    Text(line, color = OnSurface, modifier = Modifier.fillMaxWidth().padding(14.dp))
                }
            }
        }
    }

    @Composable
    private fun EmptyProjectState(onAdd: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Stroke)) {
            Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("No bots in the forge yet", color = BabyBlue, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Start with a Discord TypeScript bot, moderation toolkit, or blank workspace. Import lanes are visible so the page matches the finished product direction.", color = Muted)
                Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("Create first project") }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ProjectCard(project: LocalProjectRepository.LocalProjectSummary, onOpen: () -> Unit, onMenu: () -> Unit) {
        Card(modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onOpen, onLongClick = onMenu), colors = CardDefaults.cardColors(containerColor = Panel), border = BorderStroke(1.dp, Stroke), shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(project.name, color = BabyBlue, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Last modified: ${DateFormat.getDateTimeInstance().format(project.lastModified)}", color = Muted)
                    }
                    TextButton(onClick = onMenu) { Text("⋯", color = HotPink) }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AssistChip(onClick = {}, label = { Text(project.status) })
                    AssistChip(onClick = {}, label = { Text("Forge-ready") })
                    AssistChip(onClick = {}, label = { Text("Open") })
                }
                Text("Tap to open editor. Long press for project actions.", color = Muted)
            }
        }
    }

    @Composable
    private fun AddSourceList(selectedSource: ProjectSource, onSelect: (ProjectSource) -> Unit) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ProjectSources.forEach { source ->
                FilterChip(
                    selected = selectedSource == source,
                    onClick = { onSelect(source) },
                    label = {
                        Column(Modifier.padding(4.dp)) {
                            Text(source.title, fontWeight = FontWeight.Bold)
                            Text(source.detail)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    @Composable
    private fun ReviewCard(projectName: String, source: ProjectSource) {
        Card(colors = CardDefaults.cardColors(containerColor = BotBlack), border = BorderStroke(1.dp, BabyBlue)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Review forge order", color = BabyBlue, fontWeight = FontWeight.Bold)
                Text("Name: $projectName", color = OnSurface)
                Text("Source: ${source.title}", color = OnSurface)
                Text("First action: create workspace, open Forge Editor, run Blade Pack scan.", color = Muted)
            }
        }
    }

    @Composable
    private fun SectionTitle(text: String) {
        Text(text, color = HotPink, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }

    private data class ProjectSource(val title: String, val detail: String, val templateAssetDir: String?)

    private companion object {
        val BotBlack = Color(0xFF05060A)
        val Panel = Color(0xFF101522)
        val RaisedPanel = Color(0xFF151D2E)
        val Stroke = Color(0xFF2F405F)
        val BabyBlue = Color(0xFF8FD8FF)
        val HotPink = Color(0xFFFF3EA5)
        val OnSurface = Color(0xFFEEF7FF)
        val Muted = Color(0xFFAAB8CC)

        val ProjectSources = listOf(
            ProjectSource("Discord TypeScript Starter", "slash-command bot with generated TypeScript project files", "project_templates/simple_echo_bot"),
            ProjectSource("Moderation Toolkit", "moderation-oriented starter ready for rules and actions", "project_templates/moderation_bot"),
            ProjectSource("Blank Workspace", "clean bot workspace for custom imports and experiments", "project_templates/blank_project"),
            ProjectSource("Import from Git", "clone and repair a repository through Forge Sync", null),
            ProjectSource("Import ZIP", "unpack an archive, scan it, then repair missing project metadata", null),
            ProjectSource("Open Folder", "select an Android workspace folder and turn it into a BotBlade project", null),
            ProjectSource("Repair Existing", "scan loose files and generate missing BotBlade metadata", null),
        )
    }
}