// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.api  // line 7: executes this statement as part of this file's behavior

sealed class ApiResult<out T> {  // line 9: executes this statement as part of this file's behavior
    data object Loading : ApiResult<Nothing>()  // line 10: executes this statement as part of this file's behavior
    data class Success<T>(val data: T) : ApiResult<T>()  // line 11: executes this statement as part of this file's behavior
    data class Error(  // line 12: executes this statement as part of this file's behavior
        val message: String,  // line 13: executes this statement as part of this file's behavior
        val cause: Throwable? = null,  // line 14: executes this statement as part of this file's behavior
    ) : ApiResult<Nothing>()  // line 15: executes this statement as part of this file's behavior
}  // line 16: executes this statement as part of this file's behavior
