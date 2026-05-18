# royalScepter Security

## API authentication and authorization

All backend API routes except `GET /api/health` and `GET /api/version` require a bearer token or session credential. Configure bearer credentials with `ROYALSCEPTER_AUTH_TOKENS` (preferred JSON format) or the legacy `ROYALSCEPTER_API_TOKEN` / `ROYALSCEPTER_API_TOKENS` variables. Configure session credentials with `ROYALSCEPTER_SESSION_TOKENS`, then send them as `Cookie: royalScepterSession=<session-token>` or `x-session-token: <session-token>`.

Credential objects should include `token`, `actorId` or `userId`, optional `tokenId`, `roles`, and `projectIds`. Use `roles: ["admin"]` or `projectIds: ["*"]` only for trusted local administrators. Project-scoped credentials must list allowed project IDs and are denied before project routes, runtime/deployment/build/GitHub routes, audit records, project secrets, and other protected resources are accessed.

Audit records include the authenticated `actorId`; avoid using shared tokens when you need per-user accountability. Rotate tokens regularly and do not commit real tokens to source control, docs, screenshots, generated files, or Android resources.

## Secret handling

Secret values must never appear in API responses, generated example files, logs, audit metadata, or documentation examples. The current backend stores secret values in memory only for local development and returns `SecretSummary` metadata containing IDs, names, types, storage mode, fingerprints, and timestamps.

## Android local storage

The Android app stores the active project ID locally through `ActiveProjectStore`. It should not persist Discord tokens, GitHub tokens, deployment private keys, or API credentials in plain SharedPreferences. Future production Android secret handling should use Android Keystore-backed storage or avoid local secret persistence entirely.

## Backend secret storage

Current mode: `local_dev` in-memory secret storage. Restarting the backend clears all secret values and metadata. This is not production storage.

Future production requirements:

- Encrypted database records or an external vault.
- Production-grade identity providers for issuing bearer/session credentials and managing per-user/project authorization.
- Strict audit logs around secret creation, rotation, deletion, and reference use.
- Secret value access limited to runtime/build adapters that truly need it.

## Redaction rules

The backend redaction utility removes registered secret values and common token-like strings from logs and JSON serialization. Audit metadata is redacted before storage. Runtime, build, and deployment logs call the redactor before appending log lines.

Sensitive keys such as token, secret, password, API key, authorization, and auth should never be added to deployment target config. Use `secretRefs` instead.

## GitHub token handling

GitHub integration stores only a `tokenSecretRef`, not the token value. Push execution resolves the secret value only in memory and sends generated project files plus the generated GitHub Actions workflow through the GitHub REST API without logging raw tokens. Production GitHub integration should request the narrowest possible scopes, rotate credentials regularly, and review success/failure audit events for push operations.

## Discord token handling

Discord bot tokens are stored as secret values in local-dev memory and referenced by `discord.tokenSecretRef`. Generated bots receive `DISCORD_TOKEN` through the runtime environment; generated `.env.example` contains only placeholders. Never paste a real Discord token into docs, source files, screenshots, generated examples, logs, or audit metadata.

## Build isolation

The build service resolves the generated workspace and refuses to build outside `generated-projects`. It runs `npm install`/`npm ci`, `npm run build`, and optional tests in that controlled workspace. Production builds should run in isolated containers or workers with CPU, memory, filesystem, and network controls.

## Deployment credential handling

Deployment targets accept `secretRefs` for credentials. Target `config` rejects secret-like fields. Production adapters should resolve credentials only at execution time, redact all command output, and avoid writing credential material to disk.

## Android API client configuration

`RoyalScepterApiClient` accepts a bearer token or a session token in its constructor and applies it to every API call. Leave `ApiConfig.DEFAULT_BEARER_TOKEN` and `ApiConfig.DEFAULT_SESSION_TOKEN` empty in committed source; for local debug builds, inject a short-lived development token through local-only Gradle, IDE, or dependency-injection configuration. Prefer bearer tokens for API tooling and session tokens only when the backend has issued a session credential.
