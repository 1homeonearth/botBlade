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

## Environment-scoped check policy

### Codex sandbox / cloud agent execution
- **Do not run Gradle commands** (`gradle`, `./gradlew`, or Android assemble/test tasks) from Codex sandbox sessions.
- Use only the smallest offline-capable checks that do not require Android toolchain downloads (for example: targeted `npm` scripts, static validation, or file-level checks).
- Never treat Gradle commands as universally required in Codex; defer Android triage to CI/local runners.

### GitHub Actions / local developer runner
- Use the following commands for Android triage when validating Android build/test issues in CI or on a fully provisioned local machine:
  - Android SDK preflight: `./scripts/android-sdk-preflight.sh`
  - Android debug assemble: `gradle :app:assembleDebug`
  - Android unit tests (flavor-specific): `gradle :app:testLocalDevDebugUnitTest`
- Backend build check (all environments when applicable): `npm run build`

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

## botBlade security design manual
- Before modifying any of the following areas, read `docs/design/botblade-security-manual/README.md` and relevant linked sections:
  - repo import
  - archive extraction
  - manifests
  - build plans
  - runtime profiles
  - terminal sessions
  - external app integrations
  - secrets
  - sandboxing
  - upstream dependencies
  - Rust crates
  - deployment security
  - native modules/plugins
- Keep the manual updated whenever behavior in those areas changes.
- Add tests for security gates, archive handling, terminal handling, path resolution, secret redaction, external intents, and upstream dependency changes when applicable.
- Do not vendor upstream code without an `upstreams.yml` entry, license review, attribution, and tests.
- Prefer Rust for security-critical validation.
- Preserve existing Discord behavior while universal workload support evolves.
