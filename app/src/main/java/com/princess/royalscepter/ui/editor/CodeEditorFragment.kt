package com.princess.royalscepter.ui.editor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.ProjectFileContent
import com.princess.royalscepter.data.model.ProjectFileSummary
import com.princess.royalscepter.data.repository.EditorRepository
import com.princess.royalscepter.data.store.ActiveProjectStore
import com.princess.royalscepter.ui.common.addBodyText
import com.princess.royalscepter.ui.common.copyToClipboard
import com.princess.royalscepter.ui.common.materialListCard
import com.princess.royalscepter.ui.common.visibleWhen
import kotlinx.coroutines.launch

class CodeEditorFragment : Fragment() {
    private val repository = EditorRepository()
    private var projectId: String? = null
    private var selectedFilePath: String? = null
    private lateinit var status: TextView
    private lateinit var progress: ProgressBar
    private lateinit var fileList: LinearLayout
    private lateinit var metadata: TextView
    private lateinit var editor: EditText
    private lateinit var generateButton: Button
    private lateinit var buildButton: Button
    private lateinit var saveButton: Button
    private lateinit var revertButton: Button
    private lateinit var copyPathButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_code_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        status = view.findViewById(R.id.editor_status)
        progress = view.findViewById(R.id.editor_progress)
        fileList = view.findViewById(R.id.file_list_container)
        metadata = view.findViewById(R.id.selected_file_metadata)
        editor = view.findViewById(R.id.file_content_editor)
        generateButton = view.findViewById(R.id.generate_project_button)
        buildButton = view.findViewById(R.id.start_build_button)
        saveButton = view.findViewById(R.id.save_file_button)
        revertButton = view.findViewById(R.id.revert_file_button)
        copyPathButton = view.findViewById(R.id.copy_file_path_button)

        val hasProject = projectId != null
        generateButton.isEnabled = hasProject
        buildButton.isEnabled = hasProject
        editor.isEnabled = false
        updateSelectionButtons()
        generateButton.setOnClickListener { generateProject() }
        buildButton.setOnClickListener { startBuild() }
        saveButton.setOnClickListener { saveSelectedFile() }
        revertButton.setOnClickListener { selectedFilePath?.let { loadFile(it) } }
        copyPathButton.setOnClickListener { selectedFilePath?.let { requireContext().copyToClipboard(getString(R.string.copy_path), it) } }
        editor.doAfterTextChanged { updateSelectionButtons() }
        if (!hasProject) {
            status.text = getString(R.string.select_project_first)
            renderEmpty(getString(R.string.select_project_first))
            return
        }
        loadFiles()
    }

    private fun loadFiles() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy(getString(R.string.loading_files))
        when (val result = repository.listFiles(id)) {
            is ApiResult.Success -> renderFiles(result.data)
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun generateProject() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy(getString(R.string.generating_project_files))
        when (val result = repository.generateProject(id)) {
            is ApiResult.Success -> renderFiles(result.data, getString(R.string.generated_files_message, result.data.size))
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun loadFile(path: String) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy(getString(R.string.loading_file_path, path))
        when (val result = repository.getFile(id, path)) {
            is ApiResult.Success -> renderFile(result.data)
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun saveSelectedFile() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        val path = selectedFilePath ?: return@launch
        setBusy(getString(R.string.saving_file_path, path))
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
        setBusy(getString(R.string.starting_build))
        when (val result = repository.createBuild(id)) {
            is ApiResult.Success -> { setIdle(); status.text = getString(R.string.build_started, result.data.buildId, result.data.status) }
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun renderFiles(files: List<ProjectFileSummary>, message: String = getString(R.string.files_loaded_message, files.size)) {
        setIdle()
        fileList.removeAllViews()
        if (files.isEmpty()) {
            renderEmpty(getString(R.string.editor_empty_files))
            return
        }
        files.forEach { file ->
            fileList.addView(requireContext().materialListCard {
                addBodyText(getString(R.string.file_path_label, file.path))
                addBodyText(getString(R.string.generated_status, file.generated.toString(), file.editable.toString(), file.updatedAt))
                addView(Button(requireContext()).apply { text = getString(R.string.copy_path); isAllCaps = false; setOnClickListener { requireContext().copyToClipboard(getString(R.string.copy_path), file.path) } })
                addView(Button(requireContext()).apply { text = getString(R.string.file_loaded, file.path); isAllCaps = false; setOnClickListener { loadFile(file.path) } })
            })
        }
        status.text = message
    }

    private fun renderEmpty(message: String) {
        fileList.removeAllViews()
        fileList.addView(requireContext().materialListCard {
            addBodyText(message)
            addView(Button(requireContext()).apply { text = getString(R.string.generate_project); isEnabled = projectId != null; setOnClickListener { generateProject() } })
        })
    }

    private fun renderFile(file: ProjectFileContent) {
        setIdle()
        selectedFilePath = file.path
        editor.isEnabled = file.editable
        editor.setText(file.content)
        metadata.text = getString(R.string.generated_status, file.generated.toString(), file.editable.toString(), file.updatedAt)
        status.text = getString(R.string.file_loaded, file.path)
        updateSelectionButtons()
    }

    private fun setBusy(message: String) {
        progress.visibleWhen(true)
        generateButton.isEnabled = false
        buildButton.isEnabled = false
        saveButton.isEnabled = false
        revertButton.isEnabled = false
        copyPathButton.isEnabled = false
        status.text = message
    }

    private fun setIdle() {
        progress.visibleWhen(false)
        val hasProject = projectId != null
        generateButton.isEnabled = hasProject
        buildButton.isEnabled = hasProject
        updateSelectionButtons()
    }

    private fun setError(message: String) {
        setIdle()
        status.text = getString(R.string.error_value, message)
        fileList.addView(requireContext().materialListCard {
            addBodyText(getString(R.string.error_value, message))
            addView(Button(requireContext()).apply { text = getString(R.string.retry); setOnClickListener { loadFiles() } })
        })
    }

    private fun updateSelectionButtons() {
        val enabled = projectId != null && selectedFilePath != null && editor.isEnabled
        saveButton.isEnabled = enabled
        revertButton.isEnabled = enabled
        copyPathButton.isEnabled = selectedFilePath != null
    }
}
