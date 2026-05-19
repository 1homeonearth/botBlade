# royalScepter

royalScepter is an Android-based Discord bot builder and deployment console. The repository contains:

- A native Android client for managing bot projects from a phone, tablet, emulator, or local debug device.
- A local backend builder API that stores project metadata in memory, generates TypeScript Discord bot projects, edits generated files, validates projects, runs builds, manages local runtime controls, stores secret metadata, and records audit events for sensitive actions.
- A generated bot template for Node 22 + TypeScript + `discord.js`.

## Current status

Implemented today:

- Android shell with Dashboard, Projects, Editor, Deployments, Settings, and reachable project/secret/deployment flows wired through a centralized API client.
- Backend on port `8000` by default.
- Legacy local compatibility endpoints: `GET /api/bot-status/` and `POST /api/bot-toggle/`.
- Project CRUD, archive, clone, validation, command management, and generated file list/read/save.
- Secret metadata storage with in-memory local-dev secret values, fingerprints, and redacted responses.
- Build jobs for generated TypeScript bots.
- Local-process runtime start/stop/restart controls when a generated project and Discord token secret reference are configured.
- Deployment target skeleton with `local_process` behavior and explicit `local_docker` future limitations.
- Audit event recording and listing for important project, secret, generation, build, deployment, runtime, GitHub, and command-registration actions.

Future / not complete yet:

- Persistent database storage.
- Real GitHub push execution.
- Real Docker deployment execution.
- Remote/cloud deployment adapters.
- Production authentication/authorization.
- Encrypted production secret vault integration.

## Android app setup

The Android app is in `app/` and uses Kotlin, AndroidX, AppCompat, Material Components, and native Android views. The package namespace is `com.princess.royalscepter`.

Build a debug APK from the repository root:

```bash
./gradlew :app:assembleDebug
```

If your environment only provides a system Gradle install:

```bash
gradle :app:assembleDebug
```

The API base URL is resolved from Android configuration instead of being hard-coded:

- `localDev` variants default to `http://10.0.2.2:8000`, which reaches a backend running on the development host from an emulator.
- `prod` variants default to the production HTTPS `BuildConfig.API_BASE_URL` value from `app/build.gradle.kts`; override packaging endpoints with `-PPROD_API_BASE_URL=https://...`.
- The Settings tab includes a Backend API field that validates `http://` or `https://` URLs, rejects embedded credentials/query strings/fragments, saves only the URL in app `SharedPreferences`, and provides a **Test Connection** button that calls `/api/health`.

Emulator example:

```text
http://10.0.2.2:8000
```

Physical-device example on the same LAN as your development machine:

```text
http://192.168.1.25:8000
```

For a real device, start the backend on an address reachable from your LAN and enter `http://<host-lan-ip>:8000` in Settings. Keep this on a trusted development network; do not put credentials, tokens, query parameters, or fragments in the backend URL.

Release packaging, signing, versioning, icon, privacy, and network-security checklists are documented in [docs/android-release.md](docs/android-release.md), [docs/privacy-policy.md](docs/privacy-policy.md), and [docs/android-screens.md](docs/android-screens.md).

## Backend setup

The backend lives in `backend/` and requires Node 22 or newer.

```bash
cd backend
npm install
npm run build
npm start
```

The backend listens on `PORT` or `8000` by default:

```bash
PORT=8000 npm start
```

Health check:

```bash
curl http://localhost:8000/api/health
```

## Local development mode

Local development intentionally uses in-memory stores. Restarting the backend clears projects, secret metadata, secret values, build jobs, deployment targets, runtime records, and audit events.

Local compatibility endpoints remain available for early Android dashboard integration:

- `GET /api/bot-status/`
- `POST /api/bot-toggle/` with `{"action":"start"}` or `{"action":"stop"}`

Project-scoped endpoints under `/api/projects/:projectId/...` are preferred for new code.

## How to create a bot project

Create a project:

```bash
curl -X POST http://localhost:8000/api/projects \
  -H 'content-type: application/json' \
  -d '{"name":"My Bot","description":"Local development bot"}'
```

Add commands with `POST /api/projects/:projectId/commands`, update Discord IDs with `PATCH /api/projects/:projectId`, and create a Discord token secret with `POST /api/secrets`. Use placeholder examples only; never commit real tokens.

## How to generate a bot

Generate TypeScript bot files:

```bash
curl -X POST http://localhost:8000/api/projects/<projectId>/generate
```

Generated files are written under `backend/generated-projects/<sanitized-project-id>/`. The generated project contains `package.json`, `tsconfig.json`, `.env.example`, `README.md`, `src/index.ts`, and command modules.

## How to build

Run a backend build job:

```bash
curl -X POST http://localhost:8000/api/projects/<projectId>/builds \
  -H 'content-type: application/json' \
  -d '{"source":"current_project","clean":true,"runTests":true,"createDockerImage":false}'
```

The build service validates the project, ensures files are generated, runs `npm install`/`npm ci`, runs `npm run build`, and runs generated tests if a test script exists.

## How to run locally

1. Create a secret with type `discord_bot_token`.
2. Patch the project so `discord.tokenSecretRef` points to that secret ID.
3. Generate and build the project.
4. Start runtime:

```bash
curl -X POST http://localhost:8000/api/projects/<projectId>/runtime/start
```

Stop or restart with `/runtime/stop` and `/runtime/restart`.

## Security notes

- Secret API responses return metadata and fingerprints only, never secret values.
- Secret values are stored in memory for local development only.
- Redaction is applied to logs, API JSON serialization, and audit metadata.
- Generated `.env.example` contains placeholders only.
- File routes normalize paths and block traversal outside the generated project workspace.
- Build commands run only inside the controlled `generated-projects` workspace.
- Production use requires real authentication, persistent storage, encrypted secret storage, and hardened build isolation.

## Durable persistence

Project metadata, secret summaries, audit events, build/deployment jobs, and runtime metadata can be persisted with the SQLite adapter. See [docs/persistence.md](docs/persistence.md) for configuration, migrations, and backup/restore procedures.
