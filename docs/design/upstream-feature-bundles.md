# Upstream feature bundles: JGit, Fossify, and Acode

This document turns three upstream projects into legally safe BotBlade implementation bundles. It captures what can be embedded directly, what must stay as reference-only design input, and exact original code scaffolding that Codex can add without copying upstream GPL source.

## Upstream links

- JGit: https://github.com/eclipse-jgit/jgit
- Fossify organization: https://github.com/FossifyOrg
- Fossify File Manager: https://github.com/FossifyOrg/File-Manager
- Fossify Gallery: https://github.com/FossifyOrg/Gallery
- Acode: https://github.com/Acode-Foundation/Acode
- Acode plugin docs: https://docs.acode.app

## License posture

JGit may be embedded as a normal dependency if its current published artifact license remains compatible with BotBlade. The upstream repository describes JGit as licensed under the Eclipse Distribution License 1.0, BSD-3-Clause, and explicitly says the core package accepts no GPL, LGPL, or EPL contributions.

Acode may be used as design inspiration and may be embedded where exact MIT license attribution is preserved. Prefer original Kotlin/Compose implementation for BotBlade because Acode is a Cordova/web-heavy editor architecture and BotBlade is an Android-first native forge.

Fossify apps are GPL-3.0 in the relevant Android app repositories. GPL code must not be copied, translated, lightly modified, embedded, vendored, or mixed into BotBlade unless BotBlade intentionally adopts GPL-compatible distribution obligations. Changing GPL source until it looks unique is still derivative-risk work and is not a safe path. BotBlade may implement original UX and architecture that provides comparable file-picker, gallery, notes, theming, and privacy-respecting flows without copying Fossify code or assets.

## Feature bundle 1: JGit native repo forge

Goal: make BotBlade import, inspect, branch, diff, commit, and sync bot repositories locally without shelling out to Git.

Target capabilities:

- Clone/import repository by HTTPS URL.
- Open existing local repository.
- List branches and current HEAD.
- Inspect status for changed, untracked, missing, staged, and conflicted files.
- Stage selected files.
- Commit with author identity supplied by BotBlade settings.
- Create and switch branches.
- Fetch/pull/push through a credential boundary controlled by BotBlade secrets.
- Produce structured audit events for every mutating Git operation.

Implementation notes:

- Use JGit from the Android/backend layer where it fits best after build compatibility is confirmed.
- Never pass raw secret values into logs, summaries, screenshots, or audit payloads.
- Keep destructive operations behind explicit confirmation and audit trail.
- Treat imported repositories as untrusted until the repo scanner approves them.

## Feature bundle 2: Fossify-style Android file and media ergonomics

Goal: bring the useful Fossify product feeling into BotBlade through original implementation: privacy-first, ad-free, local-first file handling with simple, colorful, fast surfaces.

Target capabilities:

- Project file browser with copy, move, rename, delete, and new file/folder actions.
- Storage Access Framework import/export flows.
- Recent projects and pinned locations.
- Local media/document preview surfaces for project assets.
- Per-project accent color and icon metadata.
- No telemetry, ads, trackers, or bundled proprietary SDKs.
- Clear permission explainer screens before storage/media access.

Legal rule:

- Reference Fossify behavior and repository structure for product thinking only. Do not copy source code, resources, strings, icons, layouts, or assets.

## Feature bundle 3: Acode-inspired mobile editor workbench

Goal: make BotBlade feel like a real Android coding environment: multi-file editor, syntax awareness, preview, command palette, plugin-style extensions, and mobile-friendly navigation.

Target capabilities:

- Tabbed editor model.
- Project tree + outline drawer.
- Language mode detection by extension and shebang.
- Syntax highlighting abstraction.
- Find/replace across current file and project.
- Command palette.
- Web preview for HTML/Markdown bot docs.
- Plugin registry interface for safe editor extensions.
- Terminal/action bridge through BotBlade policy gates.

Implementation notes:

- Acode is MIT, but BotBlade should implement native Kotlin/Compose editor architecture rather than copying Cordova app internals.
- Any direct reuse must include MIT attribution and file-level provenance.

## Exact original code pack for Codex

Paste the following to Codex when starting the implementation. This code is original BotBlade scaffolding and does not copy JGit, Fossify, or Acode source.

### File: app/src/main/java/com/princess/botblade/upstreams/FeatureBundle.kt

```kotlin
package com.princess.botblade.upstreams

/**
 * A legally reviewed upstream-inspired capability bundle.
 *
 * DIRECT_DEPENDENCY means BotBlade may use the upstream as a declared dependency
 * after license and build compatibility are verified.
 *
 * ORIGINAL_IMPLEMENTATION means BotBlade implements comparable behavior with new
 * code owned by this repository.
 *
 * REFERENCE_ONLY means the upstream is useful for product research but source,
 * assets, strings, layouts, and resources must stay out of BotBlade.
 */
data class FeatureBundle(
    val id: String,
    val displayName: String,
    val upstreamUrls: List<String>,
    val licenseMode: LicenseMode,
    val capabilities: List<String>,
    val implementationNotes: List<String>,
)

enum class LicenseMode {
    DIRECT_DEPENDENCY,
    ORIGINAL_IMPLEMENTATION,
    REFERENCE_ONLY,
}
```

### File: app/src/main/java/com/princess/botblade/upstreams/FeatureBundleRegistry.kt

```kotlin
package com.princess.botblade.upstreams

object FeatureBundleRegistry {
    val jgit = FeatureBundle(
        id = "jgit-repo-forge",
        displayName = "JGit Repository Forge",
        upstreamUrls = listOf("https://github.com/eclipse-jgit/jgit"),
        licenseMode = LicenseMode.DIRECT_DEPENDENCY,
        capabilities = listOf(
            "Clone/import Git repositories",
            "Open local repositories",
            "Inspect status and changed files",
            "Create and switch branches",
            "Stage files and create commits",
            "Fetch, pull, and push through BotBlade secrets",
            "Emit structured audit events for mutating operations",
        ),
        implementationNotes = listOf(
            "Verify dependency license and Android compatibility before adding Gradle coordinates.",
            "Never log credentials or remote URLs containing embedded credentials.",
            "Keep repository content untrusted until static safety scanning passes.",
        ),
    )

    val fossifyStyleFiles = FeatureBundle(
        id = "fossify-style-file-ergonomics",
        displayName = "Fossify-Style File Ergonomics",
        upstreamUrls = listOf(
            "https://github.com/FossifyOrg",
            "https://github.com/FossifyOrg/File-Manager",
            "https://github.com/FossifyOrg/Gallery",
        ),
        licenseMode = LicenseMode.REFERENCE_ONLY,
        capabilities = listOf(
            "Project file browser",
            "Storage Access Framework import/export",
            "Recent projects and pinned locations",
            "Local asset previews",
            "Privacy-first permission explanations",
            "Per-project accent metadata",
        ),
        implementationNotes = listOf(
            "Implement with original Kotlin/Compose code only.",
            "Do not copy GPL source, resources, layouts, strings, icons, or assets.",
            "Use Fossify only as a product-quality reference for simple local-first UX.",
        ),
    )

    val acodeWorkbench = FeatureBundle(
        id = "acode-inspired-editor-workbench",
        displayName = "Acode-Inspired Editor Workbench",
        upstreamUrls = listOf(
            "https://github.com/Acode-Foundation/Acode",
            "https://docs.acode.app",
        ),
        licenseMode = LicenseMode.ORIGINAL_IMPLEMENTATION,
        capabilities = listOf(
            "Tabbed mobile editor model",
            "Project tree and outline drawer",
            "Language mode detection",
            "Find and replace",
            "Command palette",
            "Preview surfaces for HTML and Markdown",
            "Safe editor extension registry",
        ),
        implementationNotes = listOf(
            "Acode is MIT, but BotBlade should use native Android architecture.",
            "Direct MIT reuse requires attribution and provenance comments.",
            "Editor extensions must be routed through BotBlade policy gates.",
        ),
    )

    val all: List<FeatureBundle> = listOf(
        jgit,
        fossifyStyleFiles,
        acodeWorkbench,
    )
}
```

### File: app/src/main/java/com/princess/botblade/git/GitForgeModels.kt

```kotlin
package com.princess.botblade.git

import java.time.Instant

data class GitRepositoryRef(
    val projectId: String,
    val workTreePath: String,
    val gitDirectoryPath: String,
)

data class GitAuthor(
    val name: String,
    val email: String,
)

data class GitCommitSummary(
    val id: String,
    val shortId: String,
    val authorName: String,
    val authorEmail: String,
    val message: String,
    val committedAt: Instant,
)

data class GitBranchSummary(
    val name: String,
    val isCurrent: Boolean,
    val isRemote: Boolean,
)

data class GitFileStatus(
    val path: String,
    val state: GitFileState,
)

enum class GitFileState {
    MODIFIED,
    ADDED,
    DELETED,
    RENAMED,
    COPIED,
    UNTRACKED,
    MISSING,
    CONFLICTING,
    STAGED,
}

data class GitStatusSnapshot(
    val repository: GitRepositoryRef,
    val files: List<GitFileStatus>,
    val capturedAt: Instant,
)

data class GitOperationAudit(
    val operation: String,
    val projectId: String,
    val status: String,
    val redactedDetails: Map<String, String>,
    val occurredAt: Instant,
)
```

### File: app/src/main/java/com/princess/botblade/git/GitForgeService.kt

```kotlin
package com.princess.botblade.git

interface GitForgeService {
    suspend fun openRepository(projectId: String, workTreePath: String): GitRepositoryRef

    suspend fun cloneRepository(
        projectId: String,
        remoteUrl: String,
        destinationPath: String,
        credentialRef: String?,
    ): GitRepositoryRef

    suspend fun status(repository: GitRepositoryRef): GitStatusSnapshot

    suspend fun branches(repository: GitRepositoryRef): List<GitBranchSummary>

    suspend fun checkoutBranch(repository: GitRepositoryRef, branchName: String): GitOperationAudit

    suspend fun createBranch(repository: GitRepositoryRef, branchName: String, checkout: Boolean): GitOperationAudit

    suspend fun stage(repository: GitRepositoryRef, paths: List<String>): GitOperationAudit

    suspend fun commit(
        repository: GitRepositoryRef,
        message: String,
        author: GitAuthor,
    ): GitCommitSummary

    suspend fun fetch(repository: GitRepositoryRef, remoteName: String, credentialRef: String?): GitOperationAudit

    suspend fun pull(repository: GitRepositoryRef, remoteName: String, credentialRef: String?): GitOperationAudit

    suspend fun push(repository: GitRepositoryRef, remoteName: String, credentialRef: String?): GitOperationAudit
}
```

### File: app/src/main/java/com/princess/botblade/editor/EditorWorkbenchModels.kt

```kotlin
package com.princess.botblade.editor

import java.time.Instant

data class EditorDocumentId(val value: String)

data class EditorDocument(
    val id: EditorDocumentId,
    val projectId: String,
    val path: String,
    val displayName: String,
    val languageMode: LanguageMode,
    val text: String,
    val isDirty: Boolean,
    val updatedAt: Instant,
)

data class LanguageMode(
    val id: String,
    val displayName: String,
    val extensions: Set<String>,
)

data class EditorTab(
    val documentId: EditorDocumentId,
    val title: String,
    val isSelected: Boolean,
    val isDirty: Boolean,
)

data class EditorCommand(
    val id: String,
    val title: String,
    val category: String,
    val requiresPolicyGate: Boolean,
)

object BuiltInLanguageModes {
    val kotlin = LanguageMode("kotlin", "Kotlin", setOf("kt", "kts"))
    val javascript = LanguageMode("javascript", "JavaScript", setOf("js", "mjs", "cjs"))
    val typescript = LanguageMode("typescript", "TypeScript", setOf("ts", "tsx"))
    val json = LanguageMode("json", "JSON", setOf("json"))
    val markdown = LanguageMode("markdown", "Markdown", setOf("md", "markdown"))
    val shell = LanguageMode("shell", "Shell", setOf("sh", "bash", "zsh"))
    val plainText = LanguageMode("plain-text", "Plain Text", emptySet())

    val all = listOf(kotlin, javascript, typescript, json, markdown, shell, plainText)

    fun detect(path: String): LanguageMode {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase()
        return all.firstOrNull { extension in it.extensions } ?: plainText
    }
}
```

### File: app/src/main/java/com/princess/botblade/files/ProjectFileModels.kt

```kotlin
package com.princess.botblade.files

import java.time.Instant

data class ProjectFileEntry(
    val projectId: String,
    val path: String,
    val displayName: String,
    val kind: ProjectFileKind,
    val sizeBytes: Long?,
    val modifiedAt: Instant?,
)

enum class ProjectFileKind {
    FILE,
    DIRECTORY,
    SYMLINK,
    UNKNOWN,
}

data class ProjectLocationPin(
    val projectId: String,
    val label: String,
    val path: String,
    val createdAt: Instant,
)

data class FileOperationRequest(
    val projectId: String,
    val operation: FileOperation,
    val sourcePath: String,
    val destinationPath: String? = null,
)

enum class FileOperation {
    CREATE_FILE,
    CREATE_DIRECTORY,
    RENAME,
    COPY,
    MOVE,
    DELETE,
}
```

## Codex task prompt

Use this exact prompt when asking Codex to implement the bundles:

```text
You are working inside the BotBlade repository.

Goal: implement the upstream feature-bundle scaffolding from docs/design/upstream-feature-bundles.md without copying upstream source code.

Legal constraints:
- JGit may be integrated as a dependency only after confirming Gradle/Android compatibility and preserving license attribution.
- Fossify is GPL-3.0 in the relevant app repositories. Do not copy, translate, rewrite, vendor, or embed Fossify source, resources, strings, layouts, icons, or assets. Implement comparable behavior with original Kotlin/Compose code only.
- Acode is MIT. Prefer original native Kotlin/Compose implementation. Any direct reuse requires MIT attribution and file-level provenance.

Implementation requirements:
1. Add the exact Kotlin model/scaffolding files from docs/design/upstream-feature-bundles.md.
2. Add tests for BuiltInLanguageModes.detect.
3. Add a docs/upstreams.md page that links JGit, Fossify, Fossify File Manager, Fossify Gallery, Acode, and Acode plugin docs.
4. Update docs/design/botblade-security-manual/upstreams.yml with jgit, fossify-org, fossify-file-manager, fossify-gallery, acode, and acode-plugin-docs entries.
5. Do not add GPL code. Do not add generated code from upstream repositories. Do not add third-party assets.
6. Run the smallest available checks and report exactly what passed or failed.
```
