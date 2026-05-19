# HUMANS.md — Android SDK blocker playbook for this container

## Problem summary
`./scripts/android-sdk-preflight.sh` fails in this container because Android SDK packages are not installed and outbound HTTPS downloads for SDK artifacts are blocked by a proxy (`CONNECT tunnel failed, response 403`).

## What was tried (multiple approaches)
1. **Standard bootstrap script**
   - Command: `./scripts/android-sdk-bootstrap.sh`
   - Result: `curl: (22) ... 403` from `dl.google.com`.

2. **Direct mirror candidates / alternate Google endpoint checks**
   - Commands:
     - `curl -I -L https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip`
     - `curl -I -L https://redirector.gvt1.com/edgedl/android/repository/commandlinetools-linux-13114758_latest.zip`
     - `curl -I -L https://mirrors.cloud.tencent.com/AndroidSDK/android/repository/commandlinetools-linux-13114758_latest.zip`
   - Result for all: `CONNECT tunnel failed, response 403` (proxy-level deny before artifact host).

3. **Check for preinstalled SDK fallback**
   - Command: `test -d /root/android-sdk/platforms/android-35` and `test -d /root/android-sdk/build-tools/35.0.0`
   - Result: both missing.

## Why this repeats
The blocker is not in repository code. It is infrastructure/network policy for this container. Until an approved SDK artifact source is reachable through the proxy, every Android compile/assemble task will fail early.

## Fastest path to green
Use **one** of these:

1. **Preferred:** provide an approved mirror URL that the proxy allows, then run:
   ```bash
   ANDROID_CMDLINE_TOOLS_URL=https://<approved-mirror>/commandlinetools-linux-13114758_latest.zip \
   ./scripts/android-sdk-bootstrap.sh
   ./scripts/android-sdk-preflight.sh
   ```

2. **Alternative:** mount or pre-provision an SDK directory that already contains:
   - `platforms/android-35`
   - `build-tools/35.0.0`
   - `platform-tools`

   Then set either:
   - `export ANDROID_HOME=/path/to/android-sdk`, or
   - untracked `local.properties` with `sdk.dir=/path/to/android-sdk`.

3. **CI-only validation fallback:** rely on GitHub Actions Android workflow (which provisions SDK on runner) for APK build verification when local container networking is restricted.

## Verification commands after fix
```bash
./scripts/android-sdk-preflight.sh
gradle :app:compileLocalDevDebugKotlin --stacktrace
gradle :app:assembleLocalDevDebug --stacktrace
```

## Notes for LLMs / engineers
- Do not keep retrying random mirrors unless proxy policy changed; record attempts in `ISSUES.md` under the existing Android SDK issue chain.
- If this is unresolved at end of session, update `LEFTOVERS.md` with exact mirror/proxy requirement and stop burning cycles.
