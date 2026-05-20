package com.princess.botblade.data.api

import com.princess.botblade.BuildConfig

object BackendConfig {
    const val localUrl: String = "http://127.0.0.1:7432"
    const val remoteUrl: String = ""

    val baseUrl: String
        get() = if (BuildConfig.USE_REMOTE_BACKEND && remoteUrl.isNotBlank()) remoteUrl else localUrl
}
