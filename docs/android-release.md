# Android Release Packaging

This document is the checklist for producing a production Android package without committing private signing material.

## Build variants and API endpoints

The app has one flavor dimension, `environment`:

| Variant family | API base URL | Intended use |
| --- | --- | --- |
| `localDev*` | `http://10.0.2.2:8000` | Emulator/local backend development. The application id is suffixed with `.localdev`; debug also adds `.debug`. |
| `prod*` | `https://api.royalscepter.app` by default | Production/staging-style release packaging. Override with `-PPROD_API_BASE_URL=https://...` when cutting a package for another hosted backend. |

## Multi-channel packaging/signing strategy

To avoid uninstall-before-install conflicts and signature collisions, BotBlade now uses explicit install channels by package id:

- **Local developer channel**: `prodDebug` / `localDevDebug` (`.debug` suffix)
- **CI distributable channel**: `prodCi` (`.ci` suffix)
- **Store/release channel**: `prodRelease` (no suffix)

Because each channel installs as a different package name, users can keep local debug, CI APK, and release APK side-by-side without overwrite or signature conflict.

Recommended rules:

1. Use CI artifacts (`prodCi`) for external testing.
2. Use local debug builds only for local development.
3. Use `prodRelease` only for signed release-track installs.
4. Do not distribute locally-built debug APKs to testers who already use CI APKs.

The Settings screen can still save a validated backend URL override in app `SharedPreferences`; do not enter credentials, query strings, fragments, or tokens in that field.

Useful commands:

```bash
gradle :app:assembleLocalDevDebug
gradle :app:assembleProdRelease
gradle :app:assembleProdCi
gradle :app:bundleProdRelease
```

Run `./scripts/android-sdk-preflight.sh` first in environments where Android SDK availability is uncertain.

## Version policy

`app/build.gradle.kts` keeps source defaults at `versionCode = 1` and `versionName = "0.1.0"` for the first production package.

For every Play Console upload:

1. Increase `VERSION_CODE` monotonically. Never reuse a value that has been uploaded.
2. Set `VERSION_NAME` with semantic versioning (`MAJOR.MINOR.PATCH`) plus an optional pre-release suffix for non-production tracks.
3. Prefer passing release values at build time so emergency packaging does not require a source edit:

```bash
gradle :app:bundleProdRelease -PVERSION_CODE=2 -PVERSION_NAME=0.1.1
```

Commit source default bumps when the new version becomes the baseline for the next release train.

## Release signing without committed keystores

Never commit `.jks`, `.keystore`, passwords, key aliases, or generated signing reports. Store keystores in a password manager, CI secret store, or another approved private vault.

Generate a local upload key only on a trusted workstation:

```bash
keytool -genkeypair \
  -v \
  -storetype PKCS12 \
  -keystore bot-blade-upload.jks \
  -alias bot-blade-upload \
  -keyalg RSA \
  -keysize 4096 \
  -validity 10000
```

Provide signing values with Gradle properties or environment variables at build time:

```bash
export ROYAL_SCEPTER_RELEASE_STORE_FILE=/secure/path/bot-blade-upload.jks
export ROYAL_SCEPTER_RELEASE_STORE_PASSWORD='***'
export ROYAL_SCEPTER_RELEASE_KEY_ALIAS=bot-blade-upload
export ROYAL_SCEPTER_RELEASE_KEY_PASSWORD='***'

gradle :app:bundleProdRelease -PVERSION_CODE=2 -PVERSION_NAME=0.1.1
```

The Gradle release signing config is only attached when all four values are present. If they are omitted, Gradle can still build an unsigned release artifact for local inspection, but that artifact is not suitable for store upload.

## Launcher icons and adaptive icons

Production launcher assets live in `app/src/main/res`:

- Adaptive icons: `mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml`.
- Adaptive foreground: `drawable/ic_launcher_foreground.xml`.
- Adaptive background color: `@color/ic_launcher_background`.
- Legacy density icon XML wrappers: `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi`, and `mipmap-xxxhdpi`, each containing `ic_launcher.xml` and `ic_launcher_round.xml` that point at the shared vector foreground.

Verify icon density coverage and release/debug manifest policy with:

```bash
./scripts/verify-android-release-assets.sh
```

## Backup, cleartext traffic, and network security

| Build source set | Backup | Cleartext | Network security config |
| --- | --- | --- | --- |
| `main` / release baseline | Disabled with `android:allowBackup="false"` and `android:fullBackupContent="false"`. | Disabled with `android:usesCleartextTraffic="false"`. | `@xml/network_security_config` has `cleartextTrafficPermitted="false"`. |
| `debug` overlay | Enabled for local troubleshooting and emulator data migration. | Enabled for local backend URLs. | `@xml/debug_network_security_config` permits cleartext for `10.0.2.2`, `127.0.0.1`, and `localhost`. |

Production backend URLs must use HTTPS. Keep local HTTP usage in debug/local development only.

## Packaging checklist

1. Confirm the privacy policy and data-safety notes are current.
2. Run `./scripts/verify-android-release-assets.sh`.
3. Run `./scripts/android-sdk-preflight.sh` in the packaging environment.
4. Build `prodRelease` with explicit `VERSION_CODE`, `VERSION_NAME`, `PROD_API_BASE_URL`, and release signing values.
5. Install or upload only the signed production artifact.
6. Archive the final version values, commit SHA, signing key alias, and Play track in the release notes.
