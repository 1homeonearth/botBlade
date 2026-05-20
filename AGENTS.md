# Repository Agent Guide

Finish the requested task completely when possible.

At session start, read LEFTOVERS.md if present. Attempt every actionable leftover before starting new work. Delete completed leftovers after verification. Delete LEFTOVERS.md when empty.

Use LEFTOVERS.md only for unfinished handoff work. Keep it short: blocker, relevant files, exact next action.

After editing, run the smallest useful check available.

## Instruction precedence and scope
- Root `AGENTS.md` applies repository-wide.
- A nested `AGENTS.md` overrides root instructions for files in that subdirectory tree.
- If `AGENTS.override.md` exists at a directory level, treat it as the active instruction file for that level instead of `AGENTS.md`.
- Example: `app/mobile/AGENTS.md` overrides root guidance only for `app/mobile/**`.

## Repository expectations
- Keep instructions concise and action-oriented; avoid duplicating code-level details available in source files.
- Prefer targeted edits and preserve existing project conventions.
- Use existing tooling/package managers already used by the repo.

## Common build/test/lint checks
- Android SDK preflight: `./scripts/android-sdk-preflight.sh`
- Android debug assemble: `gradle :app:assembleDebug`
- Android unit tests (flavor-specific): `gradle :app:testLocalDevDebugUnitTest`
- Backend build: `npm run build` (repo root proxies to `backend`; backend package root is `backend/`)

## Android release signing secrets
For required GitHub Actions signing secrets and setup notes, see `docs/ci/android-signing.md`.


## CI health gate
- Before opening any PR, check the current status of `.github/workflows/android.yml`
  on the target branch. If it is failing, do not open new PRs for unrelated work.
  The only PR permitted when the workflow is red is one that directly fixes the
  failing workflow.
- Do not merge any PR to main while the android.yml workflow is in a failure state
  on main, regardless of whether the PR itself touches the workflow file.
- After merging any PR that modifies .github/workflows/android.yml, verify the
  next workflow run succeeds before opening or merging further PRs.
- If workflow status cannot be queried because runtime auth is missing, mark the gate as
  **not verifiable (token unavailable)** and follow `docs/project/gh-cli-auth.md`
  (Token-less CI verification fallback playbook). Do **not** classify this as CI failing.


## Release-facing docs maintenance
- On each release (or at minimum every 5 releases), review and refresh user-facing release docs: `README.md`, `INSTALL.md`, and `docs/releases.md`.
- Remove stale/manual release links in README and rely on the repository main page + latest-release automation paths.
- Keep release notes concise: end-user changes, developer changes, known limitations.

## Codex local check policy
- In Codex/offline environments, avoid `./gradlew` checks that require wrapper downloads.
- Prefer smallest offline-capable checks first (for example: `gradle` if available locally, targeted `npm`/script checks, and static file validation).
- Do not change GitHub workflow validation behavior just to satisfy local Codex constraints.

## botBlade security design manual
- Before modifying any of the following areas, read `docs/design/botblade-security-manual/README.md` and relevant linked sections:
  - repository import
  - archive extraction
  - manifests
  - build plans / command plans
  - runtime profiles
  - terminal sessions
  - external app integrations
  - secrets
  - sandboxing
  - upstream dependency borrowing
  - Rust crates
  - deployment security
  - native modules/plugins
- Treat the manual as a required design contract for covered work; implementation and docs changes in those areas must conform to it unless intentionally superseded in a documented design update.
- Keep the manual updated whenever behavior in those areas changes.
- Add tests for security gates, archive handling, terminal handling, repo-import handling, path resolution, secret redaction, external intents, and upstream dependency changes when applicable.
- Do not vendor upstream code without an `upstreams.yml` entry, license review, attribution, and tests.
- Prefer Rust for hostile-input parsing, path normalization, archive validation, manifest validation, command-plan validation, checksum/signature helpers, and policy evaluation.
- Preserve existing Discord behavior while universal workload support evolves.
- **Stern reliability gate:** if Android builds fail with dependency-download errors (for example `Could not GET ... dl.google.com ... 403 Forbidden`), stop feature work and first resolve repository/network/proxy configuration (`HTTP_PROXY`/`HTTPS_PROXY`/`GRADLE_OPTS`) so `gradle :app:assembleDebug` can resolve dependencies again. Do not mark APK compile failures as non-blocking.

- **Hard CI/APK triage checklist (required before feature work resumes when APK compile breaks):** run `./scripts/check-android-ci-health.sh main` (or record **not verifiable (token unavailable)**), run `./scripts/android-sdk-preflight.sh`, and run `gradle :app:assembleDebug --stacktrace`. Capture the first failing task and root cause in PR notes.
- **Do not misclassify build command failures:** if `npm run build` fails at repo root due to missing script/package wiring, fix workspace scripts immediately (root build must delegate to backend build).


## Codex runtime network constraints

The Codex Cloud agent phase cannot reach Android Maven repositories:
- Proxied path → dl.google.com returns HTTP 403 Forbidden
- Unproxied path → UnknownHostException (no DNS in sandbox)

This affects all `./gradlew` and `gradle` commands that need dependency resolution,
the CI health check script (also needs GH_TOKEN, unavailable at runtime),
and any preflight script that makes network calls.

Rules for all agents:
- Never run ./gradlew or gradle to verify code changes.
- Never treat a Gradle sandbox failure as evidence of a code bug.
- Use `actionlint .github/workflows/android.yml` for workflow file verification.
- Use static file inspection (read, grep) for Kotlin error verification.
- All Gradle build verification happens exclusively in GitHub Actions CI.
- The ⚠️ "token unavailable" warning from check-android-ci-health.sh is expected and harmless. Do not attempt to fix it from within a task.
- Dependencies are pre-fetched during setup via ./gradlew resolveAllDependencies. If the cache is stale, reset the Codex environment to trigger a fresh setup.
