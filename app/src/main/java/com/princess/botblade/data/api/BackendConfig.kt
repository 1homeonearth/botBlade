// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.api  // line 7: executes this statement as part of this file's behavior

import com.princess.botblade.BuildConfig  // line 9: executes this statement as part of this file's behavior

object BackendConfig {  // line 11: executes this statement as part of this file's behavior
    const val localUrl: String = "http://127.0.0.1:7432"  // line 12: executes this statement as part of this file's behavior
    const val remoteUrl: String = ""  // line 13: executes this statement as part of this file's behavior

    val baseUrl: String  // line 15: executes this statement as part of this file's behavior
        get() = if (BuildConfig.USE_REMOTE_BACKEND && remoteUrl.isNotBlank()) remoteUrl else localUrl  // line 16: executes this statement as part of this file's behavior
}  // line 17: executes this statement as part of this file's behavior
