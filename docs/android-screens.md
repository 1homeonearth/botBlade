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

When verifying the Projects and Settings GitHub UI manually, load or stub an active project whose GitHub object has only one repository identifier populated, such as `owner = "princess"` with `repo = ""` or `owner = ""` with `repo = "royal-scepter"`. Both the Projects card GitHub row and the Settings GitHub status line should show the `github_not_linked` text (`not linked`) instead of a partial `owner/repo` value, and the GitHub push/workflow actions should remain disabled until both owner and repo are non-blank.

## Secrets UI

Secrets UI is reachable through the project/deployment/settings flows where implemented. It should create, rotate, and delete secret metadata without displaying stored secret values. Secret entry fields should be cleared after successful save/rotate.
