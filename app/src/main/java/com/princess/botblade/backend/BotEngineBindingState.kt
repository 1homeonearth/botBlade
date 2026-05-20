package com.princess.botblade.backend

import kotlinx.coroutines.flow.MutableStateFlow

object BotEngineBindingState {
    val serviceRunning = MutableStateFlow<Boolean?>(null)
}
