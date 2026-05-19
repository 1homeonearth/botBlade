# Repository Agent Guide

## Session workflow (run in order)
1. Read `docs/project/ISSUES.md` and `docs/project/LEFTOVERS.md` before making changes.
2. Resolve unresolved issue chains when practical before starting new prompt work.
3. Log every troubleshooting attempt under the latest matching issue chain with UTC timestamp, commands, outputs/errors, versions/environment details, and `Complete`/`Incomplete` status.
4. Verify presumed fixes with relevant tests/checks before marking an issue resolved.
5. If session work cannot be finished, update `docs/project/LEFTOVERS.md` with exact continuation steps, commands already run, blockers, and remaining scope.

## ISSUES.md entry template
Use this template for each new attempt entry under the latest matching issue chain:

```md
### <UTC timestamp> — <short attempt label>
- Context:
- Commands run:
  - `...`
- Observed output/errors:
- Versions/environment:
- Status: Complete | Incomplete
- Next action:
```

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

## Avoid
- Creating a new issue chain for a known repeat; append to the most recent equivalent chain.
- Claiming resolution without recording the exact verification command/result.
- Leaving large stale raw error logs once a fix is validated (retain final useful resolution notes).
- Starting unrelated feature work before triaging unresolved blockers when practical.

## Android release signing secrets
For required GitHub Actions signing secrets and setup notes, see `docs/ci/android-signing.md`.
