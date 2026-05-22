# BotBlade

<p align="center">
  <img src="docs/app-icon.svg" alt="BotBlade app icon" width="180" />
</p>

<p align="center">
  <a href="../../actions/workflows/android.yml"><img alt="Android CI" src="../../actions/workflows/android.yml/badge.svg" /></a>
  <a href="../../releases/latest"><img alt="Latest Release" src="https://img.shields.io/github/v/release/1homeonearth/botBlade?display_name=release" /></a>
  <a href="../../releases/latest/download/bot-blade-debug.apk"><img alt="Download APK" src="https://img.shields.io/badge/Android-Download%20Debug%20APK-2ea44f" /></a>
</p>

BotBlade is a mobile-first Android app plus local backend for creating, editing, building, and operating Discord bot projects.

> Debug APK releases are published automatically after successful merges to `main`. Versioning starts at `0.001` and increments by thousandths.

## What you can do today

- Build and manage projects from Android: Dashboard, Projects, Editor, Deployments, and Settings.
- Connect to a local backend API with token-based protected routes.
- Generate TypeScript Discord bot projects and edit files directly in-app.
- Run project validation, local builds, runtime start/stop/restart, and deployment actions.
- Store projects, encrypted secrets, build/deploy job history, and audit logs in SQLite.

## Download and verify (end users)

1. Open the latest release: [Latest Release](../../releases/latest)
2. Download `bot-blade-debug.apk`
3. Verify checksum:

```bash
sha256sum -c SHA256SUMS.txt
```

## Quick start (developers)

### Android app

```bash
gradle :app:assembleDebug
```

### Backend

```bash
cd backend
npm install
npm run preflight:node
npm run build
BOTBLADE_SECRET_KEY=$(openssl rand -hex 32) npm start
```

Health check:

```bash
curl http://localhost:8000/api/health
```

## Project docs

- Installation: [INSTALL.md](INSTALL.md)
- Release flow and assets: [docs/releases.md](docs/releases.md)
- Android packaging/signing: [docs/android-release.md](docs/android-release.md)
- Local development: [docs/local-development.md](docs/local-development.md)
- API overview: [docs/api.md](docs/api.md)

## Status

BotBlade is production-usable for local workflows and still evolving for hardened cloud deployment and long-running mobile-hosted reliability.