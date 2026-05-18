package com.princess.royalscepter.data.api

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoyalScepterApiClientPathTest {
    private val requestPaths = LinkedBlockingQueue<String>()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/") { exchange ->
            requestPaths.add(exchange.requestURI.rawPath)
            exchange.respondJson(responseFor(exchange.requestURI.rawPath))
        }
        start()
    }
    private val client = RoyalScepterApiClient(
        baseUrl = "http://127.0.0.1:${server.address.port}",
        bearerToken = "test-token",
        sessionToken = "",
    )

    @After
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun projectFilePathsEncodeSpecialCharactersButPreserveFilePathSlashes() {
        client.getProjectFile(
            projectId = "project #?/space%雪",
            filePath = "src/main file #?.kt/folder%/emoji-☃.txt",
        )

        assertEquals(
            "/api/projects/project%20%23%3F%2Fspace%25%E9%9B%AA/files/src/main%20file%20%23%3F.kt/folder%25/emoji-%E2%98%83.txt",
            takeRequestPath(),
        )
    }

    @Test
    fun saveProjectFileUsesTheSameSafeFilePathEncoding() {
        client.saveProjectFile(
            projectId = "project %/雪",
            filePath = "docs/space #? %/雪.md",
            content = "hello",
        )

        assertEquals(
            "/api/projects/project%20%25%2F%E9%9B%AA/files/docs/space%20%23%3F%20%25/%E9%9B%AA.md",
            takeRequestPath(),
        )
    }

    @Test
    fun projectCommandBuildDeploymentTargetAndSecretIdsAreEncodedAsSinglePathSegments() {
        client.getProject("project #?%/雪")
        client.updateCommand("project #?%/雪", "command #?%/雪", commandRequest())
        client.getBuildLogs("project #?%/雪", "build #?%/雪")
        client.getDeploymentLogs("project #?%/雪", "deployment #?%/雪")
        client.testDeploymentTarget("target #?%/雪")
        client.rotateSecret("secret #?%/雪", "value")

        assertEquals("/api/projects/project%20%23%3F%25%2F%E9%9B%AA", takeRequestPath())
        assertEquals(
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/commands/command%20%23%3F%25%2F%E9%9B%AA",
            takeRequestPath(),
        )
        assertEquals(
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/builds/build%20%23%3F%25%2F%E9%9B%AA/logs",
            takeRequestPath(),
        )
        assertEquals(
            "/api/projects/project%20%23%3F%25%2F%E9%9B%AA/deployments/deployment%20%23%3F%25%2F%E9%9B%AA/logs",
            takeRequestPath(),
        )
        assertEquals("/api/deployment-targets/target%20%23%3F%25%2F%E9%9B%AA/test", takeRequestPath())
        assertEquals("/api/secrets/secret%20%23%3F%25%2F%E9%9B%AA/rotate", takeRequestPath())
    }

    private fun takeRequestPath(): String {
        val path = requestPaths.poll(2, TimeUnit.SECONDS)
        assertNotNull(path)
        return path ?: error("Expected request path to be captured")
    }

    private fun commandRequest() = com.princess.royalscepter.data.model.CommandCreateRequest(
        name = "ping",
        description = "Ping command",
        handlerKind = "static_response",
        handlerContent = "pong",
        ephemeral = true,
    )

    private fun responseFor(path: String): String = when {
        path.contains("/commands/") -> commandJson()
        path.contains("/builds/") -> buildJson()
        path.contains("/deployments/") -> deploymentJson()
        path.contains("/deployment-targets/") -> targetTestJson()
        path.contains("/secrets/") -> secretJson()
        path.contains("/files/") -> fileJson()
        else -> projectJson()
    }

    private fun HttpExchange.respondJson(body: String) {
        val bytes = body.toByteArray(Charsets.UTF_8)
        responseHeaders.add("Content-Type", "application/json")
        sendResponseHeaders(200, bytes.size.toLong())
        responseBody.use { it.write(bytes) }
    }

    private fun projectJson() = """
        {
          "id": "project",
          "name": "Project",
          "slug": "project",
          "description": "Test project",
          "createdAt": "2026-05-18T00:00:00Z",
          "updatedAt": "2026-05-18T00:00:00Z"
        }
    """.trimIndent()

    private fun commandJson() = """
        {
          "id": "command",
          "name": "ping",
          "description": "Ping command",
          "handler": { "kind": "static_response", "content": "pong", "ephemeral": true }
        }
    """.trimIndent()

    private fun buildJson() = """
        {
          "buildId": "build",
          "projectId": "project",
          "status": "succeeded"
        }
    """.trimIndent()

    private fun deploymentJson() = """
        {
          "deploymentId": "deployment",
          "projectId": "project",
          "targetId": "target",
          "buildId": "build",
          "status": "succeeded"
        }
    """.trimIndent()

    private fun targetTestJson() = """
        {
          "ok": true,
          "status": "ok",
          "message": "Target works"
        }
    """.trimIndent()

    private fun secretJson() = """
        {
          "id": "secret",
          "name": "DISCORD_TOKEN",
          "type": "discord_bot_token",
          "storageMode": "local_encrypted",
          "fingerprint": "fingerprint",
          "createdAt": "2026-05-18T00:00:00Z",
          "updatedAt": "2026-05-18T00:00:00Z"
        }
    """.trimIndent()

    private fun fileJson() = """
        {
          "path": "src/index.ts",
          "size": 5,
          "updatedAt": "2026-05-18T00:00:00Z",
          "generated": false,
          "editable": true,
          "content": "hello"
        }
    """.trimIndent()
}
