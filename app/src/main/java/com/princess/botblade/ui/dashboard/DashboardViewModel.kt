package com.princess.botblade.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.princess.botblade.backend.BotEngineBindingState
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

class DashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {
    private val client = OkHttpClient()
    private var ws: WebSocket? = null
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()

    init {
        viewModelScope.launch {
            BotEngineBindingState.serviceRunning.collectLatest { running ->
                if (running != null) {
                    val status = if (running) "running" else "stopped"
                    _status.value = RuntimeStatusResponse("bound", status, "Status from bound BotEngineService")
                }
            }
        }
    }

    private val _status = MutableStateFlow<RuntimeStatusResponse?>(null)
    val status = _status.asStateFlow()

    fun loadStatus(projectId: String?) = viewModelScope.launch {
        if (BotEngineBindingState.serviceRunning.value == null) {
            when (val result = repository.getBotStatus(projectId)) {
                is ApiResult.Success -> _status.value = result.data
                else -> {}
            }
        }
    }

    fun connectLogs() {
        if (ws != null) return
        ws = client.newWebSocket(Request.Builder().url("ws://127.0.0.1:7432/logs").build(), object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                val next = (_logs.value + text).takeLast(200)
                _logs.value = next
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                ws = null
            }
        })
    }

    fun disconnectLogs() { ws?.close(1000, "pause"); ws = null }
}
