// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.backend  // line 7: executes this statement as part of this file's behavior

import android.content.BroadcastReceiver  // line 9: executes this statement as part of this file's behavior
import android.content.Context  // line 10: executes this statement as part of this file's behavior
import android.content.Intent  // line 11: executes this statement as part of this file's behavior
import kotlinx.coroutines.CoroutineScope  // line 12: executes this statement as part of this file's behavior
import kotlinx.coroutines.Dispatchers  // line 13: executes this statement as part of this file's behavior
import kotlinx.coroutines.launch  // line 14: executes this statement as part of this file's behavior
import kotlinx.coroutines.flow.first  // line 15: executes this statement as part of this file's behavior

class BootReceiver : BroadcastReceiver() {  // line 17: executes this statement as part of this file's behavior
    override fun onReceive(context: Context, intent: Intent) {  // line 18: executes this statement as part of this file's behavior
        val action = intent.action ?: return  // line 19: executes this statement as part of this file's behavior
        if (action != Intent.ACTION_BOOT_COMPLETED && action != ACTION_QUICKBOOT_POWERON) return  // line 20: executes this statement as part of this file's behavior

        val pendingResult = goAsync()  // line 22: executes this statement as part of this file's behavior
        CoroutineScope(Dispatchers.IO).launch {  // line 23: executes this statement as part of this file's behavior
            try {  // line 24: executes this statement as part of this file's behavior
                if (EnginePreferences.autoStartOnBoot(context).first()) {  // line 25: executes this statement as part of this file's behavior
                    context.startForegroundService(Intent(context, BotEngineService::class.java))  // line 26: executes this statement as part of this file's behavior
                }  // line 27: executes this statement as part of this file's behavior
            } finally {  // line 28: executes this statement as part of this file's behavior
                pendingResult.finish()  // line 29: executes this statement as part of this file's behavior
            }  // line 30: executes this statement as part of this file's behavior
        }  // line 31: executes this statement as part of this file's behavior
    }  // line 32: executes this statement as part of this file's behavior

    companion object {  // line 34: executes this statement as part of this file's behavior
        private const val ACTION_QUICKBOOT_POWERON = "android.intent.action.QUICKBOOT_POWERON"  // line 35: executes this statement as part of this file's behavior
    }  // line 36: executes this statement as part of this file's behavior
}  // line 37: executes this statement as part of this file's behavior
