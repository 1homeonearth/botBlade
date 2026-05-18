# LEFTOVERS

## Session date
2026-05-18 (UTC)

## Completed in this session
- Implemented backend GitHub push path with token secret reference resolution, generated workflow inclusion, audit success/failure recording, and `lastPushedAt` update only after successful push.
- Added backend tests for GitHub push not-configured, repo-not-linked, and success paths with a mock GitHub client.

## Remaining requested work
2. **Android GitHub defer UX + validation**
   - If GitHub push remains deferred in UI flows, disable/hide push button and show copyable workflow/export instructions.
   - Warn for missing Discord IDs based on command registration mode.

3. **Android release UX sweep**
   - First-run checklist, loading/progress guards, card/list redesign for multiline rows, copy buttons, empty/retry states.
   - Ensure all text is moved to `strings.xml`.

4. **Android tests expansion**
   - Add/verify Robolectric/JUnit deps in `app/build.gradle.kts`.
   - Add tests for `BotBladeApiClient` parsing helpers, `ActiveProjectStore` persistence, URL encoding, redaction, GitHub display formatting.
   - Document Android test command in `README.md`.

5. **Production deployment path completion**
   - Confirm one adapter as primary target and add full deploy lifecycle semantics + tests.
   - Ensure secrets remain in `secretRefs` only.
   - Update Android Deployments UI capability/unsupported messaging as needed.

6. **Android release packaging polish**
   - Launcher/adaptive icons across densities.
   - Release signing docs.
   - Build types/flavors for local vs prod API config.
   - Versioning policy.
   - Privacy/data-safety/screenshot docs.
   - Verify debug vs release backup/cleartext/network security behavior.

## Environment blockers to revisit first next session
- Android SDK validation is blocked in this container: no SDK variables/local.properties are present, and `./scripts/android-sdk-bootstrap.sh` cannot download command-line tools because `dl.google.com` returns HTTP 403. Retry with network access to Google downloads or set `ANDROID_CMDLINE_TOOLS_URL` to an approved mirror, then run `./scripts/android-sdk-preflight.sh` and `gradle :app:assembleDebug :app:assembleRelease --stacktrace`.
