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

**Status:** Incomplete (repeat Android SDK availability issue observed 2026-05-18T23:37:00Z)

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

### 2026-05-18T23:37:00Z repeat occurrence — SDK missing in current validation environment

**Status:** Incomplete

**Context:** While validating GitHub Actions release-link changes, `gradle :app:assembleDebug :app:assembleRelease --stacktrace` failed before task execution because the current container had no `ANDROID_HOME`, `ANDROID_SDK_ROOT`, or `local.properties` SDK pointer. This repeats the earlier Android validation environment class of issue.

**Key logs:**

```text
Could not determine the dependencies of task ':app:compileDebugJavaWithJavac'.
> SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/royalScepter/local.properties'.
```

**Resolution attempt:**
- Ran `./scripts/android-sdk-bootstrap.sh` to reinstall Android command-line tools and write `local.properties`.
- The bootstrap failed because `curl` received HTTP 403 from `https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip`.

**Bootstrap log:**

```text
curl: (22) The requested URL returned error: 403
Failed to download Android command-line tools.
This environment may require an approved mirror.
```

**Relevant versions observed:**
- Gradle: 8.14.4
- Android Gradle Plugin: 8.7.3
- Kotlin Android plugin: 2.0.21

**Next steps:** Retry `./scripts/android-sdk-bootstrap.sh` in an environment that can access `dl.google.com`, or set `ANDROID_CMDLINE_TOOLS_URL` to an approved mirror for `commandlinetools-linux-13114758_latest.zip`, then rerun `./scripts/android-sdk-preflight.sh` and `gradle :app:assembleDebug :app:assembleRelease --stacktrace`.

### 2026-05-19T01:32:00Z repeat occurrence — bootstrap retry still blocked by Google download 403

**Status:** Incomplete

**Context:** Re-attempted to resolve the outstanding Android SDK blocker so the previously incomplete chain could be closed and obsolete logs removed.

**Resolution attempt:**
- Ran `./scripts/android-sdk-bootstrap.sh` again in the current container.
- Download still failed with HTTP 403 from `dl.google.com`, so Android SDK bootstrap cannot complete in this environment without a mirror URL.

**Key logs:**

```text
curl: (22) The requested URL returned error: 403
Failed to download Android command-line tools.
```

**Outcome:**
- Issue remains incomplete and cannot yet be removed from `ISSUES.md`.


## 2026-05-19T00:12:00Z — Test request-id generation used `Math.random()` instead of stable UUID primitive

**Status:** Complete

**Context:** Repository-wide review identified a test-quality/security inconsistency in backend route tests: request IDs were generated with `Math.random()`, which is non-cryptographic and less deterministic for uniqueness guarantees than platform UUID helpers.

**Troubleshooting and resolution:**
- Replaced `Math.random().toString(16).slice(2)` with `randomUUID()` from `node:crypto` in `backend/src/__tests__/serverRoutes.test.ts`.
- Preserved existing `req_test_` prefix so log filtering behavior and assertions remain unchanged.

**Relevant versions observed:**
- Node.js runtime used by tests via npm scripts (`backend/package.json` test command).

**Validation:**
- `cd backend && npm test -- --runInBand` completed successfully after the change.
- `cd backend && npm audit --audit-level=moderate` reported `found 0 vulnerabilities`.

**Remaining warnings:**
- npm emits `Unknown env config "http-proxy"`; this originates from the execution environment config rather than repository code.

### 2026-05-19T00:42:00Z repeat occurrence — npm warns about deprecated `http-proxy` env config

**Status:** Complete

**Context:** User reported repeated warning during backend test runs:

```text
npm warn Unknown env config "http-proxy". This will stop working in the next major version of npm.
```

**Troubleshooting and resolution:**
- Confirmed warning is emitted before script execution by running `npm config list -l`.
- Verified environment-level proxy values are present (`http-proxy` and `https-proxy`) and warning is not caused by repository `.npmrc` files.
- Reduced nested npm invocations in `backend/package.json` by replacing `npm run build` within `dev` and `test` scripts with direct `tsc -p tsconfig.json`, which removes duplicate warning emissions from chained npm subprocesses.
- Added local development guidance documenting forward-compatible proxy configuration (`HTTP_PROXY` / `HTTPS_PROXY` / `NO_PROXY` or underscore npm env keys).

**Validation:**
- `cd backend && npm test -- --runInBand` completed successfully.
- `cd backend && npm run build` completed successfully.

**Remaining warnings:**
- One warning may still appear when invoking npm in environments that export deprecated `http-proxy` key; this must be corrected in user/CI environment variables for full removal.

## 2026-05-19T01:05:00Z — Request path parsing depended on Host header and could throw non-deterministically

**Status:** Complete

**Context:** Full-repo review found that request URL parsing in `backend/src/server.ts` used `new URL(req.url, \`http://${req.headers.host}\`)`. This coupled path parsing to caller-controlled host header formatting and could throw inconsistent errors for malformed host values.

**Troubleshooting and resolution:**
- Reworked request path extraction to parse against a constant base (`http://localhost`) via new `extractPathname()` helper.
- Added explicit `INVALID_REQUEST_URL` 400 response for malformed request URLs.
- Added route test coverage for malformed request URL handling to prevent regressions.

**Validation:**
- `cd backend && npm test -- --runInBand` completed successfully.
- `cd backend && npm run build` completed successfully.

**Remaining warnings:**
- npm environment still emits one `Unknown env config "http-proxy"` warning at npm startup in this container.

### 2026-05-19T02:10:00Z repeat occurrence — bootstrap retry still blocked by Google download 403

**Status:** Incomplete

**Context:** Per repository workflow requirements, retried unresolved Android SDK bootstrap blocker before starting new requested changes.

**Resolution attempt:**
- Ran `./scripts/android-sdk-bootstrap.sh` from repository root.
- Download failed again with HTTP 403 from `https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip`.

**Key logs:**

```text
curl: (22) The requested URL returned error: 403
Failed to download Android command-line tools.
```

**Outcome:**
- Issue remains incomplete in this environment without an approved mirror URL for Android command-line tools.
