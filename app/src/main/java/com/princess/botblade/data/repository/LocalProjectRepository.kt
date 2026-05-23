// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.data.repository  // line 7: executes this statement as part of this file's behavior

import android.content.Context  // line 9: executes this statement as part of this file's behavior
import org.json.JSONObject  // line 10: executes this statement as part of this file's behavior
import java.io.File  // line 11: executes this statement as part of this file's behavior

class LocalProjectRepository(private val context: Context) {  // line 13: executes this statement as part of this file's behavior
    data class LocalProjectSummary(  // line 14: executes this statement as part of this file's behavior
        val id: String,  // line 15: executes this statement as part of this file's behavior
        val name: String,  // line 16: executes this statement as part of this file's behavior
        val lastModified: Long,  // line 17: executes this statement as part of this file's behavior
        val status: String,  // line 18: executes this statement as part of this file's behavior
    )  // line 19: executes this statement as part of this file's behavior

    fun createProject(projectName: String, templateAssetDir: String): File {  // line 21: executes this statement as part of this file's behavior
        val projectRoot = File(context.filesDir, "projects/$projectName")  // line 22: executes this statement as part of this file's behavior
        projectRoot.mkdirs()  // line 23: executes this statement as part of this file's behavior
        copyAssetDirectory(templateAssetDir, projectRoot)  // line 24: executes this statement as part of this file's behavior
        val metadata = JSONObject()  // line 25: executes this statement as part of this file's behavior
            .put("name", projectName)  // line 26: executes this statement as part of this file's behavior
            .put("template", templateAssetDir.substringAfterLast('/'))  // line 27: executes this statement as part of this file's behavior
            .put("createdAt", System.currentTimeMillis())  // line 28: executes this statement as part of this file's behavior
        File(projectRoot, "project-metadata.json").writeText(metadata.toString(2))  // line 29: executes this statement as part of this file's behavior
        return projectRoot  // line 30: executes this statement as part of this file's behavior
    }  // line 31: executes this statement as part of this file's behavior

    private fun copyAssetDirectory(assetPath: String, destination: File) {  // line 33: executes this statement as part of this file's behavior
        val children = context.assets.list(assetPath).orEmpty()  // line 34: executes this statement as part of this file's behavior
        if (children.isEmpty()) {  // line 35: executes this statement as part of this file's behavior
            destination.parentFile?.mkdirs()  // line 36: executes this statement as part of this file's behavior
            context.assets.open(assetPath).use { input -> destination.outputStream().use { output -> input.copyTo(output) } }  // line 37: executes this statement as part of this file's behavior
            return  // line 38: executes this statement as part of this file's behavior
        }  // line 39: executes this statement as part of this file's behavior
        destination.mkdirs()  // line 40: executes this statement as part of this file's behavior
        for (child in children) {  // line 41: executes this statement as part of this file's behavior
            val childAssetPath = "$assetPath/$child"  // line 42: executes this statement as part of this file's behavior
            copyAssetDirectory(childAssetPath, File(destination, child))  // line 43: executes this statement as part of this file's behavior
        }  // line 44: executes this statement as part of this file's behavior
    }  // line 45: executes this statement as part of this file's behavior

    fun listProjects(): List<LocalProjectSummary> {  // line 47: executes this statement as part of this file's behavior
        val projectsRoot = File(context.filesDir, "projects")  // line 48: executes this statement as part of this file's behavior
        if (!projectsRoot.exists()) return emptyList()  // line 49: executes this statement as part of this file's behavior
        return projectsRoot.listFiles()  // line 50: executes this statement as part of this file's behavior
            .orEmpty()  // line 51: executes this statement as part of this file's behavior
            .filter { it.isDirectory }  // line 52: executes this statement as part of this file's behavior
            .sortedByDescending { it.lastModified() }  // line 53: executes this statement as part of this file's behavior
            .map { directory ->  // line 54: executes this statement as part of this file's behavior
                LocalProjectSummary(  // line 55: executes this statement as part of this file's behavior
                    id = directory.name,  // line 56: executes this statement as part of this file's behavior
                    name = directory.name,  // line 57: executes this statement as part of this file's behavior
                    lastModified = directory.lastModified(),  // line 58: executes this statement as part of this file's behavior
                    status = "Ready",  // line 59: executes this statement as part of this file's behavior
                )  // line 60: executes this statement as part of this file's behavior
            }  // line 61: executes this statement as part of this file's behavior
    }  // line 62: executes this statement as part of this file's behavior

    fun findProjectByName(projectName: String): LocalProjectSummary? =  // line 64: executes this statement as part of this file's behavior
        listProjects().firstOrNull { it.name == projectName }  // line 65: executes this statement as part of this file's behavior

    fun renameProject(projectId: String, newName: String): Boolean {  // line 67: executes this statement as part of this file's behavior
        val current = File(context.filesDir, "projects/$projectId")  // line 68: executes this statement as part of this file's behavior
        val renamed = File(context.filesDir, "projects/$newName")  // line 69: executes this statement as part of this file's behavior
        return current.exists() && !renamed.exists() && current.renameTo(renamed)  // line 70: executes this statement as part of this file's behavior
    }  // line 71: executes this statement as part of this file's behavior

    fun deleteProject(projectId: String): Boolean =  // line 73: executes this statement as part of this file's behavior
        File(context.filesDir, "projects/$projectId").deleteRecursively()  // line 74: executes this statement as part of this file's behavior
}  // line 75: executes this statement as part of this file's behavior
