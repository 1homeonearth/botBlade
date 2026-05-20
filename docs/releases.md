# BotBlade Releases

This project publishes Android artifacts from GitHub Actions (`.github/workflows/android.yml`).

## Channels

- **`main` branch pushes:** update rolling prerelease channel (`latest`).
- **`v*` tags:** publish immutable versioned releases.
- **Pull requests:** produce downloadable CI artifacts (debug + unsigned release APKs).

## Standard release assets

- `bot-blade.apk` (primary user APK)
- `bot-blade-debug.apk`
- `bot-blade-vVERSION-VERSIONCODE-signed.apk`
- `bot-blade-vVERSION-VERSIONCODE-debug.apk`
- `SHA256SUMS.txt`
- `release.json`
- `INSTALL.md`

## Release notes policy

Each release should include notes covering:

1. End-user changes (UI, onboarding, behavior)
2. Developer changes (build, backend, tooling)
3. Known limitations and migration notes

After every release, sync release-facing docs when relevant:

- `README.md`
- `INSTALL.md`
- `docs/releases.md`

## If release fails

1. Open the failed workflow run.
2. Inspect failing job logs and artifact selection output.
3. Correct signing/version/input issues.
4. Re-run with `workflow_dispatch` in the correct mode.
