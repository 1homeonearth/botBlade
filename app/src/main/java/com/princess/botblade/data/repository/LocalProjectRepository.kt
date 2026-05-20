package com.princess.botblade.data.repository

import android.content.Context
import org.json.JSONObject
import java.io.File

class LocalProjectRepository(private val context: Context) {
    data class LocalProjectSummary(
        val id: String,
        val name: String,
        val lastModified: Long,
        val status: String,
    )

    fun createProject(projectName: String, templateAssetDir: String): File {
        val projectRoot = File(context.filesDir, "projects/$projectName")
        projectRoot.mkdirs()
        copyAssetDirectory(templateAssetDir, projectRoot)
        val metadata = JSONObject()
            .put("name", projectName)
            .put("template", templateAssetDir.substringAfterLast('/'))
            .put("createdAt", System.currentTimeMillis())
        File(projectRoot, "project-metadata.json").writeText(metadata.toString(2))
        return projectRoot
    }

    private fun copyAssetDirectory(assetPath: String, destination: File) {
        val children = context.assets.list(assetPath).orEmpty()
        if (children.isEmpty()) {
            destination.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input -> destination.outputStream().use { output -> input.copyTo(output) } }
            return
        }
        destination.mkdirs()
        for (child in children) {
            val childAssetPath = "$assetPath/$child"
            copyAssetDirectory(childAssetPath, File(destination, child))
        }
    }

    fun listProjects(): List<LocalProjectSummary> {
        val projectsRoot = File(context.filesDir, "projects")
        if (!projectsRoot.exists()) return emptyList()
        return projectsRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .sortedByDescending { it.lastModified() }
            .map { directory ->
                LocalProjectSummary(
                    id = directory.name,
                    name = directory.name,
                    lastModified = directory.lastModified(),
                    status = "Ready",
                )
            }
    }

    fun findProjectByName(projectName: String): LocalProjectSummary? =
        listProjects().firstOrNull { it.name == projectName }

    fun renameProject(projectId: String, newName: String): Boolean {
        val current = File(context.filesDir, "projects/$projectId")
        val renamed = File(context.filesDir, "projects/$newName")
        return current.exists() && !renamed.exists() && current.renameTo(renamed)
    }

    fun deleteProject(projectId: String): Boolean =
        File(context.filesDir, "projects/$projectId").deleteRecursively()
}
