# BotBlade Architecture Addendum: Forge Sync + Editor + Import Catalog

## New domains

### Forge Sync domain
- Backend `ForgeSyncService` handles URL import and repository status.
- Security constraints:
  - HTTPS/SSH URLs only.
  - no command shell interpolation.
  - workspace non-empty guard.
  - nested `.git` warnings + large-repo warnings.
- Audit actions:
  - `forge.import`
  - `forge.status`

### Project classifier domain
- `projectClassifier` inspects `package.json` and lockfiles for:
  - package manager
  - framework hints
  - script inventory
  - entrypoint hints
- Audit action: `project.classify`.

### Project health ribbon domain
- API aggregates:
  - Forge Sync status
  - dependency/classifier summary
  - latest build status
  - secret reference readiness

### Editor and files domain
- Existing file APIs remain project-scoped and path-confined.
- Editor remains local-first against workspace file APIs and can layer diagnostics/repair commands.

## API additions
- `POST /api/projects/:projectId/forge-sync/import`
- `GET /api/projects/:projectId/forge-sync/status`
- `GET /api/projects/:projectId/forge-sync/classify`
- `GET /api/projects/:projectId/forge-sync/health`

## Data posture
- Store operation history as audit events.
- Keep secret values out of request/response payloads.
- Return only metadata in health and classifier summaries.

