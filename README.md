# royalScepter

royalScepter is an Android control console for building, configuring, validating, deploying, starting, stopping, restarting, and monitoring Discord bots from an Android device.

## Android app

The Android app uses Kotlin, native Android views, Material Components, and the package namespace `com.princess.royalscepter`. Jetpack Compose is intentionally not used in this scaffold phase.

Current app shell tabs:

- Dashboard: shows `Bot Status: Unknown` by default, displays the locally stored active project ID when one exists, and queries local compatibility endpoints through a repository/API layer.
- Projects: empty state for future bot project creation.
- Editor: placeholder screen for future project-aware bot editing.
- Deployments: empty state for future project deployments; the action button is disabled until an active project exists.
- Settings: placeholder screen for future local API settings and secret references.

Local compatibility endpoints preserved for the Dashboard:

- `GET /api/bot-status/`
- `POST /api/bot-toggle/` with JSON body `{"action":"start"}` or `{"action":"stop"}`

The centralized Android API base URL currently lives in `ApiConfig.DEFAULT_BASE_URL` and defaults to `http://127.0.0.1:8000`.

Future API models anticipate these project-scoped endpoints:

- `GET /api/health`
- `GET /api/version`
- `GET /api/projects`
- `POST /api/projects`
- `GET /api/projects/{projectId}`
- `PATCH /api/projects/{projectId}`
- `POST /api/projects/{projectId}/validate`
- `POST /api/projects/{projectId}/builds`
- `POST /api/projects/{projectId}/deployments`
- `GET /api/projects/{projectId}/runtime/status`
- `POST /api/projects/{projectId}/runtime/start`
- `POST /api/projects/{projectId}/runtime/stop`
- `POST /api/projects/{projectId}/runtime/restart`

For Android emulator debug builds, local HTTP access is allowed for `10.0.2.2`, `127.0.0.1`, and `localhost` through a debug-only network security configuration. If your backend runs on the host machine while testing in the Android emulator, update the single base URL setting to `http://10.0.2.2:8000`.

## Build

From the repository root:

```bash
./gradlew :app:assembleDebug
```

If your environment provides a system Gradle install instead of the wrapper, use:

```bash
gradle :app:assembleDebug
```

Install the generated debug APK on an emulator or device after the build completes. No backend or secrets are required to launch the app; the Dashboard will show a clear disconnected-backend message if the local API is not running.
