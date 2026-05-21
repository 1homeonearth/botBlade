# Agent Workflow Notes

## ISSUES.md usage
- Always review `ISSUES.md` before starting new work.
- If there are unresolved issues, attempt resolution first and log each attempt with:
  - UTC timestamp, command(s) run, observed output/errors, status (Complete or Incomplete)
- If a repeated issue occurs, add the new entry directly below the most recent entry
  in that issue chain.
- When an issue is confirmed resolved by rerunning relevant checks, remove the
  resolved issue chain from `ISSUES.md`.

## LEFTOVERS.md usage
- Review `LEFTOVERS.md` (if present) before new work.
- Complete listed tasks before starting new prompt work.
- If work cannot be completed in one session, update/create `LEFTOVERS.md`
  with concrete continuation steps.

---

# Agent Instructions

## ISSUES.md workflow
- Keep a root-level `ISSUES.md` for troubleshooting history.
- Before starting new work, review `ISSUES.md` and `LEFTOVERS.md` at the repo root.
- Resolve incomplete issues before beginning new work when practical. Record each
  resolution attempt with UTC timestamp, context, logs or key errors, versions, status.
- Group repeat occurrences under the most recent matching issue chain.
- When resolved, mark complete and prune obsolete raw error logs.
- If work cannot be completed, write clear continuation steps in `LEFTOVERS.md`.

## Required GitHub Actions secrets for Android release signing
Configure these repository secrets for signed release APK output:
- `KEYSTORE_BASE64`: Base64-encoded Android signing keystore.
- `KEYSTORE_PASSWORD`: Keystore password.
- `KEY_ALIAS`: Alias name of the signing key entry.

If any of these secrets are missing, the signing step is skipped automatically
and release uploads fall back to the unsigned APK.

---

## botBlade security design manual

**MANDATORY READ**: Before any work involving repository import, archive extraction,
manifests, build plans, runtime profiles, terminal sessions, external app integrations,
secrets, sandboxing, upstream dependencies, Rust crates, deployment security, or
module/plugin systems, read:

  docs/design/botblade-security-manual/README.md

## Environment-scoped verification policy

### Codex sandbox / cloud agent execution
- Avoid `./gradlew` and `gradle` verification when the environment lacks network access,
  Android SDK packages, or wrapper/bootstrap support.
- Use `actionlint .github/workflows/android.yml` for workflow file verification.
- Use static file inspection (`read`, `grep`, `rg`) for Kotlin error triage when Gradle
  execution is unavailable.
- Treat GitHub Actions CI as the source of truth for APK compilation in restricted agent
  environments.

### GitHub Actions / local developer runner
- Run the smallest relevant check available for the change.
- Android SDK preflight: `./scripts/android-sdk-preflight.sh`
- Android debug assemble: `gradle :app:assembleDebug`
- Android unit tests: `gradle :app:testLocalDevDebugUnitTest`
- Backend build: `npm run build`

## APK compile failure triage protocol
1. Run `actionlint .github/workflows/android.yml` and confirm exit 0.
2. Read the first failing Gradle task from the CI log.
3. Check if the failure is dependency resolution; if so, identify the infra or mirror issue before changing app code.
