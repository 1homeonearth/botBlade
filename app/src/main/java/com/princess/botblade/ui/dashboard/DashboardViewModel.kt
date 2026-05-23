// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.dashboard  // line 7: executes this statement as part of this file's behavior

import androidx.lifecycle.ViewModel  // line 9: executes this statement as part of this file's behavior
import androidx.lifecycle.viewModelScope  // line 10: executes this statement as part of this file's behavior
import com.princess.botblade.backend.BotEngineBindingState  // line 11: executes this statement as part of this file's behavior
import com.princess.botblade.data.api.ApiResult  // line 12: executes this statement as part of this file's behavior
import com.princess.botblade.data.model.RuntimeStatusResponse  // line 13: executes this statement as part of this file's behavior
import com.princess.botblade.data.repository.DashboardRepository  // line 14: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.MutableStateFlow  // line 15: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.StateFlow  // line 16: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.asStateFlow  // line 17: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.collectLatest  // line 18: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 19: executes this statement as part of this file's behavior
import okhttp3.OkHttpClient  // line 20: executes this statement as part of this file's behavior
import okhttp3.Request  // line 21: executes this statement as part of this file's behavior
import okhttp3.Response  // line 22: executes this statement as part of this file's behavior
import okhttp3.WebSocket  // line 23: executes this statement as part of this file's behavior
import okhttp3.WebSocketListener  // line 24: executes this statement as part of this file's behavior

data class DashboardControlState(val running: Boolean, val canStart: Boolean, val canStop: Boolean, val canRestart: Boolean)  // line 26: executes this statement as part of this file's behavior

class DashboardViewModel(private val repository: DashboardRepository = DashboardRepository()) : ViewModel() {  // line 28: executes this statement as part of this file's behavior
    private val client = OkHttpClient()  // line 29: executes this statement as part of this file's behavior
    private var ws: WebSocket? = null  // line 30: executes this statement as part of this file's behavior
    private val _logs = MutableStateFlow<List<String>>(emptyList())  // line 31: executes this statement as part of this file's behavior
    val logs: StateFlow<List<String>> = _logs.asStateFlow()  // line 32: executes this statement as part of this file's behavior
    private val _status = MutableStateFlow<RuntimeStatusResponse?>(null)  // line 33: executes this statement as part of this file's behavior
    val status = _status.asStateFlow()  // line 34: executes this statement as part of this file's behavior
    private val _controls = MutableStateFlow(DashboardControlState(false, true, false, false))  // line 35: executes this statement as part of this file's behavior
    val controls: StateFlow<DashboardControlState> = _controls.asStateFlow()  // line 36: executes this statement as part of this file's behavior

    init {  // line 38: executes this statement as part of this file's behavior
        viewModelScope.launch {  // line 39: executes this statement as part of this file's behavior
            BotEngineBindingState.serviceRunning.collectLatest { running ->  // line 40: executes this statement as part of this file's behavior
                if (running != null) {  // line 41: executes this statement as part of this file's behavior
                    val status = if (running) "running" else "stopped"  // line 42: executes this statement as part of this file's behavior
                    _status.value = RuntimeStatusResponse("bound", status, running, "Status from bound BotEngineService")  // line 43: executes this statement as part of this file's behavior
                    _controls.value = if (running) DashboardControlState(true, false, true, true) else DashboardControlState(false, true, false, false)  // line 44: executes this statement as part of this file's behavior
                }  // line 45: executes this statement as part of this file's behavior
            }  // line 46: executes this statement as part of this file's behavior
        }  // line 47: executes this statement as part of this file's behavior
    }  // line 48: executes this statement as part of this file's behavior

    fun loadStatus(projectId: String?) = viewModelScope.launch {  // line 50: executes this statement as part of this file's behavior
        if (BotEngineBindingState.serviceRunning.value == null) {  // line 51: executes this statement as part of this file's behavior
            when (val result = repository.getBotStatus(projectId)) {  // line 52: executes this statement as part of this file's behavior
                is ApiResult.Success -> {  // line 53: executes this statement as part of this file's behavior
                    _status.value = result.data  // line 54: executes this statement as part of this file's behavior
                    val running = result.data.status.equals("running", ignoreCase = true)  // line 55: executes this statement as part of this file's behavior
                    _controls.value = if (running) DashboardControlState(true, false, true, true) else DashboardControlState(false, true, false, false)  // line 56: executes this statement as part of this file's behavior
                }  // line 57: executes this statement as part of this file's behavior
                else -> {}  // line 58: executes this statement as part of this file's behavior
            }  // line 59: executes this statement as part of this file's behavior
        }  // line 60: executes this statement as part of this file's behavior
    }  // line 61: executes this statement as part of this file's behavior

    fun start() { _logs.value = (_logs.value + "Start requested").takeLast(200) }  // line 63: executes this statement as part of this file's behavior
    fun stop() { _logs.value = (_logs.value + "Stop requested").takeLast(200) }  // line 64: executes this statement as part of this file's behavior
    fun restart() { _logs.value = (_logs.value + "Restart requested").takeLast(200) }  // line 65: executes this statement as part of this file's behavior

    fun connectLogs() {  // line 67: executes this statement as part of this file's behavior
        if (ws != null) return  // line 68: executes this statement as part of this file's behavior
        ws = client.newWebSocket(Request.Builder().url("ws://127.0.0.1:7432/logs").build(), object : WebSocketListener() {  // line 69: executes this statement as part of this file's behavior
            override fun onMessage(webSocket: WebSocket, text: String) {  // line 70: executes this statement as part of this file's behavior
                _logs.value = (_logs.value + text).takeLast(200)  // line 71: executes this statement as part of this file's behavior
            }  // line 72: executes this statement as part of this file's behavior
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) { ws = null }  // line 73: executes this statement as part of this file's behavior
        })  // line 74: executes this statement as part of this file's behavior
    }  // line 75: executes this statement as part of this file's behavior

    fun disconnectLogs() { ws?.close(1000, "pause"); ws = null }  // line 77: executes this statement as part of this file's behavior
}  // line 78: executes this statement as part of this file's behavior
