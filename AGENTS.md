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
