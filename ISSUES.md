# Issues Log

## 2026-05-18T07:05:04Z — Android SDK unavailable in current shell

- **Status:** Incomplete — environment limitation; repository changes include documentation and preflight automation, but this shell does not expose an Android SDK through `ANDROID_HOME`.
- **Context:** The session requested Android SDK bootstrap guidance, a CI preflight check for `platforms/android-35` and `build-tools/35.0.0`, and build verification in an SDK-enabled environment.
- **Observed details:** `ANDROID_HOME` was empty when checked in the current shell; using the placeholder `/path/to/android-sdk` also failed because no SDK exists at that path in this environment.
- **Troubleshooting steps taken:**
  - Confirmed Gradle is available at `/root/.local/share/mise/installs/gradle/8.14.4/gradle-8.14.4/bin/gradle`.
  - Confirmed Java is available at `/root/.local/share/mise/installs/java/21/bin/java`.
  - Added a checked-in SDK preflight script that reports missing `ANDROID_HOME`, missing SDK root, or missing required SDK package directories before Gradle build work starts.
  - Added CI setup steps that install `platforms;android-35`, `build-tools;35.0.0`, and `platform-tools` before running the preflight and Android build commands.
- **Relevant logs:**
  - `ANDROID_HOME=`
  - `ANDROID_HOME=/path/to/android-sdk gradle :app:compileDebugKotlin` failed with `SDK location not found`.
  - `ANDROID_HOME=/path/to/android-sdk gradle :app:assembleDebug` failed with `SDK location not found`.
- **Resolution notes:** Run `ANDROID_HOME=/path/to/android-sdk gradle :app:compileDebugKotlin` and `ANDROID_HOME=/path/to/android-sdk gradle :app:assembleDebug` in an SDK-enabled environment.
- **Repeat occurrence 2026-05-18T08:07:51Z:** Android client configuration changed for API authentication, but this shell still cannot run Android compilation because `ANDROID_HOME` is unset.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set.`
  - Status: Incomplete — environment limitation remains; run Android Gradle checks in an SDK-enabled environment.
- **Repeat occurrence 2026-05-18T08:25:39Z:** Pre-session unresolved issue review repeated the Android SDK preflight before durable persistence work.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set.`
  - Status: Incomplete — environment limitation remains; no Android SDK is exposed in this shell.
- **Repeat occurrence 2026-05-18T08:33:15Z:** Attempted to resolve the shell `ANDROID_HOME` issue by installing Android command-line tools and writing shell exports.
  - Commands: `./scripts/android-sdk-bootstrap.sh`, `curl -fL https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip`, `apt-get update`, `./scripts/android-sdk-preflight.sh`.
  - Result: `ANDROID_HOME` and `ANDROID_SDK_ROOT` are now exported for new login shells via `/root/.bash_profile`, and untracked `local.properties` points Gradle at `/root/android-sdk`. Direct downloads for command-line tools and apt repositories still return `403 Forbidden` through the configured proxy, so required SDK package directories are still unavailable.
  - Status: Incomplete — the unset environment variable is fixed for new shells, but the actual Android SDK packages cannot be installed until this environment has an approved mirror or proxy access for Android SDK artifacts. Added `scripts/android-sdk-bootstrap.sh` and documented `ANDROID_CMDLINE_TOOLS_URL` mirror override to complete the install once a reachable mirror is available.
- **Repeat occurrence 2026-05-18T09:42:52Z:** Pre-session unresolved issue review repeated the Android SDK preflight before PATCH parsing work.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set and no sdk.dir was found in local.properties.`
  - Status: Incomplete — environment limitation remains; no SDK root or approved Android SDK artifact mirror is available in this shell.
- **Repeat occurrence 2026-05-18T09:48:44Z:** Pre-session unresolved issue review repeated the Android SDK preflight before backend URL configuration work.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set and no sdk.dir was found in local.properties.`
  - Status: Incomplete — environment limitation remains; no SDK root or approved Android SDK artifact mirror is available in this shell.
- **Repeat occurrence 2026-05-18T09:51:47Z:** Android compilation verification for backend URL configuration changes hit the same missing SDK limitation.
  - Command: `gradle :app:compileDebugKotlin`
  - Result: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/royalScepter/local.properties'.`
  - Status: Incomplete — environment limitation remains; Kotlin/Android compilation must be rerun in an SDK-enabled environment.
- **Repeat occurrence 2026-05-18T10:06:14Z:** Pre-session unresolved issue review repeated the Android SDK preflight before Android release packaging polish.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set and no sdk.dir was found in local.properties.`
  - Status: Incomplete — environment limitation remains; no SDK root or approved Android SDK artifact mirror is available in this shell.
- **Repeat occurrence 2026-05-18T10:14:02Z:** Full Android assemble verification for release packaging changes hit the same missing SDK limitation.
  - Command: `gradle :app:assembleLocalDevDebug :app:assembleProdRelease`
  - Result: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/royalScepter/local.properties'.`
  - Status: Incomplete — environment limitation remains; Gradle configuration and manifest processing were verified, but full APK/AAB assembly must be rerun in an SDK-enabled environment.

## 2026-05-18T07:43:01Z — Public npm registry returns 403 for generated bot dependency resolution

- **Status:** Complete — generated bot installs now pin `discord.js`, include a generated lockfile, document registry mirror configuration, and emit redacted registry diagnostics for install failures.
- **Context:** While hardening generated bot installs from `backend/src/services/projectFiles.ts`, this environment returned `403 Forbidden` when attempting to resolve `discord.js@14.15.3` from the public npm registry.
- **Observed details:**
  - `npm install --package-lock-only --ignore-scripts --audit=false --fund=false` failed for a temporary generated bot package.
  - npm version: `11.4.2`.
  - Node version: `v20.20.2` in this shell; generated bots still target Node 22 through their template/Dockerfile.
- **Relevant logs:**
  - `npm error code E403`
  - `npm error 403 403 Forbidden - GET https://registry.npmjs.org/discord.js`
  - `npm warn Unknown env config "http-proxy". This will stop working in the next major version of npm.`
- **Troubleshooting steps taken:**
  - Reproduced the 403 with both the default registry and `NPM_CONFIG_REGISTRY=https://registry.npmmirror.com`.
  - Added generated README instructions for `NPM_CONFIG_REGISTRY` and local `.npmrc` registry mirror usage.
  - Added a backend-side generated bot lockfile template and verified the generated `package-lock.json` is accepted by `npm ci --package-lock-only --ignore-scripts --audit=false --fund=false` without registry metadata resolution.
  - Added build-service install diagnostics that log the configured registry, HTTP status/code, and failing URL with credentials/query values redacted.
  - Added backend tests for generated `package.json`/lockfile contents, `npm ci` install-command selection, and redacted registry diagnostics.
- **Resolution notes:** Use `NPM_CONFIG_REGISTRY=<approved mirror> npm ci` or an uncommitted `.npmrc` registry line in networks where the public registry returns 403.

## 2026-05-18T08:18:00Z — TypeScript Node shim coverage incomplete for SQLite persistence

- **Status:** Complete — added the missing dependency-free Node declarations and adjusted migration path resolution.
- **Context:** Durable SQLite persistence introduced synchronous filesystem, OS temp-directory, child-process, and AES-GCM crypto usage in a repository that intentionally uses local `node-shims.d.ts` instead of `@types/node`.
- **Observed details:** `npm --prefix backend run build` failed after adding persistence code.
- **Relevant logs:**
  - `src/persistence/sqlitePersistence.ts(4,10): error TS2305: Module '"node:child_process"' has no exported member 'execFileSync'.`
  - `src/persistence/sqlitePersistence.ts(12,64): error TS2339: Property 'url' does not exist on type 'ImportMeta'.`
  - `src/persistence/sqlitePersistence.ts(105,21): error TS2339: Property 'randomBytes' does not exist...`
- **Troubleshooting steps taken:**
  - Added local declarations for `node:fs`, `node:os`, `execFileSync`, `console.warn`, `Buffer.from(..., encoding)`, and AES-GCM crypto helpers.
  - Replaced `import.meta.url` migration resolution with a `process.cwd()`-based lookup that works from repo root and `backend/` cwd.
  - Reran `npm --prefix backend run build` and `npm --prefix backend test` successfully.
- **Resolution notes:** Complete; no dependency installation was needed.

## 2026-05-18T10:12:40Z — Debug manifest overrides conflicted with release baseline manifest

- **Status:** Complete — added `tools:replace` for debug-only application attributes.
- **Context:** Android release packaging polish changed the main manifest to disable backup and cleartext traffic while the debug manifest intentionally enables them for local development.
- **Observed details:** Manifest processing failed because `allowBackup`, `fullBackupContent`, `networkSecurityConfig`, and `usesCleartextTraffic` were defined in both source sets with different values.
- **Relevant logs:**
  - `Attribute application@allowBackup value=(true) ... is also present ... value=(false).`
  - `Suggestion: add 'tools:replace="android:allowBackup"' to <application> element...`
- **Troubleshooting steps taken:** Added `xmlns:tools` and `tools:replace="android:allowBackup,android:fullBackupContent,android:networkSecurityConfig,android:usesCleartextTraffic"` to the debug manifest.
- **Resolution notes:** Complete; rerun manifest processing to verify the debug overlay and release baseline merge correctly.
- **Repeat occurrence 2026-05-19T04:25:08Z:** Pre-session unresolved issue review repeated the Android SDK preflight before follow-up release packaging fixes.
  - Command: `./scripts/android-sdk-preflight.sh`
  - Result: `Android SDK preflight failed: ANDROID_HOME is not set and no sdk.dir was found in local.properties.`
  - Status: Incomplete — environment limitation remains; SDK-backed assemble verification must run in an SDK-enabled environment.

## 2026-05-19T04:28:00Z — Binary launcher icon assets made PR review difficult

- **Status:** Complete — replaced launcher binary PNGs with text-based XML drawables in mipmap density folders.
- **Context:** Follow-up requested making previously committed binary icon files non-binary while preserving Android launcher behavior.
- **Troubleshooting steps taken:** Replaced `mipmap-*` PNGs with `ic_launcher.xml`/`ic_launcher_round.xml` files, updated verification script to validate XML icon assets, and kept adaptive icon resources unchanged.
- **Resolution notes:** Complete; repository launcher assets are now text files for easier PR review and diff handling.

## 2026-05-19T04:32:00Z — Android dependency repositories returned 403 during Gradle manifest processing

- **Status:** Incomplete — environment limitation; network proxy/repository access blocks Android dependency resolution.
- **Context:** Follow-up validation after replacing binary launcher icons with XML assets attempted to rerun manifest processing tasks.
- **Observed details:** Gradle failed while resolving AndroidX/Material dependencies from both Google Maven and Maven Central.
- **Relevant logs:**
  - `Could not GET 'https://dl.google.com/dl/android/maven2/...'. Received status code 403 from server: Forbidden`
  - `Could not GET 'https://repo.maven.apache.org/maven2/...'. Received status code 403 from server: Forbidden`
- **Troubleshooting steps taken:** Confirmed icon/manifest verification script still passes locally; retained this as an environment network constraint.
- **Resolution notes:** Re-run Gradle checks in an environment with approved access/mirror for Google Maven and Maven Central.
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
