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
- Backend build: `npm run build`

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
