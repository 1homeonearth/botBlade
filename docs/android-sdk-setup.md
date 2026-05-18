# Android SDK Setup

Use this guide to bootstrap a local Android SDK without checking machine-specific SDK paths into the repository.

## Quick bootstrap

From the repository root, the checked-in bootstrap script can download command-line tools, install this app's required SDK packages, write an untracked `local.properties`, and add `ANDROID_HOME`/`ANDROID_SDK_ROOT` exports to your shell profile:

```bash
./scripts/android-sdk-bootstrap.sh
```

If your network blocks direct Google downloads, provide an approved internal mirror of the command-line tools archive:

```bash
ANDROID_CMDLINE_TOOLS_URL=https://your-mirror/commandlinetools-linux-13114758_latest.zip ./scripts/android-sdk-bootstrap.sh
```

The rest of this document shows the same setup manually.

## 1. Install Android command-line tools

1. Download the Android command-line tools for your operating system from the [Android Studio downloads page](https://developer.android.com/studio#command-tools).
2. Create an SDK directory outside the repository. Examples:

   ```bash
   mkdir -p "$HOME/Android/Sdk/cmdline-tools"
   ```

3. Unzip the command-line tools package and move the extracted `cmdline-tools` contents into a `latest` directory:

   ```bash
   unzip commandlinetools-*.zip -d /tmp/android-commandlinetools
   mkdir -p "$HOME/Android/Sdk/cmdline-tools/latest"
   mv /tmp/android-commandlinetools/cmdline-tools/* "$HOME/Android/Sdk/cmdline-tools/latest/"
   ```

4. Add the command-line tools to your shell path for the current terminal session:

   ```bash
   export ANDROID_HOME="$HOME/Android/Sdk"
   export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
   ```

Persist those exports in your shell profile if you want them available in new terminal sessions.

## 2. Install the required SDK packages

From the repository root, install the Android SDK packages required by the app:

```bash
sdkmanager --licenses
sdkmanager \
  "platforms;android-35" \
  "build-tools;35.0.0" \
  "platform-tools"
```

The app is configured for `compileSdk = 35` and `buildToolsVersion = "35.0.0"`, so both packages must be present before Gradle builds the Android project.

## 3. Set `ANDROID_HOME`

Set `ANDROID_HOME` to the SDK directory you created. Replace the example path with your local SDK path:

```bash
export ANDROID_HOME="/path/to/android-sdk"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
```

Do not commit your local SDK path. It is specific to your machine.

## 4. Optionally create `local.properties`

Android Gradle Plugin also supports an untracked `local.properties` file at the repository root:

```properties
sdk.dir=/path/to/android-sdk
```

This file is intentionally ignored by `.gitignore`; keep it local and do not commit it.

## 5. Run the SDK preflight check

Before compiling the app, run the checked-in preflight script. It reads `ANDROID_HOME`, falls back to `ANDROID_SDK_ROOT`, and can also read `sdk.dir` from the untracked `local.properties` file:

```bash
./scripts/android-sdk-preflight.sh
```

The script fails early with a clear error if either required directory is missing:

- `$ANDROID_HOME/platforms/android-35`
- `$ANDROID_HOME/build-tools/35.0.0`

## 6. Verify the Android build

In an SDK-enabled environment, verify Kotlin compilation first and then assemble the debug APK:

```bash
ANDROID_HOME=/path/to/android-sdk gradle :app:compileDebugKotlin
ANDROID_HOME=/path/to/android-sdk gradle :app:assembleDebug
```
