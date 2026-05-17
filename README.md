# royalScepter

royalScepter is an Android control console for building, configuring, validating, deploying, starting, stopping, restarting, and monitoring Discord bots from an Android device.

## Android app

The Android app uses Kotlin, native Android views, Material Components, and the package namespace `com.princess.royalscepter`.

Current scaffold screens:

- Dashboard: shows `Bot Status: Unknown` by default and can query local compatibility endpoints.
- Code Editor: placeholder screen for future project-aware bot editing.
- Settings: placeholder screen for future local API settings and secret references.

Local compatibility endpoints preserved for the Dashboard:

- `GET /api/bot-status/`
- `POST /api/bot-toggle/`

For Android emulator debug builds, local HTTP access is allowed for `10.0.2.2`, `127.0.0.1`, and `localhost` through a debug-only network security configuration.

## Build

From the repository root:

```bash
gradle :app:assembleDebug
```

Install the generated debug APK on an emulator or device after the build completes. The default Dashboard API base URL is `http://10.0.2.2:8000`, which maps to the host machine from the Android emulator.
