# Agent Workflow Notes

## ISSUES.md usage

- Always review `docs/project/ISSUES.md` before starting new work.
- If there are unresolved issues, attempt resolution first and log each attempt with:
  - UTC timestamp
  - command(s) run
  - observed output/errors
  - status (`Complete` or `Incomplete`)
- If a repeated issue occurs, add the new entry directly below the most recent entry in that issue chain.
- When an issue is confirmed resolved by rerunning relevant checks, remove the resolved issue chain from `docs/project/ISSUES.md`.

## LEFTOVERS.md usage

- Review `docs/project/LEFTOVERS.md` (if present) before new work.
- Complete listed tasks before starting new prompt work.
- If you cannot finish all requested work in one session, update/create `docs/project/LEFTOVERS.md` with concrete next steps.
# Agent Instructions

## ISSUES.md workflow

- Keep `docs/project/ISSUES.md` as the troubleshooting history file for troubleshooting history.
- Before starting new work in this repository, review `docs/project/ISSUES.md` and `docs/project/LEFTOVERS.md` under `docs/project/`.
- Resolve incomplete issues before beginning new requested work when practical, and record each resolution attempt with a UTC timestamp, context, logs or key error messages, software versions, and status.
- Group repeat occurrences directly under the most recent equivalent issue chain rather than creating unrelated duplicate sections.
- When a problem is confirmed resolved by a successful test, mark it complete and remove obsolete raw error-log chains that are no longer useful.
- If work cannot be completed before the session ends, update root-level `docs/project/LEFTOVERS.md` with enough context and next steps for the next agent to continue safely.


## How to use ISSUES.md

- Treat `docs/project/ISSUES.md` as the canonical troubleshooting journal for this repository.
- At session start, read both `docs/project/ISSUES.md` and `docs/project/LEFTOVERS.md` before making code changes.
- For each issue or validation failure, append a UTC-stamped attempt entry under the most recent matching issue chain, including commands run, relevant logs/errors, versions, and current status.
- After a fix is validated by a passing test, mark that issue chain complete and prune obsolete raw logs that are no longer useful for future debugging.
- If work is not finished, write clear continuation steps in `docs/project/LEFTOVERS.md` so the next agent can resume safely.

## Required GitHub Actions secrets for Android release signing

Configure these repository secrets for signed release APK output in `.github/workflows/android.yml`:

- `KEYSTORE_BASE64`: Base64-encoded Android signing keystore file contents.
- `KEYSTORE_PASSWORD`: Keystore password (also used as key password in the workflow).
- `KEY_ALIAS`: Alias name of the signing key entry inside the keystore.

If any of these secrets are missing, the signing step is skipped automatically and release uploads fall back to the unsigned APK.
