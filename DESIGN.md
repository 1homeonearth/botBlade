# royalScepter Design

## Product vision

royalScepter aims to make Discord bot creation and operation manageable from Android. A user should be able to create a bot project, define commands, store secret references, generate source code, build it, run it locally during development, and eventually deploy it to production adapters.

## Architecture

The repository is split into three main layers:

1. **Android client**: native Kotlin UI and repositories that call the backend API.
2. **Backend builder API**: Node 22 TypeScript HTTP API with in-memory project, secret, file, build, runtime, deployment, GitHub, and audit services.
3. **Generated bot template**: TypeScript + `discord.js` project emitted into `backend/generated-projects/`.

All current data stores are in-memory except generated files, which are written to local disk for editing/building.

## Android client

The Android app uses bottom navigation with Dashboard, Projects, Editor, Deployments, and Settings screens. It centralizes API configuration in `ApiConfig` and uses repository classes for dashboard, project, editor, build, deployment, and secret operations. The app is currently a local-development console, not a production multi-user client.

## Backend builder

The backend listens on port `8000` unless `PORT` is set. It exposes project-scoped endpoints for:

- Project CRUD, archive, clone, validation.
- Command creation/update/delete.
- Generated file list/read/write.
- Bot generation/regeneration.
- Secret metadata create/update/rotate/delete.
- Build jobs and logs.
- Runtime status/start/stop/restart/logs.
- Deployment targets and deployment jobs.
- Audit event listing.

Error responses use `{ "error": { "code", "message", "details", "requestId" } }`.

## Bot template

Generated bots target Node 22 and TypeScript. The template includes:

- `package.json` with `build`, `start`, and `test` scripts.
- `tsconfig.json` using `NodeNext` module resolution.
- `.env.example` with no real secret values.
- `src/index.ts` that validates `DISCORD_TOKEN`, creates a Discord client, imports generated commands, and logs readiness.
- `src/commands/*.ts` modules for configured commands or a default `ping` command.

## Deployment adapters

The deployment adapter interface supports validate/prepare/deploy/status/start/stop/restart/logs/rollback operations.

Implemented:

- `local_process`: marks a successful build as deployable by the local runtime and delegates runtime status/start/stop/restart/logs to the runtime service.

Future / explicit limitation:

- `local_docker`: target parsing and availability test exist, but deployment currently returns `DOCKER_NOT_CONFIGURED` rather than pretending success.
- Remote hosts, cloud providers, and container registries are future work.

## Data model

Core backend resources:

- `BotProject`: project metadata, Discord config, permissions, commands, events, deployment pointers, GitHub pointers, archive timestamps.
- `SecretSummary`: secret metadata, storage mode, fingerprint, timestamps. Secret values remain private to the in-memory store.
- `BuildJob`: build status, options, timestamps, logs URL, audit event ID.
- `DeploymentTarget`: local target metadata, non-secret config, secret reference IDs.
- `DeploymentJob`: target/build linkage, status, logs URL, timestamps, audit event ID.
- `AuditEvent`: actor, project, action, resource, redacted metadata, request ID, timestamp.

## API overview

Major groups:

- Health/version: `/api/health`, `/api/version`.
- Legacy dashboard: `/api/bot-status/`, `/api/bot-toggle/`.
- Projects: `/api/projects`, `/api/projects/:projectId` and project subresources.
- Secrets: `/api/secrets`, `/api/secrets/:secretId`, `/api/secrets/:secretId/rotate`.
- Builds: `/api/projects/:projectId/builds`.
- Runtime: `/api/projects/:projectId/runtime/*`.
- Deployment targets/jobs: `/api/deployment-targets`, `/api/projects/:projectId/deployments`.
- Audit: `/api/audit-events`, `/api/projects/:projectId/audit-events`.

See `docs/api.md` for response shapes.

## Implementation phases

1. **Local scaffold**: Android navigation, backend health/legacy endpoints, basic project models. Complete.
2. **Project builder**: project CRUD, validation, generation, editor, build jobs. Complete for local development.
3. **Secrets and safety**: secret metadata, redaction, no secret responses, path/build safeguards, audit events. Complete for local development.
4. **Runtime/deployment skeleton**: local runtime and local-process deployment skeleton. Complete with Docker/cloud marked future.
5. **Productionization**: persistence, auth, encrypted vault, isolated build workers, real deployment adapters. Future.
