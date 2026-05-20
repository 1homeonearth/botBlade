# 02 — Security Policy and Sandboxing

## Core principle

Policy gates evaluate safety before build/test/runtime/deployment/terminal/integration actions.

Security decisions are explicit, deterministic, and fail closed.

## Security gates

Required gates:
- `repository_intake`
- `archive_extract`
- `manifest_save`
- `build_plan_create`
- `build_start`
- `test_start`
- `runtime_profile_activate`
- `runtime_start`
- `deployment_start`
- `terminal_open`
- `external_app_intent`
- `module_install`
- `upstream_update`

Gate behavior:
- evaluate preconditions and policy inputs
- return `allowed`, `allowed_with_restrictions`, `requires_confirmation`, or `blocked`
- deny by default if evidence is missing/stale/inconsistent
- always emit redaction-safe audit metadata

## Policy model

Policy layers:
- global baseline
- environment overlays (dev/staging/prod/local)
- workload/runtime profile overlays
- time-bounded, auditable session overrides

Conflict resolution: most restrictive rule wins.

## Safety decisions and evidence

Decision artifacts should include:
- decision ID
- gate name
- policy version
- safety report linkage
- hash/key inputs used
- restriction list (if any)
- confirmation requirements
- expiration/freshness window
- redacted reasoning summary

## Sandbox defaults

Default sandbox posture:
- network disabled during initial scan
- no host filesystem access
- no host secrets
- no docker socket mount
- no privileged containers
- no build-time secrets by default
- runtime secrets only through approved `secretRefs`
- CPU/memory/file-count/file-size/timeout limits enforced
- network allowlist support reserved for specific deployment workflows

## Secret handling

- Secret values are never persisted to docs/logs/reports/test fixtures.
- Policy operates on secret references and metadata, not values.
- Injection requires explicit gate approval.
- Redaction applies to logs, diagnostics, terminal, and export artifacts.

## Repo provenance and import safety

- Treat all imported repos/archives as untrusted.
- Do not execute repository code during analysis.
- Block path traversal, symlink escape, and suspicious archive behavior before extraction.
- Track source metadata for provenance context (without implying trust).

## Runtime performance strategy

- Gate runtime start with cached safety artifacts when fresh.
- Re-scan only on invalidation triggers.
- Avoid full repo scan on every runtime start.

## Audit events

Every gate action should record:
- actor/context
- gate + decision
- policy/safety artifact IDs
- restrictions/confirmations
- sanitized rationale
- timestamps + TTL

## Tests

Policy/sandbox tests should cover:
- gate deny on missing evidence
- gate restrictions for medium-risk findings
- explicit confirmation requirement paths
- stale cache/runtime deny behavior
- secret redaction in all emitted artifacts
- archive/import fail-closed checks

## Stern build reliability gate (APK compilation)

If APK compilation fails due to dependency fetch failures (for example `Could not GET ... dl.google.com ... 403 Forbidden`), treat this as a blocking infrastructure/security issue:
- verify proxy/TLS settings (`HTTP_PROXY`, `HTTPS_PROXY`, `GRADLE_OPTS`, custom CA settings)
- verify Gradle can reach required artifact repositories
- fix dependency resolution before continuing unrelated feature work

Reason: unresolved dependency sources invalidate security/build reproducibility and mask real runtime defects.
