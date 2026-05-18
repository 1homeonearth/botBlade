# royalScepter API

Base URL in local development: `http://localhost:8000`.

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
{ "ok": true, "service": "royalScepter-backend", "version": "0.1.0" }
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

Returns `{ "auditEvents": AuditEvent[] }`.

### `GET /api/projects/:projectId/audit-events`

Returns events for one project.

Audit event shape:

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

## Secrets

### `GET /api/secrets`

Returns `{ "secrets": SecretSummary[] }`.

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

Returns `{ "targets": DeploymentTarget[] }`.

### `POST /api/deployment-targets`

Creates a target. Types: `local_process` or `local_docker`. Secret-like config keys are rejected; use `secretRefs`.

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

### `GET /api/projects/:projectId/deployments/:deploymentId/logs`

Returns redacted logs.

### `POST /api/projects/:projectId/deployments/:deploymentId/rollback`

Currently returns `ROLLBACK_UNSUPPORTED`.

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
