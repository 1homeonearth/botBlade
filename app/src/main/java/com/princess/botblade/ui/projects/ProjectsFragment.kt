package com.princess.botblade.ui.projects

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import com.princess.botblade.MainActivity
import com.princess.botblade.data.model.BotProject
import com.princess.botblade.data.repository.LocalProjectRepository
import com.princess.botblade.ui.theme.BotBladeTheme

class ProjectsFragment : Fragment() {
    private lateinit var localProjectRepository: LocalProjectRepository

    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); localProjectRepository = LocalProjectRepository(requireContext()) }
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme { ProjectsScreen(::openProject) } }
    }

    private fun openProject(project: BotProject) { Toast.makeText(requireContext(), "Open ${project.name}", Toast.LENGTH_SHORT).show() }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun ProjectsScreen(onOpen: (BotProject) -> Unit) {
        var showWizard by remember { mutableStateOf(false) }
        var step by remember { mutableIntStateOf(1) }
        var projectName by remember { mutableStateOf("") }
        var template by remember { mutableStateOf("simple_echo_bot") }
        val templateOptions = mapOf("simple_echo_bot" to "Simple Echo Bot", "moderation_bot" to "Moderation Bot", "blank_project" to "Blank Project")

        Scaffold(floatingActionButton = { FloatingActionButton(onClick = { showWizard = true; step = 1 }) { Text("+") } }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {}
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
                            showWizard = false
                            (activity as? MainActivity)?.showEditorForProject(projectName)
                        }) { Text("Create") }
                    }
                }
            }
        }
    }
}
