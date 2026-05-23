// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.backend  // line 7: executes this statement as part of this file's behavior

import kotlinx.coroutines.flow.MutableStateFlow  // line 9: executes this statement as part of this file's behavior

object BotEngineBindingState {  // line 11: executes this statement as part of this file's behavior
    val serviceRunning = MutableStateFlow<Boolean?>(null)  // line 12: executes this statement as part of this file's behavior
}  // line 13: executes this statement as part of this file's behavior
