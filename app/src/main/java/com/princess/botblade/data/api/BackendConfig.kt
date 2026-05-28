package com.princess.botblade.data.api

import com.princess.botblade.BuildConfig

object BackendConfig {
    const val localUrl: String = BuildConfig.API_BASE_URL
    const val remoteUrl: String = ""

    val baseUrl: String
        get() = if (BuildConfig.USE_REMOTE_BACKEND && remoteUrl.isNotBlank()) remoteUrl else localUrl
}
