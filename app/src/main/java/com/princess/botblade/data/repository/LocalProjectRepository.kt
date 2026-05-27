package com.princess.botblade.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.zip.ZipInputStream

class LocalProjectRepository(private val context: Context) {
    data class LocalProjectSummary(
        val id: String,
        val name: String,
        val lastModified: Long,
        val status: String,
    )

    fun createProject(projectName: String, templateAssetDir: String): File {
        val projectRoot = uniqueProjectRoot(projectName)
        projectRoot.mkdirs()
        copyAssetDirectory(templateAssetDir, projectRoot)
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            status = "Ready",
            source = "template",
            details = JSONObject()
                .put("template", templateAssetDir.substringAfterLast('/')),
        )
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
        val projectsRoot = projectsRoot()
        if (!projectsRoot.exists()) return emptyList()
        return projectsRoot.listFiles()
            .orEmpty()
            .filter { it.isDirectory }
            .sortedByDescending { it.lastModified() }
            .map { directory ->
                val metadata = readMetadata(directory)
                LocalProjectSummary(
                    id = directory.name,
                    name = metadata?.optString("name")?.takeIf { it.isNotBlank() } ?: directory.name,
                    lastModified = directory.lastModified(),
                    status = metadata?.optString("status")?.takeIf { it.isNotBlank() } ?: "Ready",
                )
            }
    }

    fun findProjectByName(projectName: String): LocalProjectSummary? =
        listProjects().firstOrNull { it.name == projectName }

    fun renameProject(projectId: String, newName: String): Boolean {
        val current = File(context.filesDir, "projects/$projectId")
        val renamed = File(context.filesDir, "projects/${safeProjectName(newName)}")
        if (!current.exists() || renamed.exists() || !current.renameTo(renamed)) return false
        val metadata = readMetadata(renamed) ?: JSONObject()
        metadata.put("name", renamed.name)
        metadata.put("updatedAt", System.currentTimeMillis())
        File(renamed, METADATA_FILE).writeText(metadata.toString(2))
        return true
    }

    fun deleteProject(projectId: String): Boolean =
        File(context.filesDir, "projects/$projectId").deleteRecursively()

    fun importFromGit(repositoryUrl: String, branch: String?): String = runCatching {
        require(repositoryUrl.startsWith("https://") || repositoryUrl.startsWith("http://") || repositoryUrl.startsWith("git@")) {
            "Git import needs an http(s) or git@ repository URL."
        }
        val name = gitProjectName(repositoryUrl)
        val projectRoot = uniqueProjectRoot(name)
        projectRoot.mkdirs()
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            status = "Git linked",
            source = "git",
            details = JSONObject()
                .put("repositoryUrl", repositoryUrl)
                .put("branch", branch?.takeIf { it.isNotBlank() } ?: "default"),
        )
        writeBotBladeFile(
            projectRoot = projectRoot,
            name = "git-import.json",
            json = JSONObject()
                .put("repositoryUrl", repositoryUrl)
                .put("branch", branch?.takeIf { it.isNotBlank() } ?: JSONObject.NULL)
                .put("createdAt", System.currentTimeMillis()),
        )
        File(projectRoot, "README.md").writeText(
            "# ${projectRoot.name}\n\nBotBlade registered this Git repository for import.\n\nRepository: $repositoryUrl\nBranch: ${branch?.takeIf { it.isNotBlank() } ?: "default"}\n\nNext: open this project in Forge Editor, run a scan, then wire clone/sync through the Git backend.\n",
        )
        "Git repository registered as ${projectRoot.name}. Open it, scan it, and continue from Forge Editor."
    }.getOrElse { error -> "Git import failed: ${error.message ?: "unknown error"}" }

    fun importFromZip(zipUri: Uri): String = runCatching {
        val displayName = displayName(zipUri) ?: "imported-zip"
        val projectRoot = uniqueProjectRoot(displayName.substringBeforeLast('.'))
        projectRoot.mkdirs()
        var fileCount = 0
        val skipped = mutableListOf<String>()
        context.contentResolver.openInputStream(zipUri).use { rawInput ->
            require(rawInput != null) { "Unable to open ZIP file." }
            ZipInputStream(rawInput).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    val safeEntry = safeZipEntryPath(entry.name)
                    if (safeEntry == null) {
                        skipped += entry.name
                        zip.closeEntry()
                        continue
                    }
                    val target = File(projectRoot, safeEntry)
                    if (entry.isDirectory) {
                        target.mkdirs()
                    } else {
                        target.parentFile?.mkdirs()
                        target.outputStream().use { output -> zip.copyTo(output) }
                        fileCount += 1
                    }
                    zip.closeEntry()
                }
            }
        }
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            status = if (fileCount > 0) "Imported ZIP" else "ZIP empty",
            source = "zip",
            details = JSONObject()
                .put("sourceUri", zipUri.toString())
                .put("sourceName", displayName)
                .put("fileCount", fileCount)
                .put("skippedEntries", JSONArray(skipped)),
        )
        if (fileCount == 0) {
            File(projectRoot, "README.md").writeText("# ${projectRoot.name}\n\nBotBlade imported this ZIP, but no files were extracted.\n")
        }
        "Imported ZIP as ${projectRoot.name} with $fileCount file(s). ${if (skipped.isEmpty()) "" else "Skipped ${skipped.size} unsafe path(s)."}".trim()
    }.getOrElse { error -> "ZIP import failed: ${error.message ?: "unknown error"}" }

    fun registerWorkspaceFolder(folderUri: Uri): String = runCatching {
        val name = folderProjectName(folderUri, "linked-workspace")
        val projectRoot = uniqueProjectRoot(name)
        projectRoot.mkdirs()
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            status = "Folder linked",
            source = "folder",
            details = JSONObject()
                .put("folderUri", folderUri.toString()),
        )
        writeBotBladeFile(
            projectRoot = projectRoot,
            name = "folder-link.json",
            json = JSONObject()
                .put("folderUri", folderUri.toString())
                .put("createdAt", System.currentTimeMillis()),
        )
        File(projectRoot, "README.md").writeText(
            "# ${projectRoot.name}\n\nBotBlade linked this Android folder as an external workspace.\n\nFolder URI: $folderUri\n\nNext: open the project, run scan, then repair or generate missing BotBlade metadata.\n",
        )
        "Registered folder as ${projectRoot.name}. Open it in Forge Editor to scan and repair."
    }.getOrElse { error -> "Folder registration failed: ${error.message ?: "unknown error"}" }

    fun repairWorkspace(folderUri: Uri): String = runCatching {
        val name = folderProjectName(folderUri, "repaired-workspace")
        val projectRoot = uniqueProjectRoot("$name-repair")
        projectRoot.mkdirs()
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            status = "Repair staged",
            source = "repair",
            details = JSONObject()
                .put("folderUri", folderUri.toString()),
        )
        writeBotBladeFile(
            projectRoot = projectRoot,
            name = "repair-plan.json",
            json = JSONObject()
                .put("folderUri", folderUri.toString())
                .put("checks", JSONArray(listOf("detect-runtime", "find-package-files", "find-secrets", "generate-metadata")))
                .put("createdAt", System.currentTimeMillis()),
        )
        File(projectRoot, "README.md").writeText(
            "# ${projectRoot.name}\n\nBotBlade staged a repair workspace for this folder.\n\nFolder URI: $folderUri\n\nRepair plan: detect runtime, find package files, identify secrets, and generate BotBlade metadata.\n",
        )
        "Repair workspace created as ${projectRoot.name}. Open it to continue the repair scan."
    }.getOrElse { error -> "Workspace repair failed: ${error.message ?: "unknown error"}" }

    private fun projectsRoot(): File = File(context.filesDir, "projects").apply { mkdirs() }

    private fun uniqueProjectRoot(rawName: String): File {
        val baseName = safeProjectName(rawName)
        var candidate = File(projectsRoot(), baseName)
        var suffix = 2
        while (candidate.exists()) {
            candidate = File(projectsRoot(), "$baseName-$suffix")
            suffix += 1
        }
        return candidate
    }

    private fun safeProjectName(rawName: String): String {
        val cleaned = rawName
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9._ -]+"), "-")
            .replace(Regex("[\\s_]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-', '.', ' ')
        return cleaned.ifBlank { "botblade-project" }.take(80)
    }

    private fun gitProjectName(repositoryUrl: String): String = repositoryUrl
        .substringAfterLast('/')
        .substringAfterLast(':')
        .removeSuffix(".git")
        .ifBlank { "git-project" }

    private fun folderProjectName(uri: Uri, fallback: String): String {
        val lastSegment = uri.lastPathSegment
            ?.substringAfterLast('/')
            ?.substringAfterLast(':')
            ?.takeIf { it.isNotBlank() }
        return lastSegment ?: fallback
    }

    private fun safeZipEntryPath(entryName: String): String? {
        val normalized = entryName.replace('\\', '/')
        if (normalized.startsWith("/") || normalized.contains("../") || normalized == ".." || normalized.contains(":")) return null
        return normalized.trim('/').takeIf { it.isNotBlank() }
    }

    private fun displayName(uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) return cursor.getString(index)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')
    }

    private fun writeMetadata(projectRoot: File, name: String, status: String, source: String, details: JSONObject) {
        val metadata = JSONObject()
            .put("name", name)
            .put("status", status)
            .put("source", source)
            .put("details", details)
            .put("createdAt", System.currentTimeMillis())
            .put("updatedAt", System.currentTimeMillis())
        File(projectRoot, METADATA_FILE).writeText(metadata.toString(2))
    }

    private fun writeBotBladeFile(projectRoot: File, name: String, json: JSONObject) {
        val directory = File(projectRoot, ".botblade").apply { mkdirs() }
        File(directory, name).writeText(json.toString(2))
    }

    private fun readMetadata(projectRoot: File): JSONObject? = runCatching {
        File(projectRoot, METADATA_FILE).takeIf { it.exists() }?.readText()?.let(::JSONObject)
    }.getOrNull()

    private companion object {
        const val METADATA_FILE = "project-metadata.json"
    }
}
