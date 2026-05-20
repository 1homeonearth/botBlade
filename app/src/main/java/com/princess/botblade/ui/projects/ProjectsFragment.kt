package com.princess.botblade.ui.projects

import android.os.Bundle
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.BotProject
import com.princess.botblade.data.repository.ProjectRepository
import com.princess.botblade.ui.theme.BotBladeTheme
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {
    private val repository = ProjectRepository()
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?) = ComposeView(requireContext()).apply {
        setContent { BotBladeTheme { ProjectsScreen(::openProject, ::createProject, ::renameProject, ::deleteProject) } }
    }

    private fun openProject(project: BotProject) { Toast.makeText(requireContext(), "Open ${project.name}", Toast.LENGTH_SHORT).show() }
    private fun createProject() { Toast.makeText(requireContext(), "Create project", Toast.LENGTH_SHORT).show() }
    private fun renameProject(project: BotProject) { Toast.makeText(requireContext(), "Rename ${project.name}", Toast.LENGTH_SHORT).show() }
    private fun deleteProject(project: BotProject) { Toast.makeText(requireContext(), "Delete ${project.name}", Toast.LENGTH_SHORT).show() }

    @Composable
    private fun ProjectsScreen(onOpen: (BotProject) -> Unit, onCreate: () -> Unit, onRename: (BotProject) -> Unit, onDelete: (BotProject) -> Unit) {
        var projects by remember { mutableStateOf<List<BotProject>>(emptyList()) }
        androidx.compose.runtime.LaunchedEffect(Unit) {
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = repository.listProjects()) { is ApiResult.Success -> projects = result.data; else -> Unit }
            }
        }
        Scaffold(floatingActionButton = { FloatingActionButton(onClick = onCreate) { Text("+") } }) { padding ->
            LazyColumn(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(projects) { project -> ProjectCard(project, onOpen, onRename, onDelete) }
            }
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    private fun ProjectCard(project: BotProject, onOpen: (BotProject) -> Unit, onRename: (BotProject) -> Unit, onDelete: (BotProject) -> Unit) {
        var menu by remember { mutableStateOf(false) }
        Card(Modifier.fillMaxWidth().padding(horizontal = 12.dp).combinedClickable(onClick = { onOpen(project) }, onLongClick = { menu = true })) {
            Box(Modifier.padding(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(project.name, style = MaterialTheme.typography.headlineMedium)
                    Text(if (project.archivedAt == null) "Running" else "Stopped", style = MaterialTheme.typography.titleSmall)
                    Text("Modified: ${project.updatedAt}", style = MaterialTheme.typography.bodySmall)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Rename") }, onClick = { menu = false; onRename(project) })
                    DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; onDelete(project) })
                }
            }
        }
    }
}
