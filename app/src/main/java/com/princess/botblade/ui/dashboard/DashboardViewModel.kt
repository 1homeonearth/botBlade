package com.princess.botblade.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princess.botblade.backend.BotEngineBindingState
import com.princess.botblade.data.api.ApiConfig
import com.princess.botblade.data.api.ApiResult
import com.princess.botblade.data.model.RuntimeStatusResponse
import com.princess.botblade.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

data class DashboardControlState(val running: Boolean, val canStart: Boolean, val canStop: Boolean, val canRestart: Boolean)

data class DashboardMessage(val text: String, val isError: Boolean = false)

class DashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    private val _status = MutableStateFlow<RuntimeStatusResponse?>(null)
    val status = _status.asStateFlow()
    private val _controls = MutableStateFlow(DashboardControlState(false, true, false, false))
    val controls: StateFlow<DashboardControlState> = _controls.asStateFlow()
    private val _message = MutableStateFlow<DashboardMessage?>(null)
    val message: StateFlow<DashboardMessage?> = _message.asStateFlow()

    init {
        viewModelScope.launch {
            BotEngineBindingState.serviceRunning.collectLatest { running ->
                if (running != null) {
                    val status = if (running) "running" else "stopped"
                    updateRuntimeState(RuntimeStatusResponse("bound", status, running, "Status from bound BotEngineService"))
                }
            }
        }
    }

    fun loadStatus(projectId: String?) = viewModelScope.launch {
        if (BotEngineBindingState.serviceRunning.value == null) {
            when (val result = repository.getBotStatus(projectId)) {
                is ApiResult.Success -> {
                    updateRuntimeState(result.data)
                    _message.value = DashboardMessage(result.data.message ?: "Runtime status refreshed.")
                }
                is ApiResult.Error -> {
                    _message.value = DashboardMessage(result.message, isError = true)
                    appendLog("Status check failed: ${result.message}")
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun start(projectId: String?) = performRuntimeAction(projectId, "start")
    fun stop(projectId: String?) = performRuntimeAction(projectId, "stop")
    fun restart(projectId: String?) = performRuntimeAction(projectId, "restart")

    fun clearMessage() { _message.value = null }

    fun markOpenedFromNotification() {
        _message.value = DashboardMessage("Opened from BotBlade runtime notification. Review controls and recent logs here.")
        appendLog("Dashboard opened from runtime notification.")
    }

    private fun performRuntimeAction(projectId: String?, action: String) = viewModelScope.launch {
        val currentStatus = _status.value?.status
        appendLog("${action.replaceFirstChar { it.uppercase() }} requested${projectId?.let { " for workspace $it" } ?: ""}.")
        when (val result = repository.runtimeAction(projectId, action, currentStatus)) {
            is ApiResult.Success -> {
                updateRuntimeState(result.data)
                val message = result.data.message ?: "Runtime ${result.data.status}."
                _message.value = DashboardMessage(message)
                appendLog("Runtime ${action} result: ${result.data.status}${result.data.message?.let { " — $it" } ?: ""}")
            }
            is ApiResult.Error -> {
                _message.value = DashboardMessage(result.message, isError = true)
                appendLog("Runtime ${action} failed: ${result.message}")
            }
            ApiResult.Loading -> Unit
        }
    }

    private fun updateRuntimeState(status: RuntimeStatusResponse) {
        _status.value = status
        val running = status.running || status.status.equals("running", ignoreCase = true)
        _controls.value = if (running) DashboardControlState(true, false, true, true) else DashboardControlState(false, true, false, false)
    }

    private fun appendLog(line: String) {
        _logs.value = (_logs.value + line).takeLast(200)
    }

    fun connectLogs() {
        if (ws != null) return
        val logsUrl = ApiConfig.baseUrl
            .replaceFirst("http://", "ws://")
            .replaceFirst("https://", "wss://") + "/logs"
        ws = client.newWebSocket(Request.Builder().url(logsUrl).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                appendLog(text)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ws = null
                appendLog("Live log connection unavailable: ${t.message ?: "unknown error"}")
            }
        })
    }

    fun disconnectLogs() { ws?.close(1000, "pause"); ws = null }
}
