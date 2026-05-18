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
- **Repeat occurrence 2026-05-18T10:00:14Z:** Pre-session unresolved issue review and Android unit-test verification for URL path encoding changes hit the same missing SDK limitation.
  - Commands: `./scripts/android-sdk-preflight.sh`, `gradle :app:testDebugUnitTest`.
  - Results: preflight reported `ANDROID_HOME is not set and no sdk.dir was found in local.properties`; Gradle reported `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file at '/workspace/royalScepter/local.properties'.`
  - Solution attempt: Re-ran the repository preflight as directed and attempted the Android unit-test task after code changes; no SDK root or approved Android SDK artifact mirror is available in this shell.
  - Status: Incomplete — environment limitation remains; Android Gradle checks must be rerun in an SDK-enabled environment.


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
