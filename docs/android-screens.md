# Android Screens

## Dashboard

Shows backend/runtime status and active project context. It uses legacy compatibility endpoints for dashboard status/toggle behavior while project-scoped runtime APIs are preferred for new flows. It should present loading, success, empty/disconnected, and error states.

## Projects

Lists bot projects and supports project creation/selection through the project repository/API layer. The selected project ID is stored by `ActiveProjectStore` so other screens can operate on the active project.

## Editor

Project-aware file editor screen for generated files. It lists files from `/api/projects/:projectId/files`, reads selected file content, and saves edits with `PUT /api/projects/:projectId/files/:path`. Backend path traversal protection prevents edits outside the generated workspace.

## Deployments

Shows build/deployment controls and target/deployment status. Deploy actions should be disabled unless an active project exists and a successful build is available. Current backend deployment support is local-process skeleton behavior; Docker/cloud deployments are future.

## Settings

Central place for local backend configuration and future account/environment settings. The API base URL should remain centralized rather than duplicated across screens.

### GitHub display manual verification

When verifying the Projects and Settings GitHub UI manually, load or stub an active project whose GitHub object has only one repository identifier populated, such as `owner = "princess"` with `repo = ""` or `owner = ""` with `repo = "bot-blade"`. Both the Projects card GitHub row and the Settings GitHub status line should show the `github_not_linked` text (`not linked`) instead of a partial `owner/repo` value, and the GitHub push/workflow actions should remain disabled until both owner and repo are non-blank.

## Secrets UI

Secrets UI is reachable through the project/deployment/settings flows where implemented. It should create, rotate, and delete secret metadata without displaying stored secret values. Secret entry fields should be cleared after successful save/rotate.


## Release screenshots

Capture screenshots from a `prodRelease`-equivalent build or a `prodDebug` build pointed at a sanitized staging backend. Use representative but fake projects, commands, deployment targets, GitHub repositories, and secret references.

Recommended screenshot set for store review:

1. Dashboard showing sanitized bot/runtime status and active project context.
2. Projects screen with fake project names and validation status.
3. Editor screen showing non-sensitive generated template code only.
4. Deployments screen showing placeholder build/runtime logs with no tokens or private hostnames.
5. Settings screen showing an HTTPS production/staging backend URL with no credentials, query string, or fragment.

Before publishing screenshots, verify that no Discord token, GitHub token, private repository, private backend hostname, user email, or real audit record is visible. Keep raw screenshots out of source control unless they are sanitized marketing assets.
