# Android releases and APK artifacts

## Trigger behavior
- `pull_request` (`opened`, `synchronize`, `reopened`, `ready_for_review`): builds debug + unsigned release APKs and uploads workflow artifacts.
- `push` to `main`: builds APKs and publishes a prerelease channel (`latest`) with release assets.
- `push` tags matching `v*`: builds APKs and publishes a normal versioned GitHub Release.
- `workflow_dispatch`: supports `build-only`, `prerelease`, and `versioned-release` modes.

## Signing secrets
- `KEYSTORE_BASE64`
- `KEYSTORE_PASSWORD`
- `KEY_ALIAS`

If missing, PR builds still succeed. Versioned release publishing fails before publication if signing is unavailable.

## Public release assets
- `royal-scepter.apk` (stable signed user download)
- `royal-scepter-vVERSION-VERSIONCODE-signed.apk`
- `royal-scepter-debug.apk`
- `royal-scepter-vVERSION-VERSIONCODE-debug.apk`
- `SHA256SUMS.txt`
- `release.json`
- `INSTALL.md`

## PR artifacts
- `royalScepter-...-debug-apk`
- `royalScepter-...-release-unsigned-apk`
- `royalScepter-...-checksums`

Artifacts are available in the workflow run **Artifacts** section.

## Checksum and metadata generation
The workflow selects final release assets explicitly, generates `SHA256SUMS.txt` only from selected assets, validates with `sha256sum -c`, then uploads exactly that list. `release.json` captures version/build metadata and SHA-256 hashes.

## Manual recovery
If a release run fails:
1. Open the failed Actions run and inspect the step summary.
2. Fix signing/version mismatch or missing assets.
3. Re-run with `workflow_dispatch` and the correct mode/tag.
