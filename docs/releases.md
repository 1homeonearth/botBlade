# BotBlade Releases

This project publishes Android debug APKs from GitHub Actions.

## Channels

- Branch pushes run the Android build check.
- Merges to `main` publish a new debug prerelease.
- Manual dispatch can run the debug release workflow from GitHub Actions.

## Debug release versioning

Debug releases use tags like `v0.001`, `v0.002`, and `v0.003`.

The first debug release is `v0.001`. Each later debug release increments by one thousandth.

The workflow finds the latest `v0.xxx` tag, increments it, builds the APK with the matching `VERSION_SEQ`, and publishes the next prerelease.

## CI release assets

- `botBlade-ci-v<version>.apk`
- `SHA256SUMS.txt`

The release page body contains the changelog. No separate `CHANGELOG.md`, `release.json`, or `INSTALL.md` asset is published.

## Release notes

Each debug release body lists commits since the previous debug tag.

## If release fails

1. Open the failed workflow run.
2. Inspect failing job logs.
3. Fix the build or release issue.
4. Re-run the debug release workflow.
