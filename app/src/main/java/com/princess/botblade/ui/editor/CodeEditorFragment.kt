package com.princess.botblade.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.botblade.R
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.ProjectFileContent
import com.princess.botblade.data.model.ProjectFileSummary
import com.princess.botblade.data.repository.EditorRepository
import com.princess.botblade.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class CodeEditorFragment : Fragment() {
    private val repository = EditorRepository()
    private var projectId: String? = null
    private var selectedFilePath: String? = null
    private lateinit var status: TextView
    private lateinit var fileList: LinearLayout
    private lateinit var metadata: TextView
    private lateinit var editor: EditText
    private lateinit var saveButton: Button
    private lateinit var revertButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_code_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        status = view.findViewById(R.id.editor_status)
        fileList = view.findViewById(R.id.file_list_container)
        metadata = view.findViewById(R.id.selected_file_metadata)
        editor = view.findViewById(R.id.file_content_editor)
        saveButton = view.findViewById(R.id.save_file_button)
        revertButton = view.findViewById(R.id.revert_file_button)

        val hasProject = projectId != null
        view.findViewById<Button>(R.id.generate_project_button).isEnabled = hasProject
        view.findViewById<Button>(R.id.start_build_button).isEnabled = hasProject
        editor.isEnabled = false
        updateSelectionButtons()
        if (!hasProject) {
            status.text = getString(R.string.select_project_first)
            return
        }

        view.findViewById<Button>(R.id.generate_project_button).setOnClickListener { generateProject() }
        view.findViewById<Button>(R.id.start_build_button).setOnClickListener { startBuild() }
        saveButton.setOnClickListener { saveSelectedFile() }
        revertButton.setOnClickListener { selectedFilePath?.let { loadFile(it) } }
        editor.doAfterTextChanged { updateSelectionButtons() }
        loadFiles()
    }

    private fun loadFiles() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Loading files…")
        when (val result = repository.listFiles(id)) {
            is ApiResult.Success -> renderFiles(result.data)
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun generateProject() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Generating project files…")
        when (val result = repository.generateProject(id)) {
            is ApiResult.Success -> renderFiles(result.data, "Generated ${result.data.size} file(s).")
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun loadFile(path: String) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Loading $path…")
        when (val result = repository.getFile(id, path)) {
            is ApiResult.Success -> renderFile(result.data)
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun saveSelectedFile() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        val path = selectedFilePath ?: return@launch
        setBusy("Saving $path…")
        when (val result = repository.saveFile(id, path, editor.text.toString())) {
            is ApiResult.Success -> {
                renderFile(result.data)
                status.text = getString(R.string.file_saved, result.data.path)
            }
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun startBuild() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Starting build…")
        when (val result = repository.createBuild(id)) {
            is ApiResult.Success -> status.text = getString(R.string.build_started, result.data.buildId, result.data.status)
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun renderFiles(files: List<ProjectFileSummary>, message: String = "Loaded ${files.size} file(s).") {
        fileList.removeAllViews()
        if (files.isEmpty()) {
            status.text = "No files yet. Tap Generate Project."
            return
        }
        files.forEach { file ->
            val row = Button(requireContext()).apply {
                text = file.path
                isAllCaps = false
                setOnClickListener { loadFile(file.path) }
            }
            fileList.addView(row)
        }
        status.text = message
    }

    private fun renderFile(file: ProjectFileContent) {
        selectedFilePath = file.path
        editor.isEnabled = file.editable
        editor.setText(file.content)
        metadata.text = getString(R.string.generated_status, file.generated.toString(), file.editable.toString(), file.updatedAt)
        status.text = getString(R.string.file_loaded, file.path)
        updateSelectionButtons()
    }

    private fun setBusy(message: String) { status.text = message }
    private fun setError(message: String) { status.text = "Error: $message" }
    private fun updateSelectionButtons() {
        val enabled = projectId != null && selectedFilePath != null && editor.isEnabled
        saveButton.isEnabled = enabled
        revertButton.isEnabled = enabled
    }
}
