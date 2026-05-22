# BotBlade Releases

This project publishes Android artifacts from GitHub Actions.

## Channels

- **Pull requests:** run CI build checks and verify debug + unsigned release APK outputs.
- **`main` branch pushes:** publish a new debug APK prerelease.
- **Manual dispatch:** can run the debug release workflow from GitHub Actions when needed.

## Debug release versioning

Debug releases use the tag format `v0.xxx` and start at `v0.001`.

Each successful debug release increments by one thousandth:

- `v0.001`
- `v0.002`
- `v0.003`

The workflow finds the latest existing `v0.xxx` tag, increments it, builds the APK with the matching `VERSION_SEQ`, and publishes the next prerelease.

## Standard debug release assets

- `bot-blade-debug.apk` — stable download name for the newest debug APK.
- `bot-blade-VERSION-debug.apk` — versioned debug APK filename.
- `SHA256SUMS.txt` — checksums for APK assets.
- `CHANGELOG.md` — per-release changelog generated from commits since the previous `v0.xxx` tag.
- `release.json` — machine-readable release metadata.
- `INSTALL.md` — install note for the debug APK.

## Release notes policy

Each debug release includes its own `CHANGELOG.md` covering commits since the previous debug tag. The GitHub release body also uses that changelog.

After major release-flow changes, sync release-facing docs when relevant:

- `README.md`
- `INSTALL.md`
- `docs/releases.md`

## If release fails

1. Open the failed workflow run.
2. Inspect failing job logs and asset staging output.
3. Correct version, build, or GitHub release issues.
4. Re-run the debug release workflow from GitHub Actions.
