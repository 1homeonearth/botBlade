package com.princess.botblade.ui.components

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun TerminalView(
    modifier: Modifier = Modifier,
    title: String = "BotBlade Terminal",
    lines: List<String> = emptyList(),
) {
    Surface(
        color = BotBladeTokens.Ink,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, BotBladeTokens.Stroke),
        modifier = modifier.fillMaxWidth().heightIn(min = 220.dp),
    ) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                    webViewClient = WebViewClient()
                    settings.javaScriptEnabled = false
                    settings.domStorageEnabled = false
                    loadDataWithBaseURL(null, renderTerminalHtml(title, lines), "text/html", "UTF-8", null)
                }
            },
            update = { webView ->
                webView.loadDataWithBaseURL(null, renderTerminalHtml(title, lines), "text/html", "UTF-8", null)
            },
        )
    }
}

private fun renderTerminalHtml(title: String, lines: List<String>): String {
    val escapedTitle = title.escapeHtml()
    val escapedLines = if (lines.isEmpty()) {
        listOf("terminal surface ready", "xterm.js integration pending")
    } else {
        lines.takeLast(250)
    }.joinToString("\n") { "<div><span class=\"prompt\">botblade$</span> ${it.escapeHtml()}</div>" }
    return """
        <!doctype html>
        <html>
        <head>
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <style>
            html, body { margin: 0; padding: 0; background: #090B12; color: #7CFFB2; font-family: monospace; }
            .terminal { padding: 16px; font-size: 13px; line-height: 1.45; }
            .title { color: #8FD8FF; font-weight: 700; margin-bottom: 12px; }
            .prompt { color: #FFD166; }
          </style>
        </head>
        <body><main class="terminal"><div class="title">$escapedTitle</div>$escapedLines</main></body>
        </html>
    """.trimIndent()
}

private fun String.escapeHtml(): String = buildString(length) {
    this@escapeHtml.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#39;")
            else -> append(char)
        }
    }
}
