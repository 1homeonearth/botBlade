# Local Development

## Backend host/port defaults

Node.js 22+ is required for backend commands.


Start the backend:

```bash
cd backend
npm install
npm run preflight:node
npm run build
PORT=8000 HOST=127.0.0.1 npm start
```


Default listener behavior:

- `PORT` defaults to `8000` when unset.
- Host resolves from `BIND_HOST`, then `HOST`, and falls back to `127.0.0.1` for loopback-safe local use.
- For LAN/device access, set `HOST=0.0.0.0` (or a specific LAN IP) explicitly.

LAN example:

```bash
PORT=8000 HOST=0.0.0.0 npm start
```
Health check:

```bash
curl http://localhost:8000/api/health
```

## Android backend URL configuration

The Android client reads its backend URL from `BuildConfig.API_BASE_URL` unless the Settings screen has saved a user override. The Settings value is stored in app `SharedPreferences` as a URL only; credentials, bearer tokens, session tokens, query parameters, and URL fragments are not saved there.

Use the Settings tab to edit the Backend API URL, save it, and tap **Test Connection**. The test calls `/api/health` and reports the health status or connection error.

## Android emulator host note

Android emulators cannot use `127.0.0.1` to reach your host computer. Debug emulator builds default to:

```text
http://10.0.2.2:8000
```

If you need to re-enter it in Settings, use exactly that URL while the backend is running on your host at port `8000`. Debug builds allow cleartext HTTP for local development.

## Real-device LAN backend note

For a physical Android device, bind the backend to a host/interface reachable from the same network, then configure the app Backend API URL in Settings. Example:

```text
http://192.168.1.25:8000
```

Replace `192.168.1.25` with your development machine's LAN IP address. Keep this on a trusted development network. The current backend has no production authentication.

## Local compatibility endpoints

The dashboard-compatible endpoints remain available:

- `GET /api/bot-status/`
- `POST /api/bot-toggle/` with `{"action":"start"}` or `{"action":"stop"}`

New features should prefer project-scoped endpoints under `/api/projects/:projectId`.

## In-memory data

Projects, secrets, build jobs, deployment targets, runtimes, and audit events are in memory. Restarting the backend clears them. Generated project files remain on disk under `backend/generated-projects/` until deleted manually.

## Android manifest check helper

Use `scripts/run-manifest-check.sh` to run Android manifest-processing tasks with Gradle wrapper safety checks.

- **Default mode (self-healing):** `./scripts/run-manifest-check.sh`
  - If `./gradlew` or `gradle/wrapper/gradle-wrapper.jar` is missing, the script regenerates wrapper files via system `gradle` and then continues.
- **Check-only mode:** `./scripts/run-manifest-check.sh --check-only` (or `CHECK_ONLY=1 ./scripts/run-manifest-check.sh`)
  - Wrapper regeneration is disabled.
  - If wrapper files are missing, the script exits with remediation guidance instead of changing files.

## npm proxy warning mitigation

If npm prints `Unknown env config "http-proxy"`, your shell or CI is exporting a deprecated npm env key. Prefer standard proxy env vars (`HTTP_PROXY`, `HTTPS_PROXY`, `NO_PROXY`) or npm config keys written with underscores (`npm_config_http_proxy`, `npm_config_https_proxy`) to stay compatible with newer npm versions.


## ZIP import validator runtime

The current ZIP import security gate uses a Python 3 bridge for archive preflight validation and controlled extraction.

- If `python3` is unavailable, `POST /api/imports/zip` fails closed with `blocked_by_policy` and a structured `ZIP_RUNTIME_UNAVAILABLE` violation.
- To enable ZIP imports in local/dev environments, ensure `python3` is installed and on `PATH`.

