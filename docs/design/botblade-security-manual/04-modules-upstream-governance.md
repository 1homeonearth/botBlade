# 04 — Modules, Upstream Borrowing, and Community Trust

## Purpose

Enable faster product evolution through modules and upstream integrations while preserving verifiable trust boundaries.

## Core principle

No module or upstream component is trusted by default.
Every addition must declare capabilities, permissions, provenance, license status, update policy, tests, and rollback path.

## Module categories

1. Language modules (Node, Python, Rust, Go, Java/Kotlin, PHP, Ruby, Docker, Compose, generic process, GitHub Actions)
2. Platform modules (Discord, Telegram, Slack, GitHub, Matrix, Reddit, Mastodon, Bluesky, Twitch, IRC, webhooks, workers)
3. Build modules (native language build, Dockerfile, Compose, Dev Container, Procfile, Buildpacks, manual command plan)
4. Deployment modules (local process/docker, VPS/SSH, Actions runner, Railway/Render/Fly/Heroku-class hosts, manual export)
5. Android integration modules (terminal, OpenPGP provider, Autofill/password-manager, SAF, browser/custom tabs, share sheet, cloud storage)

## Module manifest model

Path:
- `.botblade/modules/<module-id>/module.yml` (or JSON equivalent)

Required fields:
- id, name, version, description, moduleType
- entrypoint, supportedBotBladeVersion
- license, sourceRepo, pinnedCommit
- checksum, signature
- capabilities
- requiredPermissions, optionalPermissions
- secretsAccess, fileAccess, networkAccess, externalIntents
- commandsExposed, routesExposed
- settingsSchema
- testsRequired
- updatePolicy, rollbackPolicy
- maintainer, trustLevel, humanReviewed

## Permissions and trust levels

Example permissions:
- `read_repository`, `write_repository`
- `read_manifest`, `write_manifest`
- `read_logs`, `write_logs`
- `read_secret_metadata`, `request_secret_value_at_runtime`
- `start_process`, `stop_process`, `open_terminal`
- `create_build`, `create_deployment`, `external_intent`
- `network_access`, `filesystem_workspace_write`
- `filesystem_host_access` (blocked by default)
- `docker_socket_access` (blocked by default)

Trust levels:
- `builtin`
- `official`
- `trusted_upstream`
- `community_reviewed`
- `local_unreviewed`
- `blocked`

## Module security constraints

- scan modules before enablement
- deny secret-value access unless explicitly granted at runtime
- deny writes outside workspace
- deny terminal launch without policy approval
- deny external intents without preview/confirmation
- deny host filesystem and docker socket access by default
- deny self-update without explicit policy path

## Native embedded feature candidates

- Rust security core
- terminal UI component (only after license review)
- static language detection scanners
- manifest editor with Rust validation path
- archive importer with Rust-backed checks
- OpenPGP provider bridge (external provider boundary)
- SAF/FileProvider bundle manager with checksum/provenance

## Trusted upstream governance

Use `docs/design/botblade-security-manual/upstreams.yml` as source of truth.

Each entry should track:
- id/name/source repo + URL
- license
- usage type (dependency, vendored module, reference-only, external app integration, platform feature, commercial candidate)
- pinned version/commit
- imported/local paths
- local modifications
- advisory sources
- required tests
- rollback plan
- owner and notes

## License boundaries

- Do not vendor GPL application code into botBlade without explicit legal/product approval.
- Prefer reference-only or external-app integration for GPL apps.
- Record all actual code imports with provenance and attribution.

## Upstream update workflow

1. review upstream release/changelog
2. review license status/changes
3. review security advisories
4. update pinned version/commit in controlled branch
5. run required tests + app checks
6. review local modifications + rollback notes
7. merge only after green checks

## Community trust expectations

- keep manual public and current
- maintain upstream list
- preserve implemented-vs-planned clarity
- provide exportable redacted safety/import diagnostics
- keep audit logs and provenance metadata reviewable

## Tests

- module manifest schema validation
- capability/permission enforcement tests
- trust-level gate behavior tests
- upstream metadata presence/consistency tests
- GPL vendoring boundary checks
