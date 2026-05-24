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

class DashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    private val _status = MutableStateFlow<RuntimeStatusResponse?>(null)
    val status = _status.asStateFlow()
    private val _controls = MutableStateFlow(DashboardControlState(false, true, false, false))
    val controls: StateFlow<DashboardControlState> = _controls.asStateFlow()

    init {
        viewModelScope.launch {
            BotEngineBindingState.serviceRunning.collectLatest { running ->
                if (running != null) {
                    val status = if (running) "running" else "stopped"
                    _status.value = RuntimeStatusResponse("bound", status, running, "Status from bound BotEngineService")
                    _controls.value = if (running) DashboardControlState(true, false, true, true) else DashboardControlState(false, true, false, false)
                }
            }
        }
    }

    fun loadStatus(projectId: String?) = viewModelScope.launch {
        if (BotEngineBindingState.serviceRunning.value == null) {
            when (val result = repository.getBotStatus(projectId)) {
                is ApiResult.Success -> {
                    _status.value = result.data
                    val running = result.data.status.equals("running", ignoreCase = true)
                    _controls.value = if (running) DashboardControlState(true, false, true, true) else DashboardControlState(false, true, false, false)
                }
                else -> {}
            }
        }
    }

    fun start() { _logs.value = (_logs.value + "Start requested").takeLast(200) }
    fun stop() { _logs.value = (_logs.value + "Stop requested").takeLast(200) }
    fun restart() { _logs.value = (_logs.value + "Restart requested").takeLast(200) }

    fun connectLogs() {
        if (ws != null) return
        ws = client.newWebSocket(Request.Builder().url(logSocketUrl()).build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                _logs.value = (_logs.value + text).takeLast(200)
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { ws = null }
        })
    }

    fun disconnectLogs() { ws?.close(1000, "pause"); ws = null }

    private fun logSocketUrl(): String {
        val base = ApiConfig.baseUrl
        return when {
            base.startsWith("https://", ignoreCase = true) -> "wss://${base.removePrefix("https://")}/logs"
            base.startsWith("http://", ignoreCase = true) -> "ws://${base.removePrefix("http://")}/logs"
            else -> "ws://127.0.0.1:7432/logs"
        }
    }
}
