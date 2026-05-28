# BotBlade

<p align="center">
  <img src="docs/app-icon.svg" alt="BotBlade app icon" width="180" />
</p>

<p align="center">
  <a href="../../actions/workflows/android.yml"><img alt="Android CI" src="../../actions/workflows/android.yml/badge.svg" /></a>
  <a href="../../releases/latest"><img alt="Latest Release" src="https://img.shields.io/github/v/release/1homeonearth/botBlade?display_name=release" /></a>
  <a href="../../releases/latest"><img alt="Download APK" src="https://img.shields.io/badge/Android-Download%20CI%20APK-2ea44f" /></a>
</p>

BotBlade is a mobile-first Android app plus local backend for importing, creating, editing, building, and operating bot and automation projects.

> Debug APK releases are published automatically after successful merges to `main`. Versioning starts at `0.001` and increments by thousandths.

## What you can do today

- Build and manage projects from Android: Dashboard, Projects, Editor, Deployments, and Settings.
- Connect to a local backend API with token-based protected routes.
- Generate TypeScript Discord bot projects and edit files directly in-app.
- Scan imported workspaces with Blade Packs for Discord.js, Telegraf, Slack Bolt, generic Node, generic Python, n8n workflow JSON, and Botpress-style projects.
- Use native Git groundwork powered by JGit for repository-aware project workflows.
- Run project validation, local builds, runtime start/stop/restart, and deployment actions.
- Store projects, encrypted secrets, build/deploy job history, and audit logs in SQLite.

## Integration direction

BotBlade favors lightweight adapters, templates, detectors, and importers over copying large upstream projects into the app. Planned/reference integrations include Squircle CE or Sora-style native editing, JGit, xterm.js, Activepieces, Bun, Eruda, Node-RED, Huginn, Microsoft Bot Framework, Fossify File Manager, VS Code-style command/extension concepts, n8n workflow import, Botpress bot-as-code projects, and first-class Discord/Telegram/Slack bot packs.

## Download and verify (end users)

1. Open the latest release: [Latest Release](../../releases/latest)
2. Download the latest `botBlade-ci-v*.apk` asset from the release page.
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
BOTBLADE_SECRET_KEY=$(openssl rand -hex 32) PORT=8000 HOST=127.0.0.1 npm start
```

Health check:

```bash
curl http://localhost:8000/api/health
```

For LAN testing from a physical device, bind to all interfaces explicitly:

```bash
BOTBLADE_SECRET_KEY=$(openssl rand -hex 32) PORT=8000 HOST=0.0.0.0 npm start
```

## Project docs

- Installation: [INSTALL.md](INSTALL.md)
- Release flow and assets: [docs/releases.md](docs/releases.md)
- Android packaging/signing: [docs/android-release.md](docs/android-release.md)
- Local development: [docs/local-development.md](docs/local-development.md)
- API overview: [docs/api.md](docs/api.md)
- Import and Blade Pack architecture: [docs/design/import-and-blade-pack-architecture.md](docs/design/import-and-blade-pack-architecture.md)

## Status

BotBlade is production-usable for local workflows and still evolving for hardened cloud deployment and long-running mobile-hosted reliability.
