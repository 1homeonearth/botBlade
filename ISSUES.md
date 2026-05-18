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
