# Reference repo snippets

This file records the upstream snippets and legally reusable reference material that BotBlade carries directly in-repo. Treat it as the intake ledger for JGit, Fossify, Acode, and hackerkid/bots. Implementation code belongs in BotBlade-native modules, adapters, tests, templates, and generated docs.

BotBlade is GPL-3.0. GPL-compatible sources can be incorporated when attribution and license obligations are preserved. Permissive sources can be incorporated when notices are preserved. CC0 material can be copied, transformed, and expanded freely. Attribution is retained here for audit clarity even when the upstream license does not require it.

## Source ledger

| Source | Repo | License signal | Safe direct-use lane | BotBlade use |
| --- | --- | --- | --- | --- |
| JGit | https://github.com/eclipse-jgit/jgit | Eclipse Distribution License v1.0 in `LICENSE` | Notice text, adapter documentation, dependency boundary notes, independently written integration code | Native Git operations through a Forge Sync adapter |
| Fossify Gallery / Fossify apps | https://github.com/FossifyOrg/Gallery and https://github.com/FossifyOrg | GPL-3.0 in `LICENSE` | GPL-compatible ideas and selectively reusable source when provenance is tracked | Privacy-first Android utility flows, settings, file surfaces, theme behavior |
| Acode | https://github.com/Acode-Foundation/Acode | MIT in `package.json` | Metadata snippets, UX reference notes, independently named editor capabilities, source only with MIT notice preservation | Mobile code editor capability planning |
| hackerkid/bots | https://github.com/hackerkid/bots | CC0-1.0 in `LICENSE` and README license section | Catalog categories, links, framework names, import seeds | Bot resource/import catalog and Blade Pack discovery |

## Direct upstream snippets retained for audit

### JGit license-obligation seed

JGit's license allows redistribution and use in source and binary forms, with or without modification, when upstream copyright notices, condition lists, and disclaimers are retained. BotBlade should keep JGit behind a dependency or adapter boundary and record any JGit-origin bundled material in this file or a future `NOTICE` file.

BotBlade implementation rule: use JGit APIs through BotBlade-authored Kotlin/Java adapter code named around Forge Sync. Preserve JGit notices for bundled source or binary artifacts.

### Acode package metadata seed

Acode's package metadata provides a useful Android code-editor reference profile:

```json
{
  "name": "com.foxdebug.acode",
  "displayName": "Acode",
  "description": "Acode is a code editor for android",
  "license": "MIT"
}
```

BotBlade transformation rule: carry the Android-first editor intent into BotBlade as Forge Editor capabilities: project file tree, tabs, open/save, syntax highlighting, command palette, project search, diagnostics, preview panes, terminal/log panel, and bot-aware templates. Keep BotBlade names, UI copy, icons, layouts, and component structure unique.

### Fossify utility-pattern seed

Fossify's GPL-compatible Android utility model fits BotBlade's local-first direction. The reusable pattern is the product behavior, not the Fossify visual identity:

```text
local-first storage
clear permission explanations
fast settings screens
dark/light theme controls
accent color controls
simple file operations
backup and restore
ad-free, tracker-free defaults
readable Android-native surfaces
```

BotBlade transformation rule: convert these patterns into Forge Files and Forge Settings. Use BotBlade workspace language, project audit logging, encrypted secrets integration, and the existing Android architecture.

### hackerkid/bots catalog category seed

The hackerkid/bots README uses the following high-value catalog structure under CC0:

```text
Platform Documentation
Tools For Building Bots
Tools For Bot Analytics
Tools For Bot Conversation Mockups
Libraries
Bot Stores
Tutorials
Communities
Developers
Testing
```

BotBlade transformation rule: convert this into a BotBlade import catalog with project-oriented buckets:

```text
Platforms
Frameworks
Runtime adapters
Templates
Testing harnesses
Analytics connectors
Deployment targets
Learning links
Community links
```

### hackerkid/bots platform seed links

The CC0 catalog includes these platform targets that map cleanly to BotBlade Blade Packs:

```text
Slack — https://api.slack.com/bot-users
Discord — https://blog.discordapp.com/the-robot-revolution-has-unofficially-begun/
Telegram — https://core.telegram.org/bots/api
Zulip — https://zulip.com/integrations/
Microsoft Bot Framework — https://dev.botframework.com/
Hangouts Chat — https://developers.google.com/hangouts/chat/concepts/
```

BotBlade transformation rule: store these as seed references for import detection and docs, then update stale links during implementation. Each platform gets a BotBlade-native detector, manifest entry, required-secret schema, local test strategy, and deployment notes.

### hackerkid/bots Discord library seed links

The CC0 catalog includes these Discord library references:

```text
Eris — https://github.com/abalabahaha/eris
Discord.js — https://github.com/hydrabolt/discord.js
Discordie — https://github.com/qeled/discordie
Discord.io — https://github.com/izy521/discord.io
Concord — https://github.com/Cogmasters/concord
Discord.Net — https://github.com/RogueException/Discord.Net
DSharpPlus — https://github.com/NaamloosDT/DSharpPlus
Serenity — https://github.com/zeyla/serenity
Discordia — https://github.com/SinisterRectus/Discordia
Discordgo — https://github.com/bwmarrin/discordgo
```

BotBlade transformation rule: prefer current, maintained libraries during implementation. Keep legacy entries as detector clues for imported projects, especially older bots that still depend on archived or renamed libraries.

## Implementation bundle requirements

### Forge Sync from JGit

Add a `ForgeSync` boundary around JGit-powered operations:

```text
cloneRepository(url, targetWorkspace, credentialsRef)
openRepository(workspace)
status(workspace)
stage(paths)
unstage(paths)
commit(title, body)
fetch(remote)
pull(remote, branch)
push(remote, branch)
listBranches()
checkoutBranch(name)
showHistory(range)
showDiff(pathOrCommit)
```

Every operation writes an audit event with operation name, workspace id, redacted remote, branch/ref, result, duration, and error code.

### Forge Files from Fossify-style Android utilities

Add local-first utility surfaces around BotBlade's workspace model:

```text
workspace browser
import from folder
import from zip
export project archive
restore project archive
rename/move/delete project files
permission explainer screens
theme and density settings
accessible storage path display
```

These features should use BotBlade's existing Android architecture and visual language.

### Forge Editor from Acode-style mobile editing

Add editor tasks in this order:

```text
open file from project tree
save file
recent files
tabs
syntax highlighting
project search
find and replace
command palette
inline diagnostics
Markdown/manifest preview
terminal/log drawer
```

The first working slice should support `package.json`, `.env.example`, Markdown, JavaScript, TypeScript, JSON, YAML, shell scripts, Kotlin, Java, Python, and Rust.

### Bot Catalog from hackerkid/bots

Add a seed catalog to backend code so detectors and import screens can use it without scraping upstream repos at runtime. Initial buckets:

```text
Discord
Telegram
Slack
Matrix
Reddit
Webhook
RSS
Local task automation
Node.js bot frameworks
Python bot frameworks
Rust bot frameworks
Testing tools
Deployment targets
```

Each catalog entry should support:

```text
id
label
category
homepage
docsUrl
repoUrl
languageHints
packageHints
entrypointHints
secretHints
importDetectorHints
status
licenseNotes
```

## Copyright-safe engineering rule

Use upstream projects as intake references. Copy only material whose license allows direct reuse, then record the source, license, and transformation in this file or a future `NOTICE` file. For product code, write BotBlade-native modules with BotBlade names, data models, UI structure, strings, and tests. Keep copied snippets short, auditable, and easy to remove.
