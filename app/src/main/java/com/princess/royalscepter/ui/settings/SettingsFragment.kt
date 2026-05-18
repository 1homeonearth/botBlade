package com.princess.royalscepter.ui.settings

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.princess.royalscepter.R
import com.princess.royalscepter.data.api.ApiResult
import com.princess.royalscepter.data.model.SecretCreateRequest
import com.princess.royalscepter.data.model.SecretSummary
import com.princess.royalscepter.data.repository.SecretRepository
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {
    private val repository = SecretRepository()
    private lateinit var status: TextView
    private lateinit var list: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var typeInput: EditText
    private lateinit var projectInput: EditText
    private lateinit var valueInput: EditText

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onResume() {
        super.onResume()
        requireActivity().window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
    }

    override fun onPause() {
        requireActivity().window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        super.onPause()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        status = view.findViewById(R.id.secrets_status)
        list = view.findViewById(R.id.secrets_list_container)
        nameInput = view.findViewById(R.id.secret_name_input)
        typeInput = view.findViewById(R.id.secret_type_input)
        projectInput = view.findViewById(R.id.secret_project_input)
        valueInput = view.findViewById(R.id.secret_value_input)
        valueInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        typeInput.setText("discord_bot_token")
        view.findViewById<Button>(R.id.create_secret_button).setOnClickListener { createSecret() }
        loadSecrets()
    }

    private fun loadSecrets() = lifecycleScope.launch {
        status.text = getString(R.string.secrets_loading)
        when (val result = repository.listSecrets()) {
            is ApiResult.Success -> renderSecrets(result.data)
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun createSecret() = lifecycleScope.launch {
        val value = valueInput.text.toString()
        val request = SecretCreateRequest(
            projectId = projectInput.text.toString().takeIf { it.isNotBlank() },
            name = nameInput.text.toString(),
            type = typeInput.text.toString().ifBlank { "custom" },
            value = value,
        )
        status.text = "Saving secret reference…"
        when (val result = repository.createSecret(request)) {
            is ApiResult.Success -> {
                valueInput.text?.clear()
                status.text = getString(R.string.secret_saved_hidden)
                loadSecrets()
            }
            is ApiResult.Error -> {
                valueInput.text?.clear()
                status.text = "Error: ${result.message}"
            }
            ApiResult.Loading -> Unit
        }
    }

    private fun rotateSecret(secret: SecretSummary) = lifecycleScope.launch {
        val value = valueInput.text.toString()
        if (value.isBlank()) {
            status.text = "Enter the new secret value in the secret value field first."
            return@launch
        }
        status.text = "Rotating ${secret.name}…"
        when (val result = repository.rotateSecret(secret.id, value)) {
            is ApiResult.Success -> {
                valueInput.text?.clear()
                status.text = getString(R.string.secret_saved_hidden)
                loadSecrets()
            }
            is ApiResult.Error -> {
                valueInput.text?.clear()
                status.text = "Error: ${result.message}"
            }
            ApiResult.Loading -> Unit
        }
    }

    private fun confirmDeleteSecret(secret: SecretSummary) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete secret reference?")
            .setMessage("Delete ${secret.name}? The full value will remain hidden and cannot be recovered from this app.")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ -> deleteSecret(secret) }
            .show()
    }

    private fun deleteSecret(secret: SecretSummary) = lifecycleScope.launch {
        status.text = "Deleting ${secret.name}…"
        when (val result = repository.deleteSecret(secret.id)) {
            is ApiResult.Success -> loadSecrets()
            is ApiResult.Error -> status.text = "Error: ${result.message}"
            ApiResult.Loading -> Unit
        }
    }

    private fun renderSecrets(secrets: List<SecretSummary>) {
        list.removeAllViews()
        if (secrets.isEmpty()) {
            status.text = "No secrets saved."
            return
        }
        secrets.forEach { secret ->
            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(0, 12, 0, 12)
            }
            row.addView(TextView(requireContext()).apply {
                text = "${secret.name} • ${secret.type}\n${secret.storageMode} • ${secret.fingerprint}\nUpdated: ${secret.updatedAt}\nProject: ${secret.projectId ?: "global"}"
            })
            row.addView(Button(requireContext()).apply {
                text = getString(R.string.rotate_secret)
                setOnClickListener { rotateSecret(secret) }
            })
            row.addView(Button(requireContext()).apply {
                text = getString(R.string.delete_secret)
                setOnClickListener { confirmDeleteSecret(secret) }
            })
            list.addView(row)
        }
        status.text = "Loaded ${secrets.size} secret reference(s). Values stay hidden."
    }
}
