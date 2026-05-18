package com.princess.royalscepter.data.api

import com.princess.royalscepter.data.model.BotToggleRequest
import com.princess.royalscepter.data.model.CommandCreateRequest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RoyalScepterApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: RoyalScepterApiClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = RoyalScepterApiClient(baseUrl = server.url("/").toString())
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun getHealthParsesBackendHealthResponse() {
        server.enqueueJson("""
            {
              "service": "royal-scepter-backend",
              "message": "ready"
            }
        """)

        val health = client.getHealth()

        assertEquals("royal-scepter-backend", health.status)
        assertEquals("ready", health.message)
    }

    @Test
    fun toggleBotParsesCompatibilityResponseAliases() {
        server.enqueueJson("""
            {
              "bot_status": "running",
              "action": "start",
              "message": "Bot started"
            }
        """)

        val toggle = client.toggleBot(BotToggleRequest(action = "start"))

        assertEquals("running", toggle.status)
        assertEquals("start", toggle.action)
        assertEquals("Bot started", toggle.message)
    }

    @Test
    fun listProjectsParsesNestedProjectConfiguration() {
        server.enqueueJson("""
            {
              "projects": [
                {
                  "id": "project 1/alpha",
                  "name": "Princess Helper",
                  "slug": "princess-helper",
                  "description": "Discord helper bot",
                  "templateId": "template_blank_discord_ts",
                  "language": "typescript",
                  "runtime": "node22",
                  "discord": {
                    "applicationId": "app-123",
                    "clientId": "client-123",
                    "defaultGuildId": "guild-123",
                    "tokenSecretRef": "secret-token",
                    "commandRegistration": "global"
                  },
                  "permissions": {
                    "intents": ["Guilds", "GuildMessages"],
                    "botPermissions": ["SendMessages"]
                  },
                  "commands": [
                    {
                      "id": "cmd-ping",
                      "name": "ping",
                      "description": "Replies pong",
                      "type": "chat_input",
                      "permissions": {
                        "defaultMemberPermissions": "0",
                        "dmPermission": true
                      },
                      "handler": {
                        "kind": "static_response",
                        "ephemeral": false,
                        "content": "pong"
                      }
                    }
                  ],
                  "deployment": {
                    "targetId": "target-local",
                    "lastDeploymentId": "deploy-1"
                  },
                  "github": {
                    "owner": "Princess",
                    "repo": "royal-bot",
                    "defaultBranch": "main",
                    "lastPushedAt": "2026-05-18T09:00:00Z"
                  },
                  "createdAt": "2026-05-18T08:00:00Z",
                  "updatedAt": "2026-05-18T09:00:00Z"
                }
              ]
            }
        """)

        val project = client.listProjects().single()

        assertEquals("project 1/alpha", project.id)
        assertEquals("Princess Helper", project.name)
        assertEquals("global", project.discord.commandRegistration)
        assertEquals(listOf("Guilds", "GuildMessages"), project.permissions.intents)
        assertEquals("target-local", project.deployment.targetId)
        assertEquals("Princess", project.github?.owner)
        assertEquals("royal-bot", project.github?.repo)
        val command = project.commands.single()
        assertEquals("ping", command.name)
        assertTrue(command.permissions.dmPermission)
        assertFalse(command.handler.ephemeral)
        assertEquals("pong", command.handler.content)
    }

    @Test
    fun listSecretsParsesRedactedSecretSummaries() {
        server.enqueueJson("""
            {
              "secrets": [
                {
                  "id": "secret-1",
                  "projectId": "project-1",
                  "name": "Discord token",
                  "type": "discord_bot_token",
                  "storageMode": "local_dev_memory",
                  "fingerprint": "sha256:abc123",
                  "createdAt": "2026-05-18T08:00:00Z",
                  "updatedAt": "2026-05-18T09:00:00Z",
                  "rotatedAt": null
                }
              ]
            }
        """)

        val secret = client.listSecrets().single()

        assertEquals("secret-1", secret.id)
        assertEquals("project-1", secret.projectId)
        assertEquals("Discord token", secret.name)
        assertEquals("local_dev_memory", secret.storageMode)
        assertEquals("sha256:abc123", secret.fingerprint)
        assertNull(secret.rotatedAt)
    }

    @Test
    fun getProjectFileUsesPercentEncodedProjectAndFilePaths() {
        server.enqueueJson("""
            {
              "path": "src/commands/hello world.ts",
              "size": 12,
              "updatedAt": "2026-05-18T09:00:00Z",
              "generated": true,
              "editable": true,
              "content": "export {};"
            }
        """)

        val file = client.getProjectFile("project id/with slash", "src/commands/hello world+token.ts")
        val request = server.takeRequest()

        assertEquals("/api/projects/project%20id%2Fwith%20slash/files/src/commands/hello%20world%2Btoken.ts", request.path)
        assertEquals("export {};", file.content)
    }

    @Test
    fun createCommandUsesPercentEncodedPathSegments() {
        server.enqueueJson("""
            {
              "id": "command/id",
              "name": "hello",
              "description": "Greets users",
              "handler": { "kind": "static_response", "content": "hi", "ephemeral": true }
            }
        """)

        val command = client.updateCommand(
            projectId = "project id/with slash",
            commandId = "command id/with slash",
            command = CommandCreateRequest(name = "hello", description = "Greets users", handlerContent = "hi"),
        )
        val request = server.takeRequest()

        assertEquals("/api/projects/project%20id%2Fwith%20slash/commands/command%20id%2Fwith%20slash", request.path)
        assertEquals("hello", command.name)
    }

    private fun MockWebServer.enqueueJson(body: String) {
        enqueue(
            MockResponse()
                .setResponseCode(200)
                .setHeader("content-type", "application/json")
                .setBody(body.trimIndent()),
        )
    }
}
