package com.princess.botblade.ui.logs

import android.content.Intent
import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LogsFragment : Fragment() {
    override fun onCreateView(inflater: android.view.LayoutInflater, container: android.view.ViewGroup?, savedInstanceState: Bundle?): View = ComposeView(requireContext()).apply {
        setContent { LogsScreen() }
    }

    @Composable
    private fun LogsScreen() {
        var lines by remember { mutableStateOf(readLogLines()) }
        var exportStatus by remember { mutableStateOf("") }
        val listState = rememberLazyListState()
        LaunchedEffect(lines.size) { if (lines.isNotEmpty()) listState.animateScrollToItem(lines.lastIndex) }
        LaunchedEffect(Unit) {
            exportStatus = exportLogcatToDownloads()
        }
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { clearLog(); lines = readLogLines() }) { Text("Clear") }
                Button(onClick = { shareLog() }) { Text("Share") }
            }
            if (exportStatus.isNotBlank()) {
                Text(exportStatus)
            }
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().background(Color.Black).padding(8.dp)) {
                items(lines) { Text(it, color = Color(0xFF00FF9C), fontFamily = FontFamily.Monospace) }
            }
        }
    }

    private fun logFile(): File = File(requireContext().filesDir, "logs/engine.log")
    private fun readLogLines(): List<String> = logFile().takeIf { it.exists() }?.readLines().orEmpty()
    private fun clearLog() { logFile().parentFile?.mkdirs(); logFile().writeText("") }
    private fun shareLog() {
        val file = logFile()
        if (!file.exists()) file.writeText("")
        val uri: Uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share logs"))
    }

    private suspend fun exportLogcatToDownloads(): String = withContext(Dispatchers.IO) {
        runCatching {
            val timestamp = System.currentTimeMillis()
            val fileName = "botblade-logcat-$timestamp.txt"
            val output = ProcessBuilder("logcat", "-d", "-v", "time").start().inputStream.bufferedReader().use { it.readText() }
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val resolver = requireContext().contentResolver
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("Unable to create logcat export")

            resolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(output) }
                ?: error("Unable to write logcat export")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            "Saved logcat to Downloads/$fileName"
        }.getOrElse { error -> "Logcat export failed: ${error.message}" }
    }
}
