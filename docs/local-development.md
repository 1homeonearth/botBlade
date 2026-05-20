# Local Development

## Backend on port 8000

Node.js 22+ is required for backend commands.


Start the backend:

```bash
cd backend
npm install
npm run preflight:node
npm run build
PORT=8000 npm start
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

## npm proxy warning mitigation

If npm prints `Unknown env config "http-proxy"`, your shell or CI is exporting a deprecated npm env key. Prefer standard proxy env vars (`HTTP_PROXY`, `HTTPS_PROXY`, `NO_PROXY`) or npm config keys written with underscores (`npm_config_http_proxy`, `npm_config_https_proxy`) to stay compatible with newer npm versions.
