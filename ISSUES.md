# Issues Log

## 2026-05-18T22:29:14Z — Android debug build Kotlin compile failure and GitHub Actions Node 20 runtime warning

**Status:** Complete

**Context:** The `android-debug-build` workflow failed at Kotlin compilation because `BotBladeApiClient.kt` called an undefined `urlPathSegment()` helper in deployment status/action paths. The same API client already uses `encodedPathSegment()` for surrounding deployment endpoints. The workflow also used JavaScript action majors that run on the older Node.js 20 action runtime.

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

## 2026-05-18T22:50:00Z — Android validation environment and JVM unit-test server failures during botBlade rename

**Status:** Complete

**Context:** While validating the rename and APK release workflow, the first Android Gradle validation attempt could not locate an Android SDK because this container did not have `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties` configured. After bootstrapping the SDK, `:app:compileDebugUnitTestKotlin` failed because the unit test imported `com.sun.net.httpserver`, which was unavailable to the Kotlin unit-test compile classpath. After replacing that test helper, the path-encoding test still failed because JDK `HttpURLConnection` rejects `PATCH` in JVM unit tests.

**Troubleshooting and resolution:**
- Ran `./scripts/android-sdk-bootstrap.sh`; the command downloaded command-line tools but exited with code 141 during the license pipe before installing packages.
- Ran `sdkmanager --sdk_root=/root/android-sdk "platform-tools" "platforms;android-35" "build-tools;35.0.0"` directly and wrote `local.properties` with `sdk.dir=/root/android-sdk`.
- Confirmed `./scripts/android-sdk-preflight.sh` passed.
- Replaced the `com.sun.net.httpserver`-based unit-test helper with a small local `ServerSocket` JSON server using only standard Java APIs visible to Kotlin unit tests.
- Adjusted the URL path encoding test to exercise command path encoding through the supported `DELETE` method instead of the JVM-incompatible `PATCH` call.
- Increased Gradle JVM/metaspace settings in `gradle.properties` after Gradle reported daemon metaspace exhaustion during release assembly.

**Relevant versions observed:**
- Gradle: 8.14.4
- Android Gradle Plugin: 8.7.3
- Kotlin Android plugin: 2.0.21
- Android SDK: `/root/android-sdk`, with `platforms;android-35` and `build-tools;35.0.0`

**Validation:**
- `./scripts/android-sdk-preflight.sh` completed successfully.
- `gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --stacktrace` completed successfully.
- `gradle :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease --warning-mode all` completed successfully; remaining deprecation warnings originate from Android Gradle Plugin/Gradle internals rather than repository build scripts.

**Remaining warnings:** Android Gradle Plugin 8.7.3 emits Gradle 8.14.4 deprecation warnings for generated/internal `is-` boolean properties and one null attribute lookup during `lintVitalAnalyzeRelease`.
