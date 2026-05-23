# App Updates

BotBlade should support release-aware updates for sideloaded builds.

Android requires explicit user confirmation before installing a sideloaded APK. BotBlade can check for new releases, show the version, open the release page, and open the APK download URL. Silent installation is outside the app's normal permission model.

## Current implementation slice

The UI groundwork branch adds a GitHub release checker and an update panel in Settings.

The checker reads the latest release from:

```text
https://api.github.com/repos/1homeonearth/botBlade/releases/latest
```

The panel can:

- Check for the latest GitHub release.
- Find the first APK release asset.
- Compare the release tag with the installed version name.
- Open the APK or release page in the browser.
- Store whether automatic checks are enabled.
- Throttle automatic checks to avoid repeated network requests.

## Signing rule

APK updates install cleanly only when the package name and signing certificate match the installed app. A CI APK signed with a different debug key can conflict with an already installed package. Production update channels need stable signing.

Recommended channels:

```text
com.princess.botblade.localdev.debug
com.princess.botblade.prod.ci
com.princess.botblade
```

Use the release-signed package for the long-lived update channel. Use CI APKs for preview testing and uninstall older CI builds when Android reports a package conflict.
