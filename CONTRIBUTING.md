# Contributing

Thanks for helping improve **botBlade** / **BotBlade**.

## Repository layout

- `app/`: Android app (`com.princess.botblade`, display name BotBlade).
- `backend/`: Node/TypeScript backend (`botblade-backend`).
- `docs/`: project documentation.
- `docs/project/`: agent/session continuity notes and process history.

`AGENTS.md` remains at the repository root for agent tooling instruction discovery.

## Build and test

### Backend

```bash
npm --prefix backend install
npm --prefix backend run typecheck
npm --prefix backend run build
npm --prefix backend test
```

### Android

If Android SDK is available:

```bash
gradle :app:assembleDebug
gradle :app:testLocalDevDebugUnitTest
```

If SDK setup is missing, follow `docs/android-sdk-setup.md`.

## Documentation expectations

- Keep public-facing project docs in `docs/`.
- Keep session continuity notes in `docs/project/`.
- Update `README.md` and `ROADMAP.md` when product scope or priorities materially change.
