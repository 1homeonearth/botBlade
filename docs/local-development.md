# Local Development

## Backend on port 8000

Start the backend:

```bash
cd backend
npm install
npm run build
PORT=8000 npm start
```

Health check:

```bash
curl http://localhost:8000/api/health
```

## Android emulator host note

Android emulators cannot use `127.0.0.1` to reach your host computer. Use:

```text
http://10.0.2.2:8000
```

Debug builds allow cleartext HTTP for `10.0.2.2`, `127.0.0.1`, and `localhost`.

## Real-device LAN backend note

For a physical Android device, run the backend on a host reachable from the same network and configure the app API base URL to:

```text
http://<host-lan-ip>:8000
```

Keep this on a trusted development network. The current backend has no production authentication.

## Local compatibility endpoints

The dashboard-compatible endpoints remain available:

- `GET /api/bot-status/`
- `POST /api/bot-toggle/` with `{"action":"start"}` or `{"action":"stop"}`

New features should prefer project-scoped endpoints under `/api/projects/:projectId`.

## In-memory data

Projects, secrets, build jobs, deployment targets, runtimes, and audit events are in memory. Restarting the backend clears them. Generated project files remain on disk under `backend/generated-projects/` until deleted manually.
