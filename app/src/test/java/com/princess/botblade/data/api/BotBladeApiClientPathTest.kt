// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.api  // line 7: executes this statement as part of this file's behavior

import java.io.BufferedReader  // line 9: executes this statement as part of this file's behavior
import java.io.InputStreamReader  // line 10: executes this statement as part of this file's behavior
import java.net.ServerSocket  // line 11: executes this statement as part of this file's behavior
import java.util.concurrent.LinkedBlockingQueue  // line 12: executes this statement as part of this file's behavior
import java.util.concurrent.TimeUnit  // line 13: executes this statement as part of this file's behavior
import kotlin.concurrent.thread  // line 14: executes this statement as part of this file's behavior
import org.junit.After  // line 15: executes this statement as part of this file's behavior
import org.junit.Assert.assertEquals  // line 16: executes this statement as part of this file's behavior
import org.junit.Assert.assertNotNull  // line 17: executes this statement as part of this file's behavior
import org.junit.Test  // line 18: executes this statement as part of this file's behavior

class BotBladeApiClientPathTest {  // line 20: executes this statement as part of this file's behavior
    private val requestPaths = LinkedBlockingQueue<String>()  // line 21: executes this statement as part of this file's behavior
    private val server = TestHttpServer { rawPath ->  // line 22: executes this statement as part of this file's behavior
        requestPaths.add(rawPath)  // line 23: executes this statement as part of this file's behavior
        responseFor(rawPath)  // line 24: executes this statement as part of this file's behavior
    }  // line 25: executes this statement as part of this file's behavior
    private val client = BotBladeApiClient(  // line 26: executes this statement as part of this file's behavior
        baseUrl = "http://127.0.0.1:${server.port}",  // line 27: executes this statement as part of this file's behavior
        bearerToken = "test-token",  // line 28: executes this statement as part of this file's behavior
        sessionToken = "",  // line 29: executes this statement as part of this file's behavior
    )  // line 30: executes this statement as part of this file's behavior

    @After  // line 32: executes this statement as part of this file's behavior
    fun tearDown() {  // line 33: executes this statement as part of this file's behavior
        server.close()  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior

    @Test  // line 37: executes this statement as part of this file's behavior
    fun projectFilePathsEncodeSpecialCharactersButPreserveFilePathSlashes() {  // line 38: executes this statement as part of this file's behavior
        client.getProjectFile(  // line 39: executes this statement as part of this file's behavior
            projectId = "project #?/space%雪",  // line 40: executes this statement as part of this file's behavior
            filePath = "src/main file #?.kt/folder%/emoji-☃.txt",  // line 41: executes this statement as part of this file's behavior
        )  // line 42: executes this statement as part of this file's behavior

        assertEquals(  // line 44: executes this statement as part of this file's behavior
            "/api/projects/project%20%23%3F%2Fspace%25%E9%9B%AA/files/src/main%20file%20%23%3F.kt/folder%25/emoji-%E2%98%83.txt",  // line 45: executes this statement as part of this file's behavior
            takeRequestPath(),  // line 46: executes this statement as part of this file's behavior
        )  // line 47: executes this statement as part of this file's behavior
    }  // line 48: executes this statement as part of this file's behavior

    @Test  // line 50: executes this statement as part of this file's behavior
    fun saveProjectFileUsesTheSameSafeFilePathEncoding() {  // line 51: executes this statement as part of this file's behavior
        client.saveProjectFile(  // line 52: executes this statement as part of this file's behavior
            projectId = "project %/雪",  // line 53: executes this statement as part of this file's behavior
            filePath = "docs/space #? %/雪.md",  // line 54: executes this statement as part of this file's behavior
            content = "hello",  // line 55: executes this statement as part of this file's behavior
        )  // line 56: executes this statement as part of this file's behavior

        assertEquals(  // line 58: executes this statement as part of this file's behavior
            "/api/projects/project%20%25%2F%E9%9B%AA/files/docs/space%20%23%3F%20%25/%E9%9B%AA.md",  // line 59: executes this statement as part of this file's behavior
            takeRequestPath(),  // line 60: executes this statement as part of this file's behavior
        )  // line 61: executes this statement as part of this file's behavior
    }  // line 62: executes this statement as part of this file's behavior

    @Test  // line 64: executes this statement as part of this file's behavior
    fun projectCommandBuildDeploymentTargetAndSecretIdsAreEncodedAsSinglePathSegments() {  // line 65: executes this statement as part of this file's behavior
        client.getProject("project #?%/雪")  // line 66: executes this statement as part of this file's behavior
        client.deleteCommand("project #?%/雪", "command #?%/雪")  // line 67: executes this statement as part of this file's behavior
        client.getBuildLogs("project #?%/雪", "build #?%/雪")  // line 68: executes this statement as part of this file's behavior
        client.getDeploymentLogs("project #?%/雪", "deployment #?%/雪")  // line 69: executes this statement as part of this file's behavior
        client.testDeploymentTarget("target #?%/雪")  // line 70: executes this statement as part of this file's behavior
        client.rotateSecret("secret #?%/雪", "value")  // line 71: executes this statement as part of this file's behavior

        assertEquals("/api/projects/project%20%23%3F%25%2F%E9%9B%AA", takeRequestPath())  // line 73: executes this statement as part of this file's behavior
        assertEquals(  // line 74: executes this statement as part of this file's behavior
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/commands/command%20%23%3F%25%2F%E9%9B%AA",  // line 75: executes this statement as part of this file's behavior
            takeRequestPath(),  // line 76: executes this statement as part of this file's behavior
        )  // line 77: executes this statement as part of this file's behavior
        assertEquals(  // line 78: executes this statement as part of this file's behavior
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/builds/build%20%23%3F%25%2F%E9%9B%AA/logs",  // line 79: executes this statement as part of this file's behavior
            takeRequestPath(),  // line 80: executes this statement as part of this file's behavior
        )  // line 81: executes this statement as part of this file's behavior
        assertEquals(  // line 82: executes this statement as part of this file's behavior
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/deployments/deployment%20%23%3F%25%2F%E9%9B%AA/logs",  // line 83: executes this statement as part of this file's behavior
            takeRequestPath(),  // line 84: executes this statement as part of this file's behavior
        )  // line 85: executes this statement as part of this file's behavior
        assertEquals("/api/deployment-targets/target%20%23%3F%25%2F%E9%9B%AA/test", takeRequestPath())  // line 86: executes this statement as part of this file's behavior
        assertEquals("/api/secrets/secret%20%23%3F%25%2F%E9%9B%AA/rotate", takeRequestPath())  // line 87: executes this statement as part of this file's behavior
    }  // line 88: executes this statement as part of this file's behavior

    private fun takeRequestPath(): String {  // line 90: executes this statement as part of this file's behavior
        val path = requestPaths.poll(2, TimeUnit.SECONDS)  // line 91: executes this statement as part of this file's behavior
        assertNotNull(path)  // line 92: executes this statement as part of this file's behavior
        return path ?: error("Expected request path to be captured")  // line 93: executes this statement as part of this file's behavior
    }  // line 94: executes this statement as part of this file's behavior

    private fun responseFor(path: String): String = when {  // line 96: executes this statement as part of this file's behavior
        path.contains("/commands/") -> commandJson()  // line 97: executes this statement as part of this file's behavior
        path.contains("/builds/") -> buildJson()  // line 98: executes this statement as part of this file's behavior
        path.contains("/deployments/") -> deploymentJson()  // line 99: executes this statement as part of this file's behavior
        path.contains("/deployment-targets/") -> targetTestJson()  // line 100: executes this statement as part of this file's behavior
        path.contains("/secrets/") -> secretJson()  // line 101: executes this statement as part of this file's behavior
        path.contains("/files/") -> fileJson()  // line 102: executes this statement as part of this file's behavior
        else -> projectJson()  // line 103: executes this statement as part of this file's behavior
    }  // line 104: executes this statement as part of this file's behavior

    private fun projectJson() = """  // line 106: executes this statement as part of this file's behavior
        {  // line 107: executes this statement as part of this file's behavior
          "id": "project",  // line 108: executes this statement as part of this file's behavior
          "name": "Project",  // line 109: executes this statement as part of this file's behavior
          "slug": "project",  // line 110: executes this statement as part of this file's behavior
          "description": "Test project",  // line 111: executes this statement as part of this file's behavior
          "createdAt": "2026-05-18T00:00:00Z",  // line 112: executes this statement as part of this file's behavior
          "updatedAt": "2026-05-18T00:00:00Z"  // line 113: executes this statement as part of this file's behavior
        }  // line 114: executes this statement as part of this file's behavior
    """.trimIndent()  // line 115: executes this statement as part of this file's behavior

    private fun commandJson() = """  // line 117: executes this statement as part of this file's behavior
        {  // line 118: executes this statement as part of this file's behavior
          "id": "command",  // line 119: executes this statement as part of this file's behavior
          "name": "ping",  // line 120: executes this statement as part of this file's behavior
          "description": "Ping command",  // line 121: executes this statement as part of this file's behavior
          "handler": { "kind": "static_response", "content": "pong", "ephemeral": true }  // line 122: executes this statement as part of this file's behavior
        }  // line 123: executes this statement as part of this file's behavior
    """.trimIndent()  // line 124: executes this statement as part of this file's behavior

    private fun buildJson() = """  // line 126: executes this statement as part of this file's behavior
        {  // line 127: executes this statement as part of this file's behavior
          "buildId": "build",  // line 128: executes this statement as part of this file's behavior
          "projectId": "project",  // line 129: executes this statement as part of this file's behavior
          "status": "succeeded"  // line 130: executes this statement as part of this file's behavior
        }  // line 131: executes this statement as part of this file's behavior
    """.trimIndent()  // line 132: executes this statement as part of this file's behavior

    private fun deploymentJson() = """  // line 134: executes this statement as part of this file's behavior
        {  // line 135: executes this statement as part of this file's behavior
          "deploymentId": "deployment",  // line 136: executes this statement as part of this file's behavior
          "projectId": "project",  // line 137: executes this statement as part of this file's behavior
          "targetId": "target",  // line 138: executes this statement as part of this file's behavior
          "buildId": "build",  // line 139: executes this statement as part of this file's behavior
          "status": "succeeded"  // line 140: executes this statement as part of this file's behavior
        }  // line 141: executes this statement as part of this file's behavior
    """.trimIndent()  // line 142: executes this statement as part of this file's behavior

    private fun targetTestJson() = """  // line 144: executes this statement as part of this file's behavior
        {  // line 145: executes this statement as part of this file's behavior
          "ok": true,  // line 146: executes this statement as part of this file's behavior
          "status": "ok",  // line 147: executes this statement as part of this file's behavior
          "message": "Target works"  // line 148: executes this statement as part of this file's behavior
        }  // line 149: executes this statement as part of this file's behavior
    """.trimIndent()  // line 150: executes this statement as part of this file's behavior

    private fun secretJson() = """  // line 152: executes this statement as part of this file's behavior
        {  // line 153: executes this statement as part of this file's behavior
          "id": "secret",  // line 154: executes this statement as part of this file's behavior
          "name": "DISCORD_TOKEN",  // line 155: executes this statement as part of this file's behavior
          "type": "discord_bot_token",  // line 156: executes this statement as part of this file's behavior
          "storageMode": "local_encrypted",  // line 157: executes this statement as part of this file's behavior
          "fingerprint": "fingerprint",  // line 158: executes this statement as part of this file's behavior
          "createdAt": "2026-05-18T00:00:00Z",  // line 159: executes this statement as part of this file's behavior
          "updatedAt": "2026-05-18T00:00:00Z"  // line 160: executes this statement as part of this file's behavior
        }  // line 161: executes this statement as part of this file's behavior
    """.trimIndent()  // line 162: executes this statement as part of this file's behavior

    private fun fileJson() = """  // line 164: executes this statement as part of this file's behavior
        {  // line 165: executes this statement as part of this file's behavior
          "path": "src/index.ts",  // line 166: executes this statement as part of this file's behavior
          "size": 5,  // line 167: executes this statement as part of this file's behavior
          "updatedAt": "2026-05-18T00:00:00Z",  // line 168: executes this statement as part of this file's behavior
          "generated": false,  // line 169: executes this statement as part of this file's behavior
          "editable": true,  // line 170: executes this statement as part of this file's behavior
          "content": "hello"  // line 171: executes this statement as part of this file's behavior
        }  // line 172: executes this statement as part of this file's behavior
    """.trimIndent()  // line 173: executes this statement as part of this file's behavior

    private class TestHttpServer(private val responseFor: (String) -> String) : AutoCloseable {  // line 175: executes this statement as part of this file's behavior
        private val serverSocket = ServerSocket(0)  // line 176: executes this statement as part of this file's behavior
        val port: Int = serverSocket.localPort  // line 177: executes this statement as part of this file's behavior
        private val worker = thread(start = true, isDaemon = true, name = "botblade-test-http-server") {  // line 178: executes this statement as part of this file's behavior
            while (!serverSocket.isClosed) {  // line 179: executes this statement as part of this file's behavior
                runCatching { handleConnection() }  // line 180: executes this statement as part of this file's behavior
            }  // line 181: executes this statement as part of this file's behavior
        }  // line 182: executes this statement as part of this file's behavior

        private fun handleConnection() {  // line 184: executes this statement as part of this file's behavior
            serverSocket.accept().use { socket ->  // line 185: executes this statement as part of this file's behavior
                val reader = BufferedReader(InputStreamReader(socket.getInputStream(), Charsets.UTF_8))  // line 186: executes this statement as part of this file's behavior
                val requestLine = reader.readLine() ?: return  // line 187: executes this statement as part of this file's behavior
                while (!reader.readLine().isNullOrEmpty()) {  // line 188: executes this statement as part of this file's behavior
                    // Drain headers before responding.
                }  // line 190: executes this statement as part of this file's behavior
                val target = requestLine.split(" ").getOrElse(1) { "/" }  // line 191: executes this statement as part of this file's behavior
                val rawPath = target.substringBefore('?')  // line 192: executes this statement as part of this file's behavior
                val body = responseFor(rawPath).toByteArray(Charsets.UTF_8)  // line 193: executes this statement as part of this file's behavior
                val headers = buildString {  // line 194: executes this statement as part of this file's behavior
                    append("HTTP/1.1 200 OK\r\n")  // line 195: executes this statement as part of this file's behavior
                    append("Content-Type: application/json; charset=utf-8\r\n")  // line 196: executes this statement as part of this file's behavior
                    append("Content-Length: ${body.size}\r\n")  // line 197: executes this statement as part of this file's behavior
                    append("Connection: close\r\n")  // line 198: executes this statement as part of this file's behavior
                    append("\r\n")  // line 199: executes this statement as part of this file's behavior
                }.toByteArray(Charsets.UTF_8)  // line 200: executes this statement as part of this file's behavior
                socket.getOutputStream().use { output ->  // line 201: executes this statement as part of this file's behavior
                    output.write(headers)  // line 202: executes this statement as part of this file's behavior
                    output.write(body)  // line 203: executes this statement as part of this file's behavior
                }  // line 204: executes this statement as part of this file's behavior
            }  // line 205: executes this statement as part of this file's behavior
        }  // line 206: executes this statement as part of this file's behavior

        override fun close() {  // line 208: executes this statement as part of this file's behavior
            serverSocket.close()  // line 209: executes this statement as part of this file's behavior
            worker.join(TimeUnit.SECONDS.toMillis(1))  // line 210: executes this statement as part of this file's behavior
        }  // line 211: executes this statement as part of this file's behavior
    }  // line 212: executes this statement as part of this file's behavior
}  // line 213: executes this statement as part of this file's behavior
