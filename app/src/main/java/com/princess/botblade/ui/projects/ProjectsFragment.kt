package com.princess.botblade.ui.projects

import android.os.Bundle
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.MainActivity
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.ui.theme.BotBladeTheme
import com.princess.botblade.ui.theme.isDynamicColorEnabled
import java.text.DateFormat
import androidx.compose.foundation.layout.Row

class ProjectsFragment : Fragment() {
    private lateinit var localProjectRepository: LocalProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); localProjectRepository = LocalProjectRepository(requireContext()) }
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) { ProjectsScreen(::openProject) } }
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
        var template by remember { mutableStateOf("simple_echo_bot") }
        var projects by remember { mutableStateOf(localProjectRepository.listProjects()) }
        var menuProject by remember { mutableStateOf<LocalProjectRepository.LocalProjectSummary?>(null) }
        val templateOptions = mapOf("simple_echo_bot" to "Simple Echo Bot", "moderation_bot" to "Moderation Bot", "blank_project" to "Blank Project")

        Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showWizard = true; step = 1 }) { Text("+") } }) { padding ->
            if (projects.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No projects yet — tap + to create one.")
                }
            } else {
                LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(projects, key = { it.id }) { project ->
                        Card(
                            modifier = Modifier.fillMaxWidth().combinedClickable(
                                onClick = { onOpen(project) },
                                onLongClick = { menuProject = project },
                            ),
                        ) {
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(project.name, fontWeight = FontWeight.Bold)
                                Text("Last modified: ${DateFormat.getDateTimeInstance().format(project.lastModified)}")
                                AssistChip(onClick = {}, label = { Text(project.status) })
                            }
                        }
                    }
                }
            }
        }
        menuProject?.let { project ->
            DropdownMenu(expanded = true, onDismissRequest = { menuProject = null }) {
                DropdownMenuItem(text = { Text("Rename") }, onClick = {
                    val renamed = localProjectRepository.renameProject(project.id, "${project.name}-renamed")
                    if (renamed) projects = localProjectRepository.listProjects()
                    menuProject = null
                })
                DropdownMenuItem(text = { Text("Delete") }, onClick = {
                    localProjectRepository.deleteProject(project.id)
                    projects = localProjectRepository.listProjects()
                    menuProject = null
                })
            }
        }

        if (showWizard) ModalBottomSheet(onDismissRequest = { showWizard = false }) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Create Project (Step $step of 3)")
                when (step) {
                    1 -> {
                        OutlinedTextField(projectName, { projectName = it }, label = { Text("Project name") })
                        Button(onClick = {
                            if (projectName.isNotBlank() && projectName.matches(Regex("^[a-zA-Z0-9 _-]+$"))) step = 2
                        }) { Text("Next") }
                    }
                    2 -> {
                        templateOptions.forEach { (k, v) ->
                            Row { RadioButton(selected = template == k, onClick = { template = k }); Text(v) }
                        }
                        Button(onClick = { step = 3 }) { Text("Next") }
                    }
                    else -> {
                        Text("Name: $projectName")
                        Text("Template: ${templateOptions[template]}")
                        Button(onClick = {
                            localProjectRepository.createProject(projectName, "project_templates/$template")
                            projects = localProjectRepository.listProjects()
                            showWizard = false
                            (activity as? MainActivity)?.showEditorForProject(projectName)
                        }) { Text("Create") }
                    }
                }
            }
        }
    }
}
