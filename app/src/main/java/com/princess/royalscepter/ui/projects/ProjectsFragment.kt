package com.princess.royalscepter.ui.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.ProjectCreateRequest
import com.princess.royalscepter.data.repository.ProjectRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {
    private val repository = ProjectRepository()

    private lateinit var activeProjectStore: ActiveProjectStore
    private lateinit var statusText: TextView
    private lateinit var projectsContainer: LinearLayout
    private lateinit var reloadButton: Button
    private lateinit var createButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_projects, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activeProjectStore = ActiveProjectStore(requireContext())
        statusText = view.findViewById(R.id.projects_status_text)
        projectsContainer = view.findViewById(R.id.projects_container)
        reloadButton = view.findViewById(R.id.reload_projects_button)
        createButton = view.findViewById(R.id.create_project_button)

        reloadButton.setOnClickListener { loadProjects() }
        createButton.setOnClickListener { showCreateProjectDialog() }
        loadProjects()
    }

    private fun loadProjects() {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.listProjects()) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> renderProjects(result.data)
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun showCreateProjectDialog() {
        val form = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 16, 48, 0)
        }
        val nameInput = EditText(requireContext()).apply {
            hint = getString(R.string.project_name_hint)
            setSingleLine(true)
        }
        val descriptionInput = EditText(requireContext()).apply {
            hint = getString(R.string.project_description_hint)
            minLines = 2
        }
        val templateInput = EditText(requireContext()).apply {
            hint = getString(R.string.project_template_hint)
            setText("template_blank_discord_ts")
            setSingleLine(true)
        }
        val runtimeInput = EditText(requireContext()).apply {
            hint = getString(R.string.project_runtime_hint)
            setText("node22")
            setSingleLine(true)
        }
        form.addView(nameInput)
        form.addView(descriptionInput)
        form.addView(templateInput)
        form.addView(runtimeInput)

        AlertDialog.Builder(requireContext())
            .setTitle(R.string.create_bot_project)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.create_bot_project, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val name = nameInput.text.toString().trim()
                        if (name.isBlank()) {
                            nameInput.error = getString(R.string.project_name_required)
                            return@setOnClickListener
                        }
                        dismiss()
                        createProject(
                            ProjectCreateRequest(
                                name = name,
                                description = descriptionInput.text.toString().trim(),
                                templateId = templateInput.text.toString().trim().ifBlank { "template_blank_discord_ts" },
                                runtime = runtimeInput.text.toString().trim().ifBlank { "node22" },
                            ),
                        )
                    }
                }
            }
            .show()
    }

    private fun createProject(request: ProjectCreateRequest) {
        viewLifecycleOwner.lifecycleScope.launch {
            renderLoading()
            when (val result = repository.createProject(request)) {
                ApiResult.Loading -> renderLoading()
                is ApiResult.Success -> {
                    activeProjectStore.setActiveProject(result.data.id, result.data.name)
                    Toast.makeText(requireContext(), getString(R.string.project_created_message, result.data.name), Toast.LENGTH_SHORT).show()
                    loadProjects()
                }
                is ApiResult.Error -> renderError(result.message)
            }
        }
    }

    private fun selectProject(project: BotProject) {
        activeProjectStore.setActiveProject(project.id, project.name)
        renderSelection(project)
        Toast.makeText(requireContext(), getString(R.string.active_project_selected, project.name), Toast.LENGTH_SHORT).show()
    }

    private fun renderLoading() {
        setButtonsEnabled(false)
        statusText.text = getString(R.string.projects_loading_message)
    }

    private fun renderProjects(projects: List<BotProject>) {
        setButtonsEnabled(true)
        projectsContainer.removeAllViews()
        if (projects.isEmpty()) {
            statusText.text = getString(R.string.projects_empty_message)
            return
        }
        statusText.text = getString(R.string.projects_loaded_message, projects.size)
        projects.forEach { project -> projectsContainer.addView(createProjectCard(project)) }
    }

    private fun renderSelection(selectedProject: BotProject) {
        val currentCards = (0 until projectsContainer.childCount).map { projectsContainer.getChildAt(it) }
        currentCards.forEach { card ->
            if (card is MaterialCardView) {
                card.strokeWidth = if (card.tag == selectedProject.id) 4 else 0
            }
        }
    }

    private fun renderError(message: String) {
        setButtonsEnabled(true)
        projectsContainer.removeAllViews()
        statusText.text = getString(R.string.projects_error_message, message)
    }

    private fun createProjectCard(project: BotProject): View {
        val card = MaterialCardView(requireContext()).apply {
            tag = project.id
            radius = 20f
            cardElevation = 2f
            useCompatPadding = true
            strokeWidth = if (activeProjectStore.getActiveProjectId() == project.id) 4 else 0
            setOnClickListener { selectProject(project) }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 24, 28, 24)
        }
        content.addView(TextView(requireContext()).apply {
            text = project.name
            textSize = 20f
        })
        content.addView(TextView(requireContext()).apply {
            text = project.description.ifBlank { getString(R.string.project_no_description) }
        })
        content.addView(TextView(requireContext()).apply {
            text = getString(R.string.project_runtime_value, project.runtime)
        })
        content.addView(TextView(requireContext()).apply {
            text = getString(R.string.project_command_registration_value, project.discord.commandRegistration)
        })
        if (project.archivedAt != null) {
            content.addView(TextView(requireContext()).apply { text = getString(R.string.project_archived_value, project.archivedAt) })
        }
        content.addView(TextView(requireContext()).apply {
            text = getString(R.string.project_updated_value, project.updatedAt)
        })
        card.addView(content)
        return card
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        reloadButton.isEnabled = enabled
        createButton.isEnabled = enabled
    }
}
