// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.editor  // line 7: executes this statement as part of this file's behavior

import android.os.Bundle  // line 9: executes this statement as part of this file's behavior
import android.view.LayoutInflater  // line 10: executes this statement as part of this file's behavior
import android.view.View  // line 11: executes this statement as part of this file's behavior
import android.view.ViewGroup  // line 12: executes this statement as part of this file's behavior
import android.widget.Button  // line 13: executes this statement as part of this file's behavior
import android.widget.EditText  // line 14: executes this statement as part of this file's behavior
import android.widget.LinearLayout  // line 15: executes this statement as part of this file's behavior
import android.widget.TextView  // line 16: executes this statement as part of this file's behavior
import androidx.core.widget.doAfterTextChanged  // line 17: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 18: executes this statement as part of this file's behavior
import androidx.lifecycle.lifecycleScope  // line 19: executes this statement as part of this file's behavior
import com.princess.botblade.R  // line 20: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiResult  // line 21: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileContent  // line 22: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectFileSummary  // line 23: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.ProjectScanResponse  // line 24: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.EditorRepository  // line 25: executes this statement as part of this file's behavior
import com.princess.botblade.data.store.ActiveProjectStore  // line 26: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 27: executes this statement as part of this file's behavior

class CodeEditorFragment : Fragment() {  // line 29: executes this statement as part of this file's behavior
    private val repository = EditorRepository()  // line 30: executes this statement as part of this file's behavior
    private var projectId: String? = null  // line 31: executes this statement as part of this file's behavior
    private var selectedFilePath: String? = null  // line 32: executes this statement as part of this file's behavior
    private lateinit var status: TextView  // line 33: executes this statement as part of this file's behavior
    private lateinit var fileList: LinearLayout  // line 34: executes this statement as part of this file's behavior
    private lateinit var metadata: TextView  // line 35: executes this statement as part of this file's behavior
    private lateinit var editor: EditText  // line 36: executes this statement as part of this file's behavior
    private lateinit var saveButton: Button  // line 37: executes this statement as part of this file's behavior
    private lateinit var revertButton: Button  // line 38: executes this statement as part of this file's behavior
    private lateinit var scanButton: Button  // line 39: executes this statement as part of this file's behavior
    private lateinit var firstCheckButton: Button  // line 40: executes this statement as part of this file's behavior
    private lateinit var detectedPackCard: TextView  // line 41: executes this statement as part of this file's behavior
    private lateinit var missingSecretsCard: TextView  // line 42: executes this statement as part of this file's behavior

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =  // line 44: executes this statement as part of this file's behavior
        inflater.inflate(R.layout.fragment_code_editor, container, false)  // line 45: executes this statement as part of this file's behavior

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {  // line 47: executes this statement as part of this file's behavior
        super.onViewCreated(view, savedInstanceState)  // line 48: executes this statement as part of this file's behavior
        projectId = ActiveProjectStore(requireContext()).getActiveProjectId()  // line 49: executes this statement as part of this file's behavior
        status = view.findViewById(R.id.editor_status)  // line 50: executes this statement as part of this file's behavior
        fileList = view.findViewById(R.id.file_list_container)  // line 51: executes this statement as part of this file's behavior
        metadata = view.findViewById(R.id.selected_file_metadata)  // line 52: executes this statement as part of this file's behavior
        editor = view.findViewById(R.id.file_content_editor)  // line 53: executes this statement as part of this file's behavior
        saveButton = view.findViewById(R.id.save_file_button)  // line 54: executes this statement as part of this file's behavior
        revertButton = view.findViewById(R.id.revert_file_button)  // line 55: executes this statement as part of this file's behavior
        scanButton = view.findViewById(R.id.scan_project_button)  // line 56: executes this statement as part of this file's behavior
        firstCheckButton = view.findViewById(R.id.run_first_check_button)  // line 57: executes this statement as part of this file's behavior
        detectedPackCard = view.findViewById(R.id.detected_pack_card)  // line 58: executes this statement as part of this file's behavior
        missingSecretsCard = view.findViewById(R.id.missing_secrets_card)  // line 59: executes this statement as part of this file's behavior

        val hasProject = projectId != null  // line 61: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.generate_project_button).isEnabled = hasProject  // line 62: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.start_build_button).isEnabled = hasProject  // line 63: executes this statement as part of this file's behavior
        scanButton.isEnabled = hasProject  // line 64: executes this statement as part of this file's behavior
        firstCheckButton.isEnabled = hasProject  // line 65: executes this statement as part of this file's behavior
        editor.isEnabled = false  // line 66: executes this statement as part of this file's behavior
        updateSelectionButtons()  // line 67: executes this statement as part of this file's behavior
        if (!hasProject) {  // line 68: executes this statement as part of this file's behavior
            status.text = getString(R.string.select_project_first)  // line 69: executes this statement as part of this file's behavior
            return  // line 70: executes this statement as part of this file's behavior
        }  // line 71: executes this statement as part of this file's behavior

        view.findViewById<Button>(R.id.generate_project_button).setOnClickListener { generateProject() }  // line 73: executes this statement as part of this file's behavior
        view.findViewById<Button>(R.id.start_build_button).setOnClickListener { startBuild() }  // line 74: executes this statement as part of this file's behavior
        scanButton.setOnClickListener { scanProject() }  // line 75: executes this statement as part of this file's behavior
        firstCheckButton.setOnClickListener { startBuild() }  // line 76: executes this statement as part of this file's behavior
        saveButton.setOnClickListener { saveSelectedFile() }  // line 77: executes this statement as part of this file's behavior
        revertButton.setOnClickListener { selectedFilePath?.let { loadFile(it) } }  // line 78: executes this statement as part of this file's behavior
        editor.doAfterTextChanged { updateSelectionButtons() }  // line 79: executes this statement as part of this file's behavior
        loadFiles()  // line 80: executes this statement as part of this file's behavior
    }  // line 81: executes this statement as part of this file's behavior

    private fun scanProject() = lifecycleScope.launch {  // line 83: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 84: executes this statement as part of this file's behavior
        setBusy("Scanning project…")  // line 85: executes this statement as part of this file's behavior
        when (val result = repository.scanProject(id)) {  // line 86: executes this statement as part of this file's behavior
            is ApiResult.Success -> {  // line 87: executes this statement as part of this file's behavior
                renderScanCards(result.data)  // line 88: executes this statement as part of this file's behavior
                status.text = "Scan completed."  // line 89: executes this statement as part of this file's behavior
            }  // line 90: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 91: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 92: executes this statement as part of this file's behavior
        }  // line 93: executes this statement as part of this file's behavior
    }  // line 94: executes this statement as part of this file's behavior

    private fun loadFiles() = lifecycleScope.launch {  // line 96: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 97: executes this statement as part of this file's behavior
        setBusy("Loading files…")  // line 98: executes this statement as part of this file's behavior
        when (val result = repository.listFiles(id)) {  // line 99: executes this statement as part of this file's behavior
            is ApiResult.Success -> renderFiles(result.data)  // line 100: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 101: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 102: executes this statement as part of this file's behavior
        }  // line 103: executes this statement as part of this file's behavior
    }  // line 104: executes this statement as part of this file's behavior

    private fun generateProject() = lifecycleScope.launch {  // line 106: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 107: executes this statement as part of this file's behavior
        setBusy("Generating project files…")  // line 108: executes this statement as part of this file's behavior
        when (val result = repository.generateProject(id)) {  // line 109: executes this statement as part of this file's behavior
            is ApiResult.Success -> renderFiles(result.data, "Generated ${result.data.size} file(s).")  // line 110: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 111: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 112: executes this statement as part of this file's behavior
        }  // line 113: executes this statement as part of this file's behavior
    }  // line 114: executes this statement as part of this file's behavior

    private fun loadFile(path: String) = lifecycleScope.launch {  // line 116: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 117: executes this statement as part of this file's behavior
        setBusy("Loading $path…")  // line 118: executes this statement as part of this file's behavior
        when (val result = repository.getFile(id, path)) {  // line 119: executes this statement as part of this file's behavior
            is ApiResult.Success -> renderFile(result.data)  // line 120: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 121: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 122: executes this statement as part of this file's behavior
        }  // line 123: executes this statement as part of this file's behavior
    }  // line 124: executes this statement as part of this file's behavior

    private fun saveSelectedFile() = lifecycleScope.launch {  // line 126: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 127: executes this statement as part of this file's behavior
        val path = selectedFilePath ?: return@launch  // line 128: executes this statement as part of this file's behavior
        setBusy("Saving $path…")  // line 129: executes this statement as part of this file's behavior
        when (val result = repository.saveFile(id, path, editor.text.toString())) {  // line 130: executes this statement as part of this file's behavior
            is ApiResult.Success -> {  // line 131: executes this statement as part of this file's behavior
                renderFile(result.data)  // line 132: executes this statement as part of this file's behavior
                status.text = getString(R.string.file_saved, result.data.path)  // line 133: executes this statement as part of this file's behavior
            }  // line 134: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 135: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 136: executes this statement as part of this file's behavior
        }  // line 137: executes this statement as part of this file's behavior
    }  // line 138: executes this statement as part of this file's behavior

    private fun startBuild() = lifecycleScope.launch {  // line 140: executes this statement as part of this file's behavior
        val id = projectId ?: return@launch  // line 141: executes this statement as part of this file's behavior
        setBusy("Starting build…")  // line 142: executes this statement as part of this file's behavior
        when (val result = repository.createBuild(id)) {  // line 143: executes this statement as part of this file's behavior
            is ApiResult.Success -> status.text = getString(R.string.build_started, result.data.buildId, result.data.status)  // line 144: executes this statement as part of this file's behavior
            is ApiResult.Error -> setError(result.message)  // line 145: executes this statement as part of this file's behavior
            ApiResult.Loading -> Unit  // line 146: executes this statement as part of this file's behavior
        }  // line 147: executes this statement as part of this file's behavior
    }  // line 148: executes this statement as part of this file's behavior

    private fun renderScanCards(scan: ProjectScanResponse) {  // line 150: executes this statement as part of this file's behavior
        val top = if (scan.recommendedPackId == "unknown") {  // line 151: executes this statement as part of this file's behavior
            null  // line 152: executes this statement as part of this file's behavior
        } else {  // line 153: executes this statement as part of this file's behavior
            scan.matches.firstOrNull { it.id == scan.recommendedPackId } ?: scan.matches.firstOrNull()  // line 154: executes this statement as part of this file's behavior
        }  // line 155: executes this statement as part of this file's behavior
        if (top == null) {  // line 156: executes this statement as part of this file's behavior
            detectedPackCard.text = "Detected pack\nUnknown project\nConfidence: weak\nRuntime: unknown"  // line 157: executes this statement as part of this file's behavior
            missingSecretsCard.text = "Missing secrets\nNo required secrets detected."  // line 158: executes this statement as part of this file's behavior
            return  // line 159: executes this statement as part of this file's behavior
        }  // line 160: executes this statement as part of this file's behavior
        val runtimeLabel = when {  // line 161: executes this statement as part of this file's behavior
            top.id.contains("python") -> "Python"  // line 162: executes this statement as part of this file's behavior
            top.id.contains("n8n") -> "Workflow"  // line 163: executes this statement as part of this file's behavior
            else -> "Node 22"  // line 164: executes this statement as part of this file's behavior
        }  // line 165: executes this statement as part of this file's behavior
        detectedPackCard.text = "Detected pack\n${top.name}\nConfidence: ${top.confidence}, ${top.score}\nRuntime: $runtimeLabel"  // line 166: executes this statement as part of this file's behavior
        val missing = top.requiredSecrets  // line 167: executes this statement as part of this file's behavior
        missingSecretsCard.text = if (missing.isEmpty()) {  // line 168: executes this statement as part of this file's behavior
            "Missing secrets\nNo required secrets detected."  // line 169: executes this statement as part of this file's behavior
        } else {  // line 170: executes this statement as part of this file's behavior
            "Missing secrets\n${missing.joinToString(", ")}\nNext action: Add secrets"  // line 171: executes this statement as part of this file's behavior
        }  // line 172: executes this statement as part of this file's behavior
    }  // line 173: executes this statement as part of this file's behavior

    private fun renderFiles(files: List<ProjectFileSummary>, message: String = "Loaded ${files.size} file(s).") {  // line 175: executes this statement as part of this file's behavior
        fileList.removeAllViews()  // line 176: executes this statement as part of this file's behavior
        if (files.isEmpty()) {  // line 177: executes this statement as part of this file's behavior
            status.text = "No files yet. Tap Generate Project."  // line 178: executes this statement as part of this file's behavior
            return  // line 179: executes this statement as part of this file's behavior
        }  // line 180: executes this statement as part of this file's behavior
        files.forEach { file ->  // line 181: executes this statement as part of this file's behavior
            val row = Button(requireContext()).apply {  // line 182: executes this statement as part of this file's behavior
                text = file.path  // line 183: executes this statement as part of this file's behavior
                isAllCaps = false  // line 184: executes this statement as part of this file's behavior
                setOnClickListener { loadFile(file.path) }  // line 185: executes this statement as part of this file's behavior
            }  // line 186: executes this statement as part of this file's behavior
            fileList.addView(row)  // line 187: executes this statement as part of this file's behavior
        }  // line 188: executes this statement as part of this file's behavior
        status.text = message  // line 189: executes this statement as part of this file's behavior
    }  // line 190: executes this statement as part of this file's behavior

    private fun renderFile(file: ProjectFileContent) {  // line 192: executes this statement as part of this file's behavior
        selectedFilePath = file.path  // line 193: executes this statement as part of this file's behavior
        editor.isEnabled = file.editable  // line 194: executes this statement as part of this file's behavior
        editor.setText(file.content)  // line 195: executes this statement as part of this file's behavior
        metadata.text = getString(R.string.generated_status, file.generated.toString(), file.editable.toString(), file.updatedAt)  // line 196: executes this statement as part of this file's behavior
        status.text = getString(R.string.file_loaded, file.path)  // line 197: executes this statement as part of this file's behavior
        updateSelectionButtons()  // line 198: executes this statement as part of this file's behavior
    }  // line 199: executes this statement as part of this file's behavior

    private fun setBusy(message: String) { status.text = message }  // line 201: executes this statement as part of this file's behavior
    private fun setError(message: String) { status.text = "Error: $message" }  // line 202: executes this statement as part of this file's behavior
    private fun updateSelectionButtons() {  // line 203: executes this statement as part of this file's behavior
        val enabled = projectId != null && selectedFilePath != null && editor.isEnabled  // line 204: executes this statement as part of this file's behavior
        saveButton.isEnabled = enabled  // line 205: executes this statement as part of this file's behavior
        revertButton.isEnabled = enabled  // line 206: executes this statement as part of this file's behavior
    }  // line 207: executes this statement as part of this file's behavior
}  // line 208: executes this statement as part of this file's behavior
