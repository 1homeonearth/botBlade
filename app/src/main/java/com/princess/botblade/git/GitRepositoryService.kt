// BEGIN NEWBIE GUIDE HEADER
// This file is part of BotBlade. The goal of this header is to help brand-new developers
// understand what this file is responsible for before reading implementation details.
// Read top-to-bottom, and treat function/class names as a map of the app flow.
// If you edit behavior here, also check related tests and docs for consistency.
// END NEWBIE GUIDE HEADER
package com.princess.botblade.git  // line 7: executes this statement as part of this file's behavior

import kotlinx.coroutines.Dispatchers  // line 9: executes this statement as part of this file's behavior
import kotlinx.coroutines.withContext  // line 10: executes this statement as part of this file's behavior
import org.eclipse.jgit.api.Git  // line 11: executes this statement as part of this file's behavior
import org.eclipse.jgit.lib.Repository  // line 12: executes this statement as part of this file's behavior
import org.eclipse.jgit.storage.file.FileRepositoryBuilder  // line 13: executes this statement as part of this file's behavior
import java.io.File  // line 14: executes this statement as part of this file's behavior

/**  // line 16: executes this statement as part of this file's behavior
 * Small Android-native Git facade for BotBlade project workspaces.  // line 17: executes this statement as part of this file's behavior
 *  // line 18: executes this statement as part of this file's behavior
 * This class intentionally starts with read-mostly repository operations. Write operations  // line 19: executes this statement as part of this file's behavior
 * such as commit, pull, push, and credential-backed remotes should be added behind explicit  // line 20: executes this statement as part of this file's behavior
 * user confirmation and token-audit plumbing.  // line 21: executes this statement as part of this file's behavior
 */  // line 22: executes this statement as part of this file's behavior
class GitRepositoryService {  // line 23: executes this statement as part of this file's behavior
    suspend fun cloneRepository(remoteUrl: String, destination: File): GitRepositorySnapshot = withIo {  // line 24: executes this statement as part of this file's behavior
        require(remoteUrl.isNotBlank()) { "remoteUrl must not be blank." }  // line 25: executes this statement as part of this file's behavior
        if (destination.exists()) {  // line 26: executes this statement as part of this file's behavior
            require(destination.listFiles().isNullOrEmpty()) { "Destination must be empty: ${destination.absolutePath}" }  // line 27: executes this statement as part of this file's behavior
        }  // line 28: executes this statement as part of this file's behavior
        destination.parentFile?.mkdirs()  // line 29: executes this statement as part of this file's behavior
        Git.cloneRepository()  // line 30: executes this statement as part of this file's behavior
            .setURI(remoteUrl)  // line 31: executes this statement as part of this file's behavior
            .setDirectory(destination)  // line 32: executes this statement as part of this file's behavior
            .call()  // line 33: executes this statement as part of this file's behavior
            .use { git -> git.snapshot(destination) }  // line 34: executes this statement as part of this file's behavior
    }  // line 35: executes this statement as part of this file's behavior

    suspend fun readSnapshot(workspace: File): GitRepositorySnapshot = withIo {  // line 37: executes this statement as part of this file's behavior
        openGit(workspace).use { git -> git.snapshot(workspace) }  // line 38: executes this statement as part of this file's behavior
    }  // line 39: executes this statement as part of this file's behavior

    suspend fun readStatus(workspace: File): GitStatusSummary = withIo {  // line 41: executes this statement as part of this file's behavior
        openGit(workspace).use { git -> git.statusSummary() }  // line 42: executes this statement as part of this file's behavior
    }  // line 43: executes this statement as part of this file's behavior

    suspend fun readCurrentBranch(workspace: File): String = withIo {  // line 45: executes this statement as part of this file's behavior
        openRepository(workspace).use { repository -> repository.branch.orEmpty() }  // line 46: executes this statement as part of this file's behavior
    }  // line 47: executes this statement as part of this file's behavior

    private fun openGit(workspace: File): Git = Git(openRepository(workspace))  // line 49: executes this statement as part of this file's behavior

    private fun openRepository(workspace: File): Repository = FileRepositoryBuilder()  // line 51: executes this statement as part of this file's behavior
        .findGitDir(workspace)  // line 52: executes this statement as part of this file's behavior
        .readEnvironment()  // line 53: executes this statement as part of this file's behavior
        .build()  // line 54: executes this statement as part of this file's behavior

    private fun Git.snapshot(workspace: File): GitRepositorySnapshot = GitRepositorySnapshot(  // line 56: executes this statement as part of this file's behavior
        workspacePath = workspace.absolutePath,  // line 57: executes this statement as part of this file's behavior
        branch = repository.branch.orEmpty(),  // line 58: executes this statement as part of this file's behavior
        status = statusSummary(),  // line 59: executes this statement as part of this file's behavior
    )  // line 60: executes this statement as part of this file's behavior

    private fun Git.statusSummary(): GitStatusSummary {  // line 62: executes this statement as part of this file's behavior
        val status = status().call()  // line 63: executes this statement as part of this file's behavior
        return GitStatusSummary(  // line 64: executes this statement as part of this file's behavior
            clean = status.isClean,  // line 65: executes this statement as part of this file's behavior
            added = status.added.sorted(),  // line 66: executes this statement as part of this file's behavior
            changed = status.changed.sorted(),  // line 67: executes this statement as part of this file's behavior
            modified = status.modified.sorted(),  // line 68: executes this statement as part of this file's behavior
            missing = status.missing.sorted(),  // line 69: executes this statement as part of this file's behavior
            removed = status.removed.sorted(),  // line 70: executes this statement as part of this file's behavior
            untracked = status.untracked.sorted(),  // line 71: executes this statement as part of this file's behavior
            conflicting = status.conflicting.sorted(),  // line 72: executes this statement as part of this file's behavior
        )  // line 73: executes this statement as part of this file's behavior
    }  // line 74: executes this statement as part of this file's behavior

    private suspend fun <T> withIo(block: () -> T): T = withContext(Dispatchers.IO) { block() }  // line 76: executes this statement as part of this file's behavior
}  // line 77: executes this statement as part of this file's behavior

data class GitRepositorySnapshot(  // line 79: executes this statement as part of this file's behavior
    val workspacePath: String,  // line 80: executes this statement as part of this file's behavior
    val branch: String,  // line 81: executes this statement as part of this file's behavior
    val status: GitStatusSummary,  // line 82: executes this statement as part of this file's behavior
)  // line 83: executes this statement as part of this file's behavior

data class GitStatusSummary(  // line 85: executes this statement as part of this file's behavior
    val clean: Boolean,  // line 86: executes this statement as part of this file's behavior
    val added: List<String>,  // line 87: executes this statement as part of this file's behavior
    val changed: List<String>,  // line 88: executes this statement as part of this file's behavior
    val modified: List<String>,  // line 89: executes this statement as part of this file's behavior
    val missing: List<String>,  // line 90: executes this statement as part of this file's behavior
    val removed: List<String>,  // line 91: executes this statement as part of this file's behavior
    val untracked: List<String>,  // line 92: executes this statement as part of this file's behavior
    val conflicting: List<String>,  // line 93: executes this statement as part of this file's behavior
) {  // line 94: executes this statement as part of this file's behavior
    val dirtyFileCount: Int  // line 95: executes this statement as part of this file's behavior
        get() = buildSet {  // line 96: executes this statement as part of this file's behavior
            addAll(added)  // line 97: executes this statement as part of this file's behavior
            addAll(changed)  // line 98: executes this statement as part of this file's behavior
            addAll(modified)  // line 99: executes this statement as part of this file's behavior
            addAll(missing)  // line 100: executes this statement as part of this file's behavior
            addAll(removed)  // line 101: executes this statement as part of this file's behavior
            addAll(untracked)  // line 102: executes this statement as part of this file's behavior
            addAll(conflicting)  // line 103: executes this statement as part of this file's behavior
        }.size  // line 104: executes this statement as part of this file's behavior
}  // line 105: executes this statement as part of this file's behavior
