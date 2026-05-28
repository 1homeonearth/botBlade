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
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.princess.botblade.R
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.BuildSummary
import com.princess.botblade.data.model.ProjectFileContent
import com.princess.botblade.data.model.ProjectFileSummary
import com.princess.botblade.data.model.ProjectScanResponse
import com.princess.botblade.data.repository.EditorRepository
import com.princess.botblade.data.store.ActiveProjectStore
import kotlinx.coroutines.launch

class CodeEditorFragment : Fragment() {
    private val repository = EditorRepository()
    private var projectId: String? = null
    private var selectedFilePath: String? = null
    private var loadedContent: String = ""
    private var allFiles: List<ProjectFileSummary> = emptyList()
    private val recentFiles = mutableListOf<String>()

    private lateinit var status: TextView
    private lateinit var fileSearch: EditText
    private lateinit var fileList: LinearLayout
    private lateinit var recentFilesContainer: LinearLayout
    private lateinit var metadata: TextView
    private lateinit var currentFileTitle: TextView
    private lateinit var editor: EditText
    private lateinit var saveButton: Button
    private lateinit var revertButton: Button
    private lateinit var scanButton: Button
    private lateinit var firstCheckButton: Button
    private lateinit var openOpsButton: Button
    private lateinit var detectedPackCard: TextView
    private lateinit var missingSecretsCard: TextView
    private lateinit var problemsContainer: LinearLayout
    private lateinit var buildOutput: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_code_editor, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        projectId = ActiveProjectStore(requireContext()).getActiveProjectId()
        status = view.findViewById(R.id.editor_status)
        fileSearch = view.findViewById(R.id.file_search_input)
        fileList = view.findViewById(R.id.file_list_container)
        recentFilesContainer = view.findViewById(R.id.recent_files_container)
        metadata = view.findViewById(R.id.selected_file_metadata)
        currentFileTitle = view.findViewById(R.id.current_file_title)
        editor = view.findViewById(R.id.file_content_editor)
        saveButton = view.findViewById(R.id.save_file_button)
        revertButton = view.findViewById(R.id.revert_file_button)
        scanButton = view.findViewById(R.id.scan_project_button)
        firstCheckButton = view.findViewById(R.id.run_first_check_button)
        openOpsButton = view.findViewById(R.id.open_ops_button)
        detectedPackCard = view.findViewById(R.id.detected_pack_card)
        missingSecretsCard = view.findViewById(R.id.missing_secrets_card)
        problemsContainer = view.findViewById(R.id.problems_container)
        buildOutput = view.findViewById(R.id.build_output_text)

        val hasProject = projectId != null
        view.findViewById<Button>(R.id.generate_project_button).isEnabled = hasProject
        view.findViewById<Button>(R.id.start_build_button).isEnabled = hasProject
        scanButton.isEnabled = hasProject
        firstCheckButton.isEnabled = hasProject
        openOpsButton.isEnabled = false
        editor.isEnabled = false
        updateSelectionButtons()
        renderRecentFiles()
        renderProblems(null)

        if (!hasProject) {
            status.text = getString(R.string.select_project_first)
            buildOutput.text = "Select a project before editing, scanning, or building."
            return
        }

        view.findViewById<Button>(R.id.generate_project_button).setOnClickListener { generateProject() }
        view.findViewById<Button>(R.id.start_build_button).setOnClickListener { startBuild() }
        scanButton.setOnClickListener { scanProject() }
        firstCheckButton.setOnClickListener { runFirstCheck() }
        saveButton.setOnClickListener { saveSelectedFile() }
        revertButton.setOnClickListener { selectedFilePath?.let { loadFile(it) } }
        openOpsButton.setOnClickListener { openOps() }
        missingSecretsCard.setOnClickListener { openVault() }
        missingSecretsCard.isClickable = true
        fileSearch.doAfterTextChanged { renderFilteredFiles() }
        editor.doAfterTextChanged { updateSelectionButtons() }
        loadFiles()
    }

    private fun openVault() {
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_settings
    }

    private fun openOps() {
        activity?.findViewById<BottomNavigationView>(R.id.bottom_navigation)?.selectedItemId = R.id.navigation_deployments
    }

    private fun scanProject() = lifecycleScope.launch {
        performScan()
    }

    private suspend fun performScan(): ProjectScanResponse? {
        val id = projectId ?: return null
        setBusy("Scanning project…")
        return when (val result = repository.scanProject(id)) {
            is ApiResult.Success -> {
                renderScanCards(result.data)
                renderProblems(result.data)
                status.text = "Scan completed. Diagnostics are ready below."
                result.data
            }
            is ApiResult.Error -> {
                setError(result.message)
                buildOutput.text = "Scan failed: ${result.message}"
                null
            }
            ApiResult.Loading -> null
        }
    }

    private fun loadFiles() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Loading project explorer…")
        when (val result = repository.listFiles(id)) {
            is ApiResult.Success -> {
                allFiles = result.data.sortedBy { it.path }
                renderFilteredFiles("Loaded ${allFiles.size} file(s).")
            }
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun generateProject() = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Generating project files…")
        when (val result = repository.generateProject(id)) {
            is ApiResult.Success -> {
                allFiles = result.data.sortedBy { it.path }
                renderFilteredFiles("Generated ${result.data.size} file(s).")
            }
            is ApiResult.Error -> setError(result.message)
            ApiResult.Loading -> Unit
        }
    }

    private fun loadFile(path: String) = lifecycleScope.launch {
        val id = projectId ?: return@launch
        setBusy("Opening $path…")
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
        performBuild()
    }

    private fun runFirstCheck() = lifecycleScope.launch {
        buildOutput.text = "First check started. Running scan, then build…"
        performScan()
        performBuild()
    }

    private suspend fun performBuild(): BuildSummary? {
        val id = projectId ?: return null
        setBusy("Starting build…")
        buildOutput.text = "Build request sent to local backend…"
        return when (val result = repository.createBuild(id)) {
            is ApiResult.Success -> {
                renderBuild(result.data)
                status.text = getString(R.string.build_started, result.data.buildId, result.data.status)
                openOpsButton.isEnabled = true
                result.data
            }
            is ApiResult.Error -> {
                setError(result.message)
                buildOutput.text = "Build failed to start: ${result.message}"
                null
            }
            ApiResult.Loading -> null
        }
    }

    private fun renderScanCards(scan: ProjectScanResponse) {
        val top = if (scan.recommendedPackId == "unknown") {
            null
        } else {
            scan.matches.firstOrNull { it.id == scan.recommendedPackId } ?: scan.matches.firstOrNull()
        }
        if (top == null) {
            detectedPackCard.text = "Detected pack\nUnknown project\nConfidence: weak\nRuntime: unknown"
            missingSecretsCard.text = "Missing secrets\nNo required secrets detected.\nTap to open Vault."
            return
        }
        val runtimeLabel = when {
            top.id.contains("python") -> "Python"
            top.id.contains("n8n") -> "Workflow"
            else -> "Node 22"
        }
        detectedPackCard.text = "Detected pack\n${top.name}\nConfidence: ${top.confidence}, ${top.score}\nRuntime: $runtimeLabel"
        val missing = top.requiredSecrets
        missingSecretsCard.text = if (missing.isEmpty()) {
            "Missing secrets\nNo required secrets detected.\nTap to open Vault."
        } else {
            "Missing secrets\n${missing.joinToString(", ")}\nNext action: tap here to add secrets in Vault."
        }
    }

    private fun renderFilteredFiles(message: String? = null) {
        fileList.removeAllViews()
        val query = fileSearch.text.toString().trim()
        val shownFiles = if (query.isBlank()) {
            allFiles
        } else {
            allFiles.filter { it.path.contains(query, ignoreCase = true) }
        }
        if (allFiles.isEmpty()) {
            status.text = "No files yet. Tap Generate Project."
            return
        }
        if (shownFiles.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "No files match '$query'."
                setTextColor(resources.getColor(R.color.botblade_on_surface_muted, null))
                setPadding(12, 12, 12, 12)
            }
            fileList.addView(empty)
        } else {
            shownFiles.forEach { file ->
                val row = Button(requireContext()).apply {
                    text = fileLabel(file)
                    isAllCaps = false
                    setOnClickListener { loadFile(file.path) }
                }
                fileList.addView(row)
            }
        }
        status.text = message ?: "${allFiles.size} file(s) loaded • ${shownFiles.size} shown."
    }

    private fun renderFile(file: ProjectFileContent) {
        selectedFilePath = file.path
        loadedContent = file.content
        editor.isEnabled = file.editable
        editor.setText(file.content)
        currentFileTitle.text = if (file.editable) "Open file: ${file.path}" else "Preview only: ${file.path}"
        metadata.text = buildString {
            append(getString(R.string.generated_status, file.generated.toString(), file.editable.toString(), file.updatedAt))
            append("\nSize: ${file.size} bytes")
            if (!file.editable) append("\nThis file is not editable from the mobile workbench.")
        }
        addRecentFile(file.path)
        renderRecentFiles()
        status.text = getString(R.string.file_loaded, file.path)
        updateSelectionButtons()
    }

    private fun renderRecentFiles() {
        recentFilesContainer.removeAllViews()
        if (recentFiles.isEmpty()) {
            val empty = TextView(requireContext()).apply {
                text = "Recent files appear here after you open code."
                setTextColor(resources.getColor(R.color.botblade_on_surface_muted, null))
                setPadding(8, 8, 8, 8)
            }
            recentFilesContainer.addView(empty)
            return
        }
        recentFiles.forEach { path ->
            val row = Button(requireContext()).apply {
                text = path
                isAllCaps = false
                setOnClickListener { loadFile(path) }
            }
            recentFilesContainer.addView(row)
        }
    }

    private fun renderProblems(scan: ProjectScanResponse?) {
        problemsContainer.removeAllViews()
        if (scan == null) {
            addProblem("Scan required", "Run Scan to detect Blade Pack, secrets, runtime, and repair needs.", "Scan") { scanProject() }
            return
        }
        val top = scan.matches.firstOrNull { it.id == scan.recommendedPackId } ?: scan.matches.firstOrNull()
        if (top == null || scan.recommendedPackId == "unknown") {
            addProblem("Unknown project", "BotBlade could not confidently match this workspace. Check package files or use Repair Existing from Projects.", "Rescan") { scanProject() }
            return
        }
        addProblem("Blade Pack", "${top.name} detected with ${top.confidence} confidence (${top.score}).", "Rescan") { scanProject() }
        if (top.requiredSecrets.isEmpty()) {
            addProblem("Secrets", "No required secrets were detected for this pack.", "Open Vault") { openVault() }
        } else {
            addProblem("Missing secrets", top.requiredSecrets.joinToString(", "), "Open Vault") { openVault() }
        }
        addProblem("First check", "Build after scan to validate dependencies, TypeScript, and generated project structure.", "Build") { startBuild() }
    }

    private fun renderBuild(build: BuildSummary) {
        buildOutput.text = buildString {
            appendLine("Build ${build.buildId}")
            appendLine("Status: ${build.status}")
            appendLine("Project: ${build.projectId}")
            appendLine("Started: ${build.startedAt ?: "unknown"}")
            appendLine("Finished: ${build.finishedAt ?: "pending"}")
            if (!build.errorMessage.isNullOrBlank()) appendLine("Error: ${build.errorMessage}")
            appendLine()
            appendLine("Next: open Ops Deck to inspect runtime, deployment targets, and logs.")
        }
    }

    private fun addProblem(title: String, detail: String, action: String, onClick: () -> Unit) {
        val row = Button(requireContext()).apply {
            text = "$title\n$detail\nAction: $action"
            isAllCaps = false
            setOnClickListener { onClick() }
        }
        problemsContainer.addView(row)
    }

    private fun addRecentFile(path: String) {
        recentFiles.remove(path)
        recentFiles.add(0, path)
        while (recentFiles.size > 5) recentFiles.removeAt(recentFiles.lastIndex)
    }

    private fun fileLabel(file: ProjectFileSummary): String = buildString {
        append(file.path)
        append("\n")
        append(if (file.editable) "editable" else "preview only")
        append(" • ${file.size} bytes")
        if (file.generated) append(" • generated")
    }

    private fun setBusy(message: String) { status.text = message }
    private fun setError(message: String) { status.text = "Error: $message" }
    private fun updateSelectionButtons() {
        val hasEditableFile = projectId != null && selectedFilePath != null && editor.isEnabled
        val dirty = hasEditableFile && editor.text.toString() != loadedContent
        saveButton.isEnabled = dirty
        revertButton.isEnabled = dirty
        if (selectedFilePath == null) {
            currentFileTitle.text = "No file open"
        } else if (dirty) {
            currentFileTitle.text = "Open file: $selectedFilePath • unsaved changes"
        } else {
            currentFileTitle.text = "Open file: $selectedFilePath"
        }
    }
}