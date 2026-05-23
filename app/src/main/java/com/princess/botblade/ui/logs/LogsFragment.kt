// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.ui.logs  // line 7: executes this statement as part of this file's behavior

import android.content.Intent  // line 9: executes this statement as part of this file's behavior
import android.net.Uri  // line 10: executes this statement as part of this file's behavior
import android.os.Bundle  // line 11: executes this statement as part of this file's behavior
import android.view.View  // line 12: executes this statement as part of this file's behavior
import androidx.compose.foundation.background  // line 13: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Arrangement  // line 14: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Column  // line 15: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.Row  // line 16: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxSize  // line 17: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.fillMaxWidth  // line 18: executes this statement as part of this file's behavior
import androidx.compose.foundation.layout.padding  // line 19: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.LazyColumn  // line 20: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.items  // line 21: executes this statement as part of this file's behavior
import androidx.compose.foundation.lazy.rememberLazyListState  // line 22: executes this statement as part of this file's behavior
import androidx.compose.material3.Button  // line 23: executes this statement as part of this file's behavior
import androidx.compose.material3.Text  // line 24: executes this statement as part of this file's behavior
import androidx.compose.runtime.Composable  // line 25: executes this statement as part of this file's behavior
import androidx.compose.runtime.LaunchedEffect  // line 26: executes this statement as part of this file's behavior
import androidx.compose.runtime.getValue  // line 27: executes this statement as part of this file's behavior
import androidx.compose.runtime.mutableStateOf  // line 28: executes this statement as part of this file's behavior
import androidx.compose.runtime.remember  // line 29: executes this statement as part of this file's behavior
import androidx.compose.runtime.setValue  // line 30: executes this statement as part of this file's behavior
import androidx.compose.ui.Modifier  // line 31: executes this statement as part of this file's behavior
import androidx.compose.ui.graphics.Color  // line 32: executes this statement as part of this file's behavior
import androidx.compose.ui.platform.ComposeView  // line 33: executes this statement as part of this file's behavior
import androidx.compose.ui.text.font.FontFamily  // line 34: executes this statement as part of this file's behavior
import androidx.compose.ui.unit.dp  // line 35: executes this statement as part of this file's behavior
import androidx.core.content.FileProvider  // line 36: executes this statement as part of this file's behavior
import androidx.fragment.app.Fragment  // line 37: executes this statement as part of this file's behavior
import java.io.File  // line 38: executes this statement as part of this file's behavior

class LogsFragment : Fragment() {  // line 40: executes this statement as part of this file's behavior
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {  // line 41: executes this statement as part of this file's behavior
        setContent { LogsScreen() }  // line 42: executes this statement as part of this file's behavior
    }  // line 43: executes this statement as part of this file's behavior

    @Composable  // line 45: executes this statement as part of this file's behavior
    private fun LogsScreen() {  // line 46: executes this statement as part of this file's behavior
        var lines by remember { mutableStateOf(readLogLines()) }  // line 47: executes this statement as part of this file's behavior
        val listState = rememberLazyListState()  // line 48: executes this statement as part of this file's behavior
        LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex) }  // line 49: executes this statement as part of this file's behavior
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {  // line 50: executes this statement as part of this file's behavior
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {  // line 51: executes this statement as part of this file's behavior
                Button(onClick = { clearLog(); lines = readLogLines() }) { Text("Clear") }  // line 52: executes this statement as part of this file's behavior
                Button(onClick = { shareLog() }) { Text("Share") }  // line 53: executes this statement as part of this file's behavior
            }  // line 54: executes this statement as part of this file's behavior
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {  // line 55: executes this statement as part of this file's behavior
                items(lines) { Text(it, color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace) }  // line 56: executes this statement as part of this file's behavior
            }  // line 57: executes this statement as part of this file's behavior
        }  // line 58: executes this statement as part of this file's behavior
    }  // line 59: executes this statement as part of this file's behavior

    private fun logFile(): File = File(requireContext().filesDir, "logs/engine.log")  // line 61: executes this statement as part of this file's behavior
    private fun readLogLines(): List<String> = logFile().takeIf { it.exists() }?.readLines().orEmpty()  // line 62: executes this statement as part of this file's behavior
    private fun clearLog() { logFile().parentFile?.mkdirs(); logFile().writeText("") }  // line 63: executes this statement as part of this file's behavior
    private fun shareLog() {  // line 64: executes this statement as part of this file's behavior
        val file = logFile()  // line 65: executes this statement as part of this file's behavior
        if (!file.exists()) file.writeText("")  // line 66: executes this statement as part of this file's behavior
        val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)  // line 67: executes this statement as part of this file's behavior
        val intent = Intent(Intent.ACTION_SEND).apply {  // line 68: executes this statement as part of this file's behavior
            type = "text/plain"  // line 69: executes this statement as part of this file's behavior
            putExtra(Intent.EXTRA_STREAM, uri)  // line 70: executes this statement as part of this file's behavior
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)  // line 71: executes this statement as part of this file's behavior
        }  // line 72: executes this statement as part of this file's behavior
        startActivity(Intent.createChooser(intent, "Share logs"))  // line 73: executes this statement as part of this file's behavior
    }  // line 74: executes this statement as part of this file's behavior
}  // line 75: executes this statement as part of this file's behavior
