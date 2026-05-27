package com.princess.botblade.data.repository

import android.content.Context
import android.net.Uri
import org.json.JSONObject
import java.io.File
import java.util.Locale

class LocalProjectRepository(private val context: Context) {
    data class LocalProjectSummary(
        val id: String,
        val name: String,
        val lastModified: Long,
        val status: String,
    )

    data class ProjectImportResult(
        val message: String,
        val project: LocalProjectSummary,
    )

    fun createProject(projectName: String, templateAssetDir: String): File {
        val projectRoot = createProjectRoot(projectName)
        copyAssetDirectory(templateAssetDir, projectRoot)
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            sourceType = "starter_template",
            status = "Ready",
            extra = mapOf("template" to templateAssetDir.substringAfterLast('/')),
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
            .map { directory -> summaryFor(directory) }
    }

    fun findProjectByName(projectName: String): LocalProjectSummary? =
        listProjects().firstOrNull { it.name == projectName }

    fun renameProject(projectId: String, newName: String): Boolean {
        val current = File(projectsRoot(), projectId)
        val renamed = File(projectsRoot(), safeProjectId(newName))
        val renamedCleanly = current.exists() && !renamed.exists() && current.renameTo(renamed)
        if (renamedCleanly) {
            val metadata = readMetadata(renamed) ?: JSONObject()
            metadata
                .put("name", renamed.name)
                .put("updatedAt", System.currentTimeMillis())
            File(renamed, METADATA_FILE).writeText(metadata.toString(2))
        }
        return renamedCleanly
    }

    fun deleteProject(projectId: String): Boolean =
        File(projectsRoot(), projectId).deleteRecursively()

    fun importFromGit(repositoryUrl: String, branch: String?): ProjectImportResult {
        val projectRoot = createProjectRoot(repositoryUrl.substringAfterLast('/').removeSuffix(".git").ifBlank { "git-project" })
        writeReadme(
            projectRoot,
            "# ${projectRoot.name}\n\nThis BotBlade project was registered from Git.\n\nRepository: $repositoryUrl\nBranch: ${branch?.ifBlank { "default" } ?: "default"}\n\nFull clone support can attach here through the JGit-backed importer.\n",
        )
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            sourceType = "git",
            status = "Registered Git source",
            extra = mapOf(
                "repositoryUrl" to repositoryUrl,
                "branch" to (branch?.takeIf { it.isNotBlank() } ?: ""),
            ),
        )
        return ProjectImportResult("Registered ${projectRoot.name} from Git and added it to Projects.", summaryFor(projectRoot))
    }

    fun importFromZip(zipUri: Uri): ProjectImportResult {
        val projectRoot = createProjectRoot(zipUri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.') ?: "zip-import")
        writeReadme(
            projectRoot,
            "# ${projectRoot.name}\n\nThis BotBlade project was registered from a ZIP source.\n\nSource URI: $zipUri\n\nArchive extraction can attach here through the ZIP import adapter.\n",
        )
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            sourceType = "zip",
            status = "Registered ZIP source",
            extra = mapOf("sourceUri" to zipUri.toString()),
        )
        return ProjectImportResult("Registered ${projectRoot.name} from ZIP and added it to Projects.", summaryFor(projectRoot))
    }

    fun registerWorkspaceFolder(folderUri: Uri): ProjectImportResult {
        val projectRoot = createProjectRoot(folderUri.lastPathSegment?.substringAfterLast(':') ?: "workspace-folder")
        writeReadme(
            projectRoot,
            "# ${projectRoot.name}\n\nThis BotBlade project links an Android workspace folder.\n\nFolder URI: $folderUri\n\nBotBlade can use this project shell for scanning, secrets, builds, and future direct folder sync.\n",
        )
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            sourceType = "folder",
            status = "Registered workspace folder",
            extra = mapOf("folderUri" to folderUri.toString()),
        )
        return ProjectImportResult("Registered ${projectRoot.name} as a workspace folder project.", summaryFor(projectRoot))
    }

    fun repairWorkspace(folderUri: Uri): ProjectImportResult {
        val projectRoot = createProjectRoot(folderUri.lastPathSegment?.substringAfterLast(':') ?: "repaired-workspace")
        val repairReport = "BotBlade repair shell created.\n\nSource URI: $folderUri\n\nGenerated metadata, README, and project registration.\nNext: open Editor, run Scan, then add missing secrets.\n"
        File(projectRoot, "BOTBLADE_REPAIR_REPORT.txt").writeText(repairReport)
        writeReadme(projectRoot, "# ${projectRoot.name}\n\n$repairReport")
        writeMetadata(
            projectRoot = projectRoot,
            name = projectRoot.name,
            sourceType = "repair",
            status = "Repair shell ready",
            extra = mapOf("folderUri" to folderUri.toString()),
        )
        return ProjectImportResult("Created repair shell ${projectRoot.name} and added it to Projects.", summaryFor(projectRoot))
    }

    private fun createProjectRoot(rawName: String): File {
        val baseId = safeProjectId(rawName)
        var candidate = baseId
        var index = 2
        while (File(projectsRoot(), candidate).exists()) {
            candidate = "$baseId-$index"
            index += 1
        }
        return File(projectsRoot(), candidate).also { it.mkdirs() }
    }

    private fun writeMetadata(projectRoot: File, name: String, sourceType: String, status: String, extra: Map<String, String> = emptyMap()) {
        val now = System.currentTimeMillis()
        val metadata = JSONObject()
            .put("name", name)
            .put("sourceType", sourceType)
            .put("status", status)
            .put("createdAt", now)
            .put("updatedAt", now)
        extra.forEach { (key, value) -> metadata.put(key, value) }
        File(projectRoot, METADATA_FILE).writeText(metadata.toString(2))
    }

    private fun readMetadata(projectRoot: File): JSONObject? =
        runCatching {
            val file = File(projectRoot, METADATA_FILE)
            if (!file.exists()) null else JSONObject(file.readText())
        }.getOrNull()

    private fun summaryFor(projectRoot: File): LocalProjectSummary {
        val metadata = readMetadata(projectRoot)
        return LocalProjectSummary(
            id = projectRoot.name,
            name = metadata?.optString("name")?.takeIf { it.isNotBlank() } ?: projectRoot.name,
            lastModified = projectRoot.lastModified(),
            status = metadata?.optString("status")?.takeIf { it.isNotBlank() } ?: "Ready",
        )
    }

    private fun writeReadme(projectRoot: File, content: String) {
        File(projectRoot, "README.md").writeText(content)
    }

    private fun projectsRoot(): File = File(context.filesDir, "projects").also { it.mkdirs() }

    private fun safeProjectId(rawName: String): String {
        val cleaned = rawName
            .trim()
            .lowercase(Locale.US)
            .replace(Regex("[^a-z0-9._ -]"), "-")
            .replace(Regex("[ _]+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-', '.')
        return cleaned.ifBlank { "botblade-project" }
    }

    private companion object {
        const val METADATA_FILE = "project-metadata.json"
    }
}