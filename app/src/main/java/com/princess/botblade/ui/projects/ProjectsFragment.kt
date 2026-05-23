// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.projects  // line 7: executes this statement as part of this file's behavior

import android.os.Bundle  // line 9: executes this statement as part of this file's behavior
import androidx.compose.foundation.ExperimentalFoundationApi  // line 10: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Arrangement  // line 11: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Box  // line 12: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Column  // line 13: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxSize  // line 14: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxWidth  // line 15: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.padding  // line 16: executes this statement as part of this file's behavior
import androidx.compose.foundation.combinedClickable  // line 17: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.LazyColumn  // line 18: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.items  // line 19: executes this statement as part of this file's behavior
import androidx.compose.material.icons.Icons  // line 20: executes this statement as part of this file's behavior
import androidx.compose.material.icons.filled.MoreVert  // line 21: executes this statement as part of this file's behavior
import androidx.compose.material3.*  // line 22: executes this statement as part of this file's behavior
import androidx.compose.runtime.*  // line 23: executes this statement as part of this file's behavior
import androidx.compose.ui.Alignment  // line 24: executes this statement as part of this file's behavior
import androidx.compose.ui.Modifier  // line 25: executes this statement as part of this file's behavior
import androidx.compose.ui.text.font.FontWeight  // line 26: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.ComposeView  // line 27: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.dp  // line 28: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 29: executes this statement as part of this file's behavior
import com.princess.botblade.MainActivity  // line 30: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.LocalProjectRepository  // line 31: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.BotBladeTheme  // line 32: executes this statement as part of this file's behavior
import com.princess.botblade.ui.theme.isDynamicColorEnabled  // line 33: executes this statement as part of this file's behavior
import java.text.DateFormat  // line 34: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Row  // line 35: executes this statement as part of this file's behavior

class ProjectsFragment : Fragment() {  // line 37: executes this statement as part of this file's behavior
    private lateinit var localProjectRepository: LocalProjectRepository  // line 38: executes this statement as part of this file's behavior

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); localProjectRepository = LocalProjectRepository(requireContext()) }  // line 40: executes this statement as part of this file's behavior
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?) = ComposeView(requireContext()).apply {  // line 41: executes this statement as part of this file's behavior
        setContent { BotBladeTheme(useDynamicColor = isDynamicColorEnabled(requireContext())) { ProjectsScreen(::openProject) } }  // line 42: executes this statement as part of this file's behavior
    }  // line 43: executes this statement as part of this file's behavior

    private fun openProject(project: LocalProjectRepository.LocalProjectSummary) {  // line 45: executes this statement as part of this file's behavior
        (activity as? MainActivity)?.showEditorForProject(project.name)  // line 46: executes this statement as part of this file's behavior
    }  // line 47: executes this statement as part of this file's behavior

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)  // line 49: executes this statement as part of this file's behavior
    @Composable  // line 50: executes this statement as part of this file's behavior
    private fun ProjectsScreen(onOpen: (LocalProjectRepository.LocalProjectSummary) -> Unit) {  // line 51: executes this statement as part of this file's behavior
        var showWizard by remember { mutableStateOf(false) }  // line 52: executes this statement as part of this file's behavior
        var step by remember { mutableIntStateOf(1) }  // line 53: executes this statement as part of this file's behavior
        var projectName by remember { mutableStateOf("") }  // line 54: executes this statement as part of this file's behavior
        var template by remember { mutableStateOf("simple_echo_bot") }  // line 55: executes this statement as part of this file's behavior
        var projects by remember { mutableStateOf(localProjectRepository.listProjects()) }  // line 56: executes this statement as part of this file's behavior
        var menuProject by remember { mutableStateOf<LocalProjectRepository.LocalProjectSummary?>(null) }  // line 57: executes this statement as part of this file's behavior
        val templateOptions = mapOf("simple_echo_bot" to "Simple Echo Bot", "moderation_bot" to "Moderation Bot", "blank_project" to "Blank Project")  // line 58: executes this statement as part of this file's behavior

        Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showWizard = true; step = 1 }) { Text("+") } }) { padding ->  // line 60: executes this statement as part of this file's behavior
            if (projects.isEmpty()) {  // line 61: executes this statement as part of this file's behavior
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {  // line 62: executes this statement as part of this file's behavior
                    Text("No projects yet — tap + to create one.")  // line 63: executes this statement as part of this file's behavior
                }  // line 64: executes this statement as part of this file's behavior
            } else {  // line 65: executes this statement as part of this file's behavior
                LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {  // line 66: executes this statement as part of this file's behavior
                    items(projects, key = { it.id }) { project ->  // line 67: executes this statement as part of this file's behavior
                        Card(  // line 68: executes this statement as part of this file's behavior
                            modifier = Modifier.fillMaxWidth().combinedClickable(  // line 69: executes this statement as part of this file's behavior
                                onClick = { onOpen(project) },  // line 70: executes this statement as part of this file's behavior
                                onLongClick = { menuProject = project },  // line 71: executes this statement as part of this file's behavior
                            ),  // line 72: executes this statement as part of this file's behavior
                        ) {  // line 73: executes this statement as part of this file's behavior
                            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {  // line 74: executes this statement as part of this file's behavior
                                Text(project.name, fontWeight = FontWeight.Bold)  // line 75: executes this statement as part of this file's behavior
                                Text("Last modified: ${DateFormat.getDateTimeInstance().format(project.lastModified)}")  // line 76: executes this statement as part of this file's behavior
                                AssistChip(onClick = {}, label = { Text(project.status) })  // line 77: executes this statement as part of this file's behavior
                            }  // line 78: executes this statement as part of this file's behavior
                        }  // line 79: executes this statement as part of this file's behavior
                    }  // line 80: executes this statement as part of this file's behavior
                }  // line 81: executes this statement as part of this file's behavior
            }  // line 82: executes this statement as part of this file's behavior
        }  // line 83: executes this statement as part of this file's behavior
        menuProject?.let { project ->  // line 84: executes this statement as part of this file's behavior
            DropdownMenu(expanded = true, onDismissRequest = { menuProject = null }) {  // line 85: executes this statement as part of this file's behavior
                DropdownMenuItem(text = { Text("Rename") }, onClick = {  // line 86: executes this statement as part of this file's behavior
                    val renamed = localProjectRepository.renameProject(project.id, "${project.name}-renamed")  // line 87: executes this statement as part of this file's behavior
                    if (renamed) projects = localProjectRepository.listProjects()  // line 88: executes this statement as part of this file's behavior
                    menuProject = null  // line 89: executes this statement as part of this file's behavior
                })  // line 90: executes this statement as part of this file's behavior
                DropdownMenuItem(text = { Text("Delete") }, onClick = {  // line 91: executes this statement as part of this file's behavior
                    localProjectRepository.deleteProject(project.id)  // line 92: executes this statement as part of this file's behavior
                    projects = localProjectRepository.listProjects()  // line 93: executes this statement as part of this file's behavior
                    menuProject = null  // line 94: executes this statement as part of this file's behavior
                })  // line 95: executes this statement as part of this file's behavior
            }  // line 96: executes this statement as part of this file's behavior
        }  // line 97: executes this statement as part of this file's behavior

        if (showWizard) ModalBottomSheet(onDismissRequest = { showWizard = false }) {  // line 99: executes this statement as part of this file's behavior
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {  // line 100: executes this statement as part of this file's behavior
                Text("Create Project (Step $step of 3)")  // line 101: executes this statement as part of this file's behavior
                when (step) {  // line 102: executes this statement as part of this file's behavior
                    1 -> {  // line 103: executes this statement as part of this file's behavior
                        OutlinedTextField(projectName, { projectName = it }, label = { Text("Project name") })  // line 104: executes this statement as part of this file's behavior
                        Button(onClick = {  // line 105: executes this statement as part of this file's behavior
                            if (projectName.isNotBlank() && projectName.matches(Regex("^[a-zA-Z0-9 _-]+$"))) step = 2  // line 106: executes this statement as part of this file's behavior
                        }) { Text("Next") }  // line 107: executes this statement as part of this file's behavior
                    }  // line 108: executes this statement as part of this file's behavior
                    2 -> {  // line 109: executes this statement as part of this file's behavior
                        templateOptions.forEach { (k, v) ->  // line 110: executes this statement as part of this file's behavior
                            Row { RadioButton(selected = template == k, onClick = { template = k }); Text(v) }  // line 111: executes this statement as part of this file's behavior
                        }  // line 112: executes this statement as part of this file's behavior
                        Button(onClick = { step = 3 }) { Text("Next") }  // line 113: executes this statement as part of this file's behavior
                    }  // line 114: executes this statement as part of this file's behavior
                    else -> {  // line 115: executes this statement as part of this file's behavior
                        Text("Name: $projectName")  // line 116: executes this statement as part of this file's behavior
                        Text("Template: ${templateOptions[template]}")  // line 117: executes this statement as part of this file's behavior
                        Button(onClick = {  // line 118: executes this statement as part of this file's behavior
                            localProjectRepository.createProject(projectName, "project_templates/$template")  // line 119: executes this statement as part of this file's behavior
                            projects = localProjectRepository.listProjects()  // line 120: executes this statement as part of this file's behavior
                            showWizard = false  // line 121: executes this statement as part of this file's behavior
                            (activity as? MainActivity)?.showEditorForProject(projectName)  // line 122: executes this statement as part of this file's behavior
                        }) { Text("Create") }  // line 123: executes this statement as part of this file's behavior
                    }  // line 124: executes this statement as part of this file's behavior
                }  // line 125: executes this statement as part of this file's behavior
            }  // line 126: executes this statement as part of this file's behavior
        }  // line 127: executes this statement as part of this file's behavior
    }  // line 128: executes this statement as part of this file's behavior
}  // line 129: executes this statement as part of this file's behavior
