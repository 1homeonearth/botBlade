# botBlade API

Base URL in local development: `http://localhost:8000`.

## Authentication and authorization

Every endpoint except `GET /api/health` and `GET /api/version` requires an authenticated actor. Send either a bearer token:

```http
Authorization: Bearer <token>
```

or a session credential via `Cookie: botBladeSession=<session-token>` or `x-session-token: <session-token>`.

Configure backend credentials with one of these environment variables before starting the API:

- `BOTBLADE_AUTH_TOKENS`: JSON credential or array of credentials for bearer tokens.
- `BOTBLADE_SESSION_TOKENS`: JSON credential or array of credentials for session credentials.
- `BOTBLADE_API_TOKEN` / `BOTBLADE_API_TOKENS`: comma-separated legacy bearer token fallback; these tokens receive admin access.

Credential objects support `token`, `actorId` (or `userId`), `tokenId`, `roles`, and `projectIds`. Use `roles: ["admin"]` or `projectIds: ["*"]` for all-project administrative access; otherwise list specific project IDs in `projectIds`. Project-scoped tokens can access only authorized `/api/projects/:projectId/*` resources and project-owned secrets. Global resources such as `/api/audit-events`, `/api/github/*`, and `/api/deployment-targets` require admin/all-project access.

Example:

```bash
export BOTBLADE_AUTH_TOKENS='[{"token":"dev-admin-token","actorId":"local_admin","roles":["admin"],"projectIds":["*"]}]'
```

Errors use:

```json
{
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Request validation failed.",
    "details": {},
    "requestId": "req_..."
  }
}
```

## Health and version

### `GET /api/health`

Returns:

```json
{ "ok": true, "service": "botBlade-backend", "version": "0.1.0" }
```

### `GET /api/version`

Returns service name, version, and API version.

## Legacy compatibility

### `GET /api/bot-status/`

Returns runtime status for the first project or a stopped default response.

### `POST /api/bot-toggle/`

Body: `{ "action": "start" }` or `{ "action": "stop" }`. Uses the first project. Project-scoped runtime routes are preferred.

## Audit events

### `GET /api/audit-events`

Requires admin/all-project access. Returns `{ "auditEvents": AuditEvent[] }`.

### `GET /api/projects/:projectId/audit-events`

Requires access to `projectId`. Returns events for one project.

Audit event shape includes `actorId`, which is populated from the authenticated bearer/session credential:

```json
{
  "id": "audit_...",
  "actorId": "local_user",
  "projectId": "project_...",
  "action": "build.start",
  "resourceType": "build",
  "resourceId": "build_...",
  "metadata": {},
  "createdAt": "2026-05-18T00:00:00.000Z",
  "requestId": "req_..."
}
```

## Projects

### `GET /api/projects`

Returns `{ "projects": BotProject[] }`.

### `POST /api/projects`

Body includes `name` and optional `slug`, `description`, `templateId`, `language`, `runtime`, `discord`, `permissions`, `commands`, `events`, `deployment`, and `github`. Returns the project plus `auditEventId`.

### `GET /api/projects/:projectId`

Returns one project.

### `PATCH /api/projects/:projectId`

Patchable fields: `name`, `slug`, `description`, `templateId`, `discord`, `permissions`, `commands`, `events`, `deployment`, `github`. Returns the updated project plus `auditEventId`.

### `DELETE /api/projects/:projectId`

Deletes the in-memory project and returns `204`.

### `POST /api/projects/:projectId/archive`

Returns the archived project plus `auditEventId`.

### `POST /api/projects/:projectId/clone`

Returns a cloned project plus `auditEventId`.

### `POST /api/projects/:projectId/validate`

Returns `{ "valid": boolean, "errors": ValidationIssue[], "warnings": ValidationIssue[] }`.

### `POST /api/projects/:projectId/generate`

Generates missing files and returns `{ "projectId", "generatedAt", "files", "auditEventId" }`.

### `POST /api/projects/:projectId/regenerate`

Regenerates template files with force overwrite and returns the same shape as generate.

## Commands

### `GET /api/projects/:projectId/commands`

Returns `{ "commands": BotCommand[] }`.

### `POST /api/projects/:projectId/commands`

Creates a command, validates all command names, records `discord.commands.register`, and returns the command plus `auditEventId`.

### `PATCH /api/projects/:projectId/commands/:commandId`

Updates a command and returns it.

### `DELETE /api/projects/:projectId/commands/:commandId`

Deletes a command and returns `204`.

## Files

### `GET /api/projects/:projectId/files`

Returns `{ "files": ProjectFileSummary[] }`.

### `GET /api/projects/:projectId/files/:path`

Returns file metadata plus `content`. Path traversal outside the workspace is blocked.

### `PUT /api/projects/:projectId/files/:path`

Body: `{ "content": "..." }`. Writes and returns file metadata plus content.

## Imports

Import validation is static-only: BotBlade materializes or copies input into a managed workspace and scans files without executing imported code.

### `POST /api/imports/workflow-json`

Body: `{ "workspacePath": "/tmp/imports", "workflowPath": "/path/to/workflow.json" }` or `{ "workspacePath": "/tmp/imports", "workflowJson": { "nodes": [], "connections": {} } }`. The workflow is validated as JSON, capped at 512 KiB, written into the managed workspace as `workflow.json`, and scanned. Invalid or oversized workflow JSON returns an import record in `blocked_by_policy` with security-card details.

### `POST /api/imports/template`

Body: `{ "workspacePath": "/tmp/imports", "templateId": "n8n-empty-workflow" }`. Only first-party template IDs in the backend allowlist are materialized and scanned; unknown template IDs are blocked before scanning.

## Secrets

### `GET /api/secrets`

Returns `{ "secrets": SecretSummary[] }` filtered to secrets whose `projectId` the actor can access. Admin/all-project actors can also see global (`projectId: null`) secrets.

### `POST /api/secrets`

Body: `{ "projectId": null, "name": "Discord token", "type": "discord_bot_token", "value": "<placeholder>" }`. Returns secret metadata plus `auditEventId`; never returns `value`.

### `GET /api/secrets/:secretId`

Returns secret metadata only.

### `PATCH /api/secrets/:secretId`

Updates metadata (`projectId`, `name`, `type`) and returns metadata only.

### `POST /api/secrets/:secretId/rotate`

Body: `{ "value": "<new-placeholder>" }`. Returns metadata plus `auditEventId`; never returns `value`.

### `DELETE /api/secrets/:secretId`

Deletes the secret and returns `204`.


## Script profiles

Script profile endpoints are CRUD endpoints for per-project command metadata discovered during scans or created by users. They do not execute commands; they only persist and return metadata for preview, setup, and later explicit execution flows. All routes require access to the target project.

Script profile shape: `id`, `projectId`, `name`, optional `description`, `source` (`package_json`, `file`, `blade_pack`, `repair_card`, `user`, or `codex`), `runtime` (`node`, `python`, `shell`, `powershell`, `docker`, `workflow`, or `custom`), `command[]`, `workingDirectory`, `envRefs[]`, `secretRefs[]`, `timeoutSeconds`, `requiresConfirmation`, `tags[]`, `createdAt`, and `updatedAt`.

Security notes: `command[]` values are metadata-only previews and are never run by these endpoints. Do not send secret values in `command[]`, `envRefs[]`, `tags[]`, or `workingDirectory`; use `secretRefs[]` for references only. The API rejects likely secret values in `command[]` and `secretRefs[]`, normalizes project-relative working directories, records create/update/delete/detect audit events, and never returns raw secret values.

### `GET /api/projects/:projectId/script-profiles`

Returns `{ "scriptProfiles": ScriptProfile[] }` sorted by most recently updated profile.

### `POST /api/projects/:projectId/script-profiles`

Body: `{ "name": "Start bot", "runtime": "node", "command": ["npm", "run", "start"], "workingDirectory": ".", "secretRefs": ["secret_discord_token"], "requiresConfirmation": true }`. Creates a user-authored metadata profile and returns it with status `201`.

### `GET /api/projects/:projectId/script-profiles/:scriptProfileId`

Returns one script profile or `404` when the profile does not belong to the project.

### `PATCH /api/projects/:projectId/script-profiles/:scriptProfileId`

Patchable fields: `name`, `description`, `source`, `runtime`, `command`, `workingDirectory`, `envRefs`, `secretRefs`, `timeoutSeconds`, `requiresConfirmation`, and `tags`. Returns the updated metadata profile.

### `DELETE /api/projects/:projectId/script-profiles/:scriptProfileId`

Deletes the script profile metadata and returns `204`.

## Builds

### `POST /api/projects/:projectId/builds`

Body: `{ "source": "current_project", "clean": true, "runTests": true, "createDockerImage": false }`. Returns `BuildJob` with `auditEventId`.

### `GET /api/projects/:projectId/builds`

Returns `{ "builds": BuildJob[] }`.

### `GET /api/projects/:projectId/builds/:buildId`

Returns one build job.

### `GET /api/projects/:projectId/builds/:buildId/logs`

Returns `{ "logs": "..." }` with redaction applied.

## Runtime

### `GET /api/projects/:projectId/runtime/status`

Returns runtime status.

### `POST /api/projects/:projectId/runtime/start|stop|restart`

Starts, stops, or restarts the local generated bot runtime. Mutation responses include `auditEventId` where the operation succeeds.

### `GET /api/projects/:projectId/runtime/logs`

Returns `{ "logs": "..." }` with redaction applied.

## Deployment targets

### `GET /api/deployment-targets`

Requires admin/all-project access. Returns `{ "targets": DeploymentTarget[] }`. Each target includes derived adapter `capabilities` so clients can show supported and unsupported deployment actions.

### `POST /api/deployment-targets`

Creates a target. Types: `local_process` or `local_docker`. Secret-like config keys are rejected; use `secretRefs`. `local_docker` accepts non-secret `config.image` and `config.dockerfile`.

### `GET|PATCH|DELETE /api/deployment-targets/:targetId`

Read, update, or delete a target.

### `POST /api/deployment-targets/:targetId/test`

Tests local adapter availability.

## Deployments

### `POST /api/projects/:projectId/deployments`

Body: `{ "targetId": "target_...", "buildId": "build_..." }`. Requires a succeeded build. Returns `DeploymentJob` with `auditEventId`.

### `GET /api/projects/:projectId/deployments`

Returns `{ "deployments": DeploymentJob[] }`.

### `GET /api/projects/:projectId/deployments/:deploymentId`

Returns one deployment job.

### `GET /api/projects/:projectId/deployments/:deploymentId/status`

Returns adapter runtime status for the selected deployment.

### `GET /api/projects/:projectId/deployments/:deploymentId/logs`

Returns adapter runtime logs with redaction applied.

### `POST /api/projects/:projectId/deployments/:deploymentId/start|stop|restart`

Runs the corresponding adapter action when the selected target supports it. Unsupported actions return `DEPLOYMENT_ACTION_UNSUPPORTED`.

### `POST /api/projects/:projectId/deployments/:deploymentId/rollback`

Rolls back when the adapter supports rollback. `local_docker` replaces the current container with the most recent previous image for the same project and target; `local_process` reports the action as unsupported.

## GitHub

### `GET /api/github/status`

Returns whether a GitHub token secret reference is configured.

### `POST /api/github/connect`

Body: `{ "tokenSecretRef": "secret_..." }`.

### `POST /api/projects/:projectId/github/create-repo`

Stores owner/repo/default branch metadata locally. It does not create the remote repository yet.

### `POST /api/projects/:projectId/github/push`

Future. Currently returns `GITHUB_PUSH_NOT_IMPLEMENTED`.

### `POST /api/projects/:projectId/github/create-workflow`

Returns a GitHub Actions workflow file path and content.

### `POST /api/projects/:projectId/scan`

Returns `{ "detection", "botbladeJsonPath" }` where `botblade.json` contains the shipped Bot Profile schema:

- `schemaVersion`: `"1.1.0"`
- `generatedBy`, `generatedAt`
- `project`: `name`, `type`, `root`, `importSource`
- `runtime`: `type`, `version`, `packageManager`, `detectedLanguages[]`, `detectedFrameworks[]`
- `bladePack`: `selected`, `version`, `detected[]` (`id`, `name`, `score`, `confidence`, `matchedEvidence[]`)
- `commandPlan`: `install[]`, `build[]`, `test[]`, `validate[]`, `start[]`, `stop[]`, `restart[]`, `deploy[]`
- `scriptProfiles[]`: metadata-only command previews with `id`, `source`, `runtime`, `command[]`, `workingDirectory`, `envRefs[]`, `secretRefs[]`, confirmation, tags, and timestamps
- `secrets.required[]` / `secrets.optional[]`: `name`, `required`, `configured`
- `permissions[]`, `capabilities[]`, `importantFiles[]`, `warnings[]`, `repairCards[]`, `git` (`branch`, `status`, `remotes[]`)

Secret safety guarantee: profile metadata stores only secret requirement metadata, configured flags, and script-profile secret references. Secret values are never serialized. Phase 4 scan/import behavior detects, previews, and persists script profile metadata only; it does not run commands.
