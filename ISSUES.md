# Issues Log

## 2026-05-18T22:29:14Z — Android debug build Kotlin compile failure and GitHub Actions Node 20 runtime warning

**Status:** Complete

**Context:** The `android-debug-build` workflow failed at Kotlin compilation because `RoyalScepterApiClient.kt` called an undefined `urlPathSegment()` helper in deployment status/action paths. The same API client already uses `encodedPathSegment()` for surrounding deployment endpoints. The workflow also used JavaScript action majors that run on the older Node.js 20 action runtime.

**Troubleshooting and resolution:**
- Replaced the five undefined deployment `urlPathSegment()` calls with the existing `encodedPathSegment()` helper.
- Verified no `urlPathSegment` references remain.
- Added workflow-level `FORCE_JAVASCRIPT_ACTIONS_TO_NODE24: true` so all jobs in the Android workflow exercise GitHub Actions' Node.js 24 runtime readiness.
- Checked upstream action tags and updated the Android workflow to newer stable Node 24-ready majors for the existing trusted actions: `actions/checkout@v5`, `actions/setup-java@v5`, `android-actions/setup-android@v4`, and `gradle/actions/setup-gradle@v5`.

**Relevant versions observed:**
- System Gradle: 8.14.4 (`/root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/bin/gradle`)
- Android SDK used for validation: `/root/android-sdk`, with `platforms;android-35` and `build-tools;35.0.0`

**Validation:**
- `rg -n "urlPathSegment" . || true` returned no matches.
- `./gradlew :app:compileDebugKotlin` and `./gradlew :app:compileDebugKotlin --warning-mode all` could not run because this checkout has no `gradlew` wrapper file.
- `gradle :app:compileDebugKotlin` completed successfully.
- `gradle :app:compileDebugKotlin --warning-mode all` completed successfully and emitted no Gradle deprecation warnings.
- `gradle :app:compileDebugKotlin --stacktrace` completed successfully, matching the workflow fallback path used when `./gradlew` is absent.
- `gradle :app:assembleDebug --stacktrace` completed successfully.

**Remaining warnings:** None from the successful `--warning-mode all` compile run.
