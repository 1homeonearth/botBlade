package com.princess.botblade.git

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.storage.file.FileRepositoryBuilder
import java.io.File

/**
 * Small Android-native Git facade for BotBlade project workspaces.
 *
 * This class intentionally starts with read-mostly repository operations. Write operations
 * such as commit, pull, push, and credential-backed remotes should be added behind explicit
 * user confirmation and token-audit plumbing.
 */
class GitRepositoryService {
    suspend fun cloneRepository(remoteUrl: String, destination: File): GitRepositorySnapshot = withIo {
        require(remoteUrl.isNotBlank()) { "remoteUrl must not be blank." }
        if (destination.exists()) {
            require(destination.listFiles().isNullOrEmpty()) { "Destination must be empty: ${destination.absolutePath}" }
        }
        destination.parentFile?.mkdirs()
        Git.cloneRepository()
            .setURI(remoteUrl)
            .setDirectory(destination)
            .call()
            .use { git -> git.snapshot(destination) }
    }

    suspend fun readSnapshot(workspace: File): GitRepositorySnapshot = withIo {
        openGit(workspace).use { git -> git.snapshot(workspace) }
    }

    suspend fun readStatus(workspace: File): GitStatusSummary = withIo {
        openGit(workspace).use { git -> git.statusSummary() }
    }

    suspend fun readCurrentBranch(workspace: File): String = withIo {
        openRepository(workspace).use { repository -> repository.branch.orEmpty() }
    }

    private fun openGit(workspace: File): Git = Git(openRepository(workspace))

    private fun openRepository(workspace: File): Repository = FileRepositoryBuilder()
        .findGitDir(workspace)
        .readEnvironment()
        .build()

    private fun Git.snapshot(workspace: File): GitRepositorySnapshot = GitRepositorySnapshot(
        workspacePath = workspace.absolutePath,
        branch = repository.branch.orEmpty(),
        status = statusSummary(),
    )

    private fun Git.statusSummary(): GitStatusSummary {
        val status = status().call()
        return GitStatusSummary(
            clean = status.isClean,
            added = status.added.sorted(),
            changed = status.changed.sorted(),
            modified = status.modified.sorted(),
            missing = status.missing.sorted(),
            removed = status.removed.sorted(),
            untracked = status.untracked.sorted(),
            conflicting = status.conflicting.sorted(),
        )
    }

    private suspend fun <T> withIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }
}

data class GitRepositorySnapshot(
    val workspacePath: String,
    val branch: String,
    val status: GitStatusSummary,
)

data class GitStatusSummary(
    val clean: Boolean,
    val added: List<String>,
    val changed: List<String>,
    val modified: List<String>,
    val missing: List<String>,
    val removed: List<String>,
    val untracked: List<String>,
    val conflicting: List<String>,
) {
    val dirtyFileCount: Int
        get() = buildSet {
            addAll(added)
            addAll(changed)
            addAll(modified)
            addAll(missing)
            addAll(removed)
            addAll(untracked)
            addAll(conflicting)
        }.size
}
