package com.princess.botblade.data.repository

import android.content.Context
import org.json.JSONObject
import java.io.File

class LocalProjectRepository(private val context: Context) {
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
}
