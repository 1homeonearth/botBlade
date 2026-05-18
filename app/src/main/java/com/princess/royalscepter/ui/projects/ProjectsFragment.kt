package com.princess.royalscepter.ui.projects

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
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
import com.princess.royalscepter.data.model.BotCommand
import com.princess.royalscepter.data.model.BotProject
import com.princess.royalscepter.data.model.CommandCreateRequest
import com.princess.royalscepter.data.model.ProjectCreateRequest
import com.princess.royalscepter.data.repository.ProjectRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class ProjectsFragment : Fragment() {
    private val repository = ProjectRepository()
    private val commandNamePattern = Regex("^[a-z0-9_-]{1,32}$")

    private lateinit var activeProjectStore: ActiveProjectStore
    private lateinit var statusText: TextView
    private lateinit var projectsContainer: LinearLayout
    private lateinit var reloadButton: Button
    private lateinit var createButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_projects, container, false)

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
        val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 16, 48, 0) }
        val nameInput = EditText(requireContext()).apply { hint = getString(R.string.project_name_hint); setSingleLine(true) }
        val descriptionInput = EditText(requireContext()).apply { hint = getString(R.string.project_description_hint); minLines = 2 }
        val templateInput = EditText(requireContext()).apply { hint = getString(R.string.project_template_hint); setText("template_blank_discord_ts"); setSingleLine(true) }
        val runtimeInput = EditText(requireContext()).apply { hint = getString(R.string.project_runtime_hint); setText("node22"); setSingleLine(true) }
        form.addView(nameInput); form.addView(descriptionInput); form.addView(templateInput); form.addView(runtimeInput)

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
                        createProject(ProjectCreateRequest(name, descriptionInput.text.toString().trim(), templateInput.text.toString().trim().ifBlank { "template_blank_discord_ts" }, runtimeInput.text.toString().trim().ifBlank { "node22" }))
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

    private fun showCommands(project: BotProject) = viewLifecycleOwner.lifecycleScope.launch {
        statusText.text = getString(R.string.commands_loading)
        when (val result = repository.getProject(project.id)) {
            is ApiResult.Success -> showCommandsDialog(result.data)
            is ApiResult.Error -> renderError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun showCommandsDialog(project: BotProject) {
        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(32, 8, 32, 0) }
        val status = TextView(requireContext()).apply { text = getString(R.string.commands_loaded, project.commands.size) }
        content.addView(status)
        project.commands.forEach { command ->
            content.addView(TextView(requireContext()).apply {
                text = "/${command.name} — ${command.description}\n${command.handler.kind}: ${command.handler.content ?: "custom handler placeholder"}"
            })
            val actions = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL }
            actions.addView(Button(requireContext()).apply { text = getString(R.string.edit_command); setOnClickListener { showCommandEditor(project, command) } })
            actions.addView(Button(requireContext()).apply { text = getString(R.string.delete_command); setOnClickListener { confirmDeleteCommand(project, command) } })
            content.addView(actions)
        }
        content.addView(Button(requireContext()).apply { text = getString(R.string.create_command); setOnClickListener { showCommandEditor(project, null) } })
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.project_commands_title, project.name))
            .setView(content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    private fun showCommandEditor(project: BotProject, command: BotCommand?) {
        val form = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(48, 8, 48, 0) }
        val nameInput = EditText(requireContext()).apply { hint = getString(R.string.command_name_hint); setSingleLine(true); setText(command?.name.orEmpty()) }
        val descriptionInput = EditText(requireContext()).apply { hint = getString(R.string.command_description_hint); setSingleLine(true); setText(command?.description.orEmpty()) }
        val contentInput = EditText(requireContext()).apply { hint = getString(R.string.command_response_hint); minLines = 3; setText(command?.handler?.content.orEmpty()) }
        val customHandlerCheck = CheckBox(requireContext()).apply { text = getString(R.string.command_custom_handler); isChecked = command?.handler?.kind == "custom_typescript_placeholder" }
        val ephemeralCheck = CheckBox(requireContext()).apply { text = getString(R.string.command_ephemeral); isChecked = command?.handler?.ephemeral ?: true }
        form.addView(nameInput); form.addView(descriptionInput); form.addView(contentInput); form.addView(customHandlerCheck); form.addView(ephemeralCheck)

        AlertDialog.Builder(requireContext())
            .setTitle(if (command == null) R.string.create_command else R.string.edit_command)
            .setView(form)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(if (command == null) R.string.create_command else R.string.save_command, null)
            .create()
            .apply {
                setOnShowListener {
                    getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        val request = validateCommandForm(nameInput, descriptionInput, contentInput, customHandlerCheck.isChecked, ephemeralCheck.isChecked) ?: return@setOnClickListener
                        dismiss()
                        saveCommand(project.id, command?.id, request)
                    }
                }
            }
            .show()
    }

    private fun validateCommandForm(nameInput: EditText, descriptionInput: EditText, contentInput: EditText, customHandler: Boolean, ephemeral: Boolean): CommandCreateRequest? {
        val name = nameInput.text.toString().trim()
        val description = descriptionInput.text.toString().trim()
        val content = contentInput.text.toString().trim()
        var valid = true
        if (!commandNamePattern.matches(name)) { nameInput.error = getString(R.string.command_name_error); valid = false }
        if (description.length !in 1..100) { descriptionInput.error = getString(R.string.command_description_error); valid = false }
        if (!customHandler && content.isBlank()) { contentInput.error = getString(R.string.command_content_error); valid = false }
        return if (valid) CommandCreateRequest(name, description, if (customHandler) "custom_typescript_placeholder" else "static_response", content, ephemeral) else null
    }

    private fun saveCommand(projectId: String, commandId: String?, request: CommandCreateRequest) = viewLifecycleOwner.lifecycleScope.launch {
        statusText.text = getString(R.string.commands_saving)
        val result = if (commandId == null) repository.createCommand(projectId, request) else repository.updateCommand(projectId, commandId, request)
        when (result) {
            is ApiResult.Success -> { Toast.makeText(requireContext(), getString(R.string.command_saved, result.data.name), Toast.LENGTH_SHORT).show(); loadProjects() }
            is ApiResult.Error -> renderError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun confirmDeleteCommand(project: BotProject, command: BotCommand) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.delete_command)
            .setMessage(getString(R.string.delete_command_confirm, command.name))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteCommand(project.id, command) }
            .show()
    }

    private fun deleteCommand(projectId: String, command: BotCommand) = viewLifecycleOwner.lifecycleScope.launch {
        val id = command.id ?: return@launch
        statusText.text = getString(R.string.commands_deleting)
        when (val result = repository.deleteCommand(projectId, id)) {
            is ApiResult.Success -> { Toast.makeText(requireContext(), getString(R.string.command_deleted, command.name), Toast.LENGTH_SHORT).show(); loadProjects() }
            is ApiResult.Error -> renderError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun renderLoading() { setButtonsEnabled(false); statusText.text = getString(R.string.projects_loading_message) }

    private fun renderProjects(projects: List<BotProject>) {
        setButtonsEnabled(true)
        projectsContainer.removeAllViews()
        if (projects.isEmpty()) { statusText.text = getString(R.string.projects_empty_message); return }
        statusText.text = getString(R.string.projects_loaded_message, projects.size)
        projects.forEach { project -> projectsContainer.addView(createProjectCard(project)) }
    }

    private fun renderSelection(selectedProject: BotProject) {
        (0 until projectsContainer.childCount).map { projectsContainer.getChildAt(it) }.forEach { card ->
            if (card is MaterialCardView) card.strokeWidth = if (card.tag == selectedProject.id) 4 else 0
        }
    }

    private fun renderError(message: String) { setButtonsEnabled(true); projectsContainer.removeAllViews(); statusText.text = getString(R.string.projects_error_message, message) }

    private fun createProjectCard(project: BotProject): View {
        val card = MaterialCardView(requireContext()).apply { tag = project.id; radius = 20f; cardElevation = 2f; useCompatPadding = true; strokeWidth = if (activeProjectStore.getActiveProjectId() == project.id) 4 else 0; setOnClickListener { selectProject(project) } }
        val content = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(28, 24, 28, 24) }
        content.addView(TextView(requireContext()).apply { text = project.name; textSize = 20f })
        content.addView(TextView(requireContext()).apply { text = project.description.ifBlank { getString(R.string.project_no_description) } })
        content.addView(TextView(requireContext()).apply { text = getString(R.string.project_runtime_value, project.runtime) })
        content.addView(TextView(requireContext()).apply { text = getString(R.string.project_command_count_value, project.commands.size) })
        content.addView(TextView(requireContext()).apply { text = getString(R.string.project_github_value, project.github?.let { "${it.owner}/${it.repo}" } ?: getString(R.string.github_not_linked)) })
        content.addView(TextView(requireContext()).apply { text = getString(R.string.project_command_registration_value, project.discord.commandRegistration) })
        commandRegistrationWarnings(project).forEach { warning ->
            content.addView(TextView(requireContext()).apply { text = warning })
        }
        if (project.archivedAt != null) content.addView(TextView(requireContext()).apply { text = getString(R.string.project_archived_value, project.archivedAt) })
        content.addView(TextView(requireContext()).apply { text = getString(R.string.project_updated_value, project.updatedAt) })
        content.addView(Button(requireContext()).apply { text = getString(R.string.manage_commands); isAllCaps = false; setOnClickListener { showCommands(project) } })
        card.addView(content)
        return card
    }

    private fun commandRegistrationWarnings(project: BotProject): List<String> {
        val warnings = mutableListOf<String>()
        val mode = project.discord.commandRegistration
        if ((mode == "guild" || mode == "global") && project.discord.applicationId.isNullOrBlank()) {
            warnings.add(getString(R.string.project_missing_application_id_warning))
        }
        if (mode == "guild" && project.discord.defaultGuildId.isNullOrBlank()) {
            warnings.add(getString(R.string.project_missing_guild_id_warning))
        }
        return warnings
    }

    private fun setButtonsEnabled(enabled: Boolean) { reloadButton.isEnabled = enabled; createButton.isEnabled = enabled }
}
