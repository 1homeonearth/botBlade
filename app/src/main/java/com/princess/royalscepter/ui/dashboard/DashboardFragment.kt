package com.princess.royalscepter.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.princess.royalscepter.R
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class DashboardFragment : Fragment() {
    private val apiBaseUrl = "http://10.0.2.2:8000"

    private lateinit var statusText: TextView
    private lateinit var messageText: TextView
    private lateinit var refreshButton: Button
    private lateinit var toggleButton: Button

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_dashboard, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        statusText = view.findViewById(R.id.bot_status_text)
        messageText = view.findViewById(R.id.dashboard_message_text)
        refreshButton = view.findViewById(R.id.refresh_status_button)
        toggleButton = view.findViewById(R.id.toggle_bot_button)

        refreshButton.setOnClickListener { loadBotStatus() }
        toggleButton.setOnClickListener { toggleBot() }

        showUnknownState()
        loadBotStatus()
    }

    private fun showUnknownState() {
        statusText.text = getString(R.string.bot_status_unknown)
        messageText.text = getString(R.string.dashboard_empty_message)
    }

    private fun setLoading(isLoading: Boolean) {
        refreshButton.isEnabled = !isLoading
        toggleButton.isEnabled = !isLoading
        if (isLoading) {
            messageText.text = getString(R.string.dashboard_loading_message)
        }
    }

    private fun loadBotStatus() {
        setLoading(true)
        runApiRequest(
            path = "/api/bot-status/",
            method = "GET",
            onSuccess = { body ->
                statusText.text = getString(R.string.bot_status_value, normalizeStatusBody(body))
                messageText.text = getString(R.string.dashboard_status_loaded)
            },
        )
    }

    private fun toggleBot() {
        setLoading(true)
        runApiRequest(
            path = "/api/bot-toggle/",
            method = "POST",
            onSuccess = { body ->
                statusText.text = getString(R.string.bot_status_value, normalizeStatusBody(body))
                messageText.text = getString(R.string.dashboard_toggle_sent)
            },
        )
    }

    private fun runApiRequest(
        path: String,
        method: String,
        onSuccess: (String) -> Unit,
    ) {
        Thread {
            val result = runCatching { request(path, method) }
            activity?.runOnUiThread {
                if (!isAdded) return@runOnUiThread
                setLoading(false)
                result.fold(
                    onSuccess = onSuccess,
                    onFailure = {
                        statusText.text = getString(R.string.bot_status_unknown)
                        messageText.text = getString(R.string.dashboard_error_message)
                    },
                )
            }
        }.start()
    }

    @Throws(IOException::class)
    private fun request(path: String, method: String): String {
        val connection = URL(apiBaseUrl + path).openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = 2_000
        connection.readTimeout = 2_000
        connection.doInput = true
        if (method == "POST") {
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.outputStream.use { output -> output.write("{}".toByteArray()) }
        }

        return connection.use { http ->
            val stream = if (http.responseCode in 200..299) http.inputStream else http.errorStream
            stream?.bufferedReader()?.use { it.readText() }.orEmpty().ifBlank {
                "HTTP ${http.responseCode}"
            }
        }
    }

    private fun normalizeStatusBody(body: String): String = body
        .replace(Regex("[{}\\\"]"), "")
        .replace(',', ' ')
        .trim()
        .ifBlank { getString(R.string.unknown) }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }
}
