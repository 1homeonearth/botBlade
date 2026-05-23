# Upstream Reference Repos

BotBlade can keep local upstream snapshots for reference work when the original license allows it and the source is recorded.

The snapshots are stored under `vendor/upstream/` and are imported from zip archives with `scripts/import-reference-zips.mjs`.

## Repositories

### Acode

Source: https://github.com/Acode-Foundation/Acode

License: MIT.

BotBlade use: mobile code editor reference, plugin model reference, language tooling reference, and preview workflow reference.

Import target: `vendor/upstream/acode/`.

### hackerkid/bots

Source: https://github.com/hackerkid/bots

License: CC0-1.0.

BotBlade use: bot platform taxonomy, framework catalog seed, testing categories, analytics categories, community resource categories, and developer resource categories.

Import target: `vendor/upstream/hackerkid-bots/`.

### JGit

Source: https://github.com/eclipse-jgit/jgit

License: Eclipse Distribution License v1.0 / BSD-3-Clause style.

BotBlade use: Forge Sync Git adapter reference, clone/status/diff/branch/commit behavior reference, SSH transport reference, HTTP transport reference, and Git test fixture reference.

Import target: `vendor/upstream/jgit/`.

Prefer a dependency-based integration for production code. Keep the snapshot as reference material.

### Fossify File Manager

Source: https://github.com/FossifyOrg/File-Manager

License: GPL-3.0.

BotBlade use: Android-native file manager reference, privacy-first UX reference, storage access reference, file operation reference, and theme/settings reference.

Import target: `vendor/upstream/fossify-file-manager/`.

BotBlade is GPL-3.0, so GPL-3.0 material can be reviewed for direct reuse when provenance stays intact. Prefer BotBlade-native implementation for app identity and UX.

## Import policy

Do not paste untracked upstream files into app or backend production paths. Import full upstream snapshots only under `vendor/upstream/`.

Production code must use BotBlade-native package names, UI copy, data models, service boundaries, and tests.

When a file is copied or ported into production paths, add a note in this document or a future `NOTICE` file explaining the source, license, and transformation.
