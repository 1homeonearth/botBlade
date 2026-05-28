# Import and Blade Pack Architecture

## 1) Executive summary
BotBlade is evolving from a Discord-only local builder into a mobile-first forge for bots and automations. The product identity is now: **import, understand, repair, run, and ship** projects from Android with a lightweight native workflow and a local backend.

First-class loop: import repo/workflow → detect project type → map files/runtime → generate safe local metadata (`botblade.json`) → request missing secrets → install/build/validate → explain failures in plain language → run or prepare deployment → keep project state understandable via logs, Git status, health signals, and repair cards.

BotBlade should borrow carefully. It should use small adapters, native service facades, templates, importers, and Blade Packs instead of vendoring large upstream applications. Direct dependencies must be narrow, justified, and license-reviewed.

## 2) Architecture overview
- **Android app shell**: navigation, dashboard cards, import wizard, editor/logs/status surfaces.
- **Local backend**: scanning, orchestration, runtime commands, dependency tools, diagnostics, and local API.
- **Workspace layer**: project dirs, `botblade.json`, `.env.local`, detector cache, logs, artifacts.
- **Blade Pack layer**: framework knowledge modules (detectors, secrets, commands, diagnostics, templates).
- **Detection layer**: weighted evidence from manifests/imports/files/env/workflow shape.
- **Editor/Git layer**: native editing and trusted source-control operations.
- **Diagnostics layer**: Crash Explainer + Panic Glow Logs + repair actions + health signals.
- **Offline/cache layer**: cached detector results, logs, metadata, Git status/remotes, queued ops.

## 3) Required integration tracks

### Direct dependency candidates
- **JGit**: Android-native Git foundation. Use it for clone, status, branch inspection, diff summaries, commit, pull, and push. Start read-mostly; put mutating operations behind explicit confirmation and credential/token-audit plumbing.
- **Squircle CE / Sora-style editor components**: preferred native editor direction. Use Android-native editing before Monaco/WebView to avoid mobile keyboard, startup, touch, and memory headaches.
- **xterm.js**: future terminal/log console surface for process output and shell-like workflows. Use after the editor/import foundation is stable.
- **Eruda**: optional WebView debugging overlay for later browser-backed tooling or embedded previews.

### Import/reference candidates
- **n8n**: first-class workflow JSON import target. Treat as import/reference-first because of licensing. Detect workflows, render graphs, list credential references, and avoid embedding the runtime.
- **Activepieces**: modern workflow/integration-piece architecture reference. Borrow the concept of small declarative pieces with input/output contracts.
- **Botpress**: bot-as-code import/template target. Import metadata/templates without copying upstream runtime code.
- **Node-RED**: compatibility/reference target for legacy flow concepts.
- **Huginn**: compatibility/reference target for agent/event concepts; do not treat as the modern primary agent architecture.
- **Microsoft Bot Framework**: later enterprise-oriented Blade Pack/template target.
- **Fossify File Manager**: Android-native file-management UX reference for project tree, open-with, copy/move/delete, and storage ergonomics.
- **VS Code-style concepts**: command palette, task definitions, extension manifest shape, problems panel, source-control view, and extension-like pack UX.
- **Bun**: optional experimental runtime lane for compatible JavaScript projects. Keep Node 22 as the default JavaScript runtime until Bun support proves stable on Android.

## 4) Blade Pack specification
A Blade Pack is a small versioned module that answers: recognition, secrets, lifecycle commands, important files, common errors, template generation, imports, and surfaced panels/docs.

Schema fields: `id`, `name`, `version`, `license`, `runtime`, `detectors`, `templates`, `commands`, `secrets`, `diagnostics`, `panels`, `docs`, `supportedImports`.

Discord example shape follows this schema and is implemented in backend seed packs.

n8n pack is **import-only**: detect workflow JSON, extract credentials references, node/edge metadata, unsupported node warnings, and conversion prep for future BotBlade flow blocks. It is not an embedded n8n runtime.

## 5) `botblade.json` specification
`botblade.json` is BotBlade-local metadata that complements native project manifests (`package.json`, `pyproject.toml`, workflow JSON, framework config). It stores detector results and operational metadata without secret values.

Stored fields (schema `1.0.0`): `schemaVersion`, `generatedBy`, `generatedAt`, `project`, `runtime.detectedLanguages`, `runtime.detectedFrameworks`, `runtime.packageManager`, `bladePack.selected`, `bladePack.detected[]` (including `matchedEvidence`), `commandPlan` (`install/build/test/validate/start/stop/restart/deploy`), `secrets.required[]`, `secrets.optional[]` (configured flags only), `permissions`, `capabilities`, `importantFiles`, `warnings`, `repairCards`, and `git` (`branch/status/remotes`).

Never persist secret values in `botblade.json`. The writer must create the workspace directory before writing metadata so a fresh scan cannot fail with `ENOENT`.

## 6) Import wizard UX
Steps and copy:
1. **What are we forging?** (“GitHub repo”, “Local folder”, “Workflow JSON”)
2. **Bringing it into BotBlade** (clone/copy/import status)
3. **Reading the project** (scan files/deps/commands/frameworks)
4. **Detect framework** (“This looks like a Discord.js bot” / “Unknown project” fallback)
5. **Choose the Blade Pack**
6. **Add the keys this bot needs** (redacted local storage)
7. **Install what the project needs** (offline warning when needed)
8. **First check** (build/validate + plain-language diagnostics)
9. **Ready to forge** (editor/logs/git/secrets/project map)

## 7) Weighted detection engine
Thresholds: `0-39 weak`, `40-59 possible`, `60-79 likely`, `80-100 high`.

Initial scoring examples implemented for Discord.js, Telegraf, Slack Bolt, generic Node, generic Python, n8n workflow JSON, Botpress, plus unknown fallback.

n8n workflow type detection must match normal JSON such as `"type": "n8n-nodes-base.webhook"`. Avoid over-escaped regex patterns that require a literal backslash before the dot.

When multiple packs score high, top match is recommended; manual override remains supported. Both recommended and all scored matches are stored in `botblade.json`.

## 8) Native editor plan
Phase 1 direction: Android-native editor foundation (Squircle CE/Sora-style inspiration), not Monaco-first WebView. Required capabilities: tree/tabs/highlight/search/problems/project map/recent/large-file and binary handling/line links from logs/keyboard and scrolling ergonomics.

## 9) Git plan
Use JGit-compatible architecture: clone/status/diff/commit/pull/push/branches/conflict warnings/progress/credential flow/offline queueing and pre-commit diff. Git status feeds Health Score + Project Map.

Implementation order:
1. Read-only status and branch inspection.
2. Clone into a managed workspace.
3. Diff summary and file-level change listing.
4. Commit with preview.
5. Pull/push with explicit credential prompts and token-use audit.

## 10) Runtime plan
Lanes: Node 22 (primary), Python (serious second), Bun (opt-in experimental). Runtime decision checks availability, package manager, dependency state, entrypoint, secrets readiness, network needs, resource expectations, known commands.

## 11) Secrets and audit plan
Generate `.env.local` where appropriate, never commit it, redact logs/token-like strings, show missing-secret repair cards, and track “What’s Using My Token” metadata (name/project/command/process/injection/last-use/domain-when-known).

## 12) Crash Explainer
Rule-based first, pack-extensible, concise card model:
- Title (human diagnosis)
- Evidence (matched line)
- Fix
- Open action (source/settings/secrets/install)

Starter rules include MODULE_NOT_FOUND, Python module missing, invalid Discord token, Discord intents, Telegram auth, Slack signing secret, EADDRINUSE, install failures, TS compile failures.

## 13) Panic Glow Logs
Semantic severity colors from theme tokens; categories: trace/debug/info/success/warning/error/crash/auth/network/dependency/security. Features: stack links, repeated error grouping, filters, redacted copy/share report, raw logs preserved, Crash Explainer cards above log stream.

## 14) Bot Health Score
0–100 signals (defined in Phase 1, polished UI in Phase 2):
- +20 secrets configured
- +20 last build passed
- +15 tests passed
- +15 runtime healthy
- +10 no recent crash
- +10 Git clean/committed
- +10 dependencies current enough
Unknown signals remain neutral on first import.

## 15) Workflow import plan
n8n JSON import first-class: parse metadata/nodes/edges/triggers/credential refs/unsupported nodes/code nodes/webhooks/AI nodes; Phase 3 read-only graph; later conversion pipeline.

Activepieces influences future integration architecture (small declarative pieces, input/output contracts, runtime separation). Node-RED/Huginn stay compatibility references only.

## 16) Template plan
Wave 1 templates: Discord Slash/GPT/Gemini, Telegram Command/AI, Slack Slash/AI, Generic Webhook, Scheduled Worker, Python Telegram/Discord, Botpress bot-as-code.

Each template should define purpose, required secrets, runtime, commands, generated files, first-run success condition, and common diagnostics.

Microsoft Bot Framework belongs in a later enterprise-oriented wave after Discord, Telegram, Slack, Python, n8n, and Botpress import paths are stable.

## 17) Offline-first model
Cache: last scan/scores/selected pack/build/run/logs/git state/queued ops/dependency cache awareness/secret presence/workflow metadata.
States: `ready`, `needs network`, `queued`, `running`, `failed`, `completed`.

## 18) Blade Pack registry
GitHub-hosted registry layout:
- `registry.json`
- `packs/<id>/pack.json`

Registry entries include versions, path, hash, compat ranges, display metadata. Validate hashes before install/update; cache last-known-good registry for offline.

## 19) Historical ecosystem seed: `hackerkid/bots`
Use as CC0 taxonomy seed only (platform families, language pack families, QA/backlog categories, docs categories). Treat as “taxonomy fossil”: no direct runtime dependency, no direct architecture anchoring, no freshness assumptions for linked projects, independent license checks for each external project.

## 20) Sandbox mode (future)
Reserve architecture for simulated Discord/Telegram/Slack/webhook/AI interactions, outbound preview, anti-spam protection, rate-limit/cost estimates, and integration with logs/Crash Explainer/token audit/future Bot QA.

## 21) Phased roadmap
- **Phase 1**: native editor foundation, JGit status/clone basics, Blade Pack schema, `botblade.json`, detectors (Discord/Telegram/Slack/Node/Python/n8n), secrets setup, Crash Explainer, Panic Glow logs foundation.
- **Phase 2**: AI templates, Health Score UI, Project Map UI, JGit diff/commit preview, import polish, Python support expansion.
- **Phase 3**: n8n graph renderer, Activepieces-style integration model, visual flow foundation, sandbox mode.
- **Phase 4**: xterm.js console, Bun experiment, Eruda overlay, Botpress importer/templates, Microsoft Bot Framework pack.

Tensions resolved: Health/Map signals defined in Phase 1 but polished UI in Phase 2; n8n detection in Phase 1 with graph rendering in Phase 3; Python expectations documented in Phase 1 with broader support in Phase 2; Bun experimental in Phase 4.

## 22) Risk register
Key risks + mitigations: license provenance checks, mobile resource constraints, editor perf pitfalls, secret leakage, dependency-install failures, offline queue complexity, workflow incompatibility, Android storage permissions, false-positive detection, registry integrity, template drift, large repo scan cost, Git credential misuse, and accidental upstream code copying.

## 23) Implementation starter tasks
Implemented slice:
- canonical design doc (this file)
- Blade Pack schema + seed packs
- weighted detector + ranked output + confidence
- `botblade.json` writer
- scan entry point (`POST /api/projects/:projectId/scan`)
- starter Crash Explainer rule table
- detector unit tests with fixtures

JGit starter slice:
- Add JGit as a narrow Android dependency.
- Add a native service facade for clone/status/branch/snapshot operations.
- Defer write operations until credential, audit, and confirmation UI are ready.

Deferred follow-ups: import wizard UI polish, Project Map UI, secrets repair cards UI, Panic Glow log consumption of rules, native editor spike, JGit diff/commit UI, xterm.js console spike.

## 24) Phase 4 import implementation checklist
- [ ] Backend detector coverage includes Discord.js, Telegraf, Slack Bolt, n8n workflow JSON, Botpress-style projects, generic Node, and generic Python.
- [ ] `POST /api/projects/:projectId/scan` and import flows return structured data sufficient for Android to render the detected profile, missing secrets, repair cards, command plans, and Git status.
- [ ] `botblade.json` conforms to `backend/src/models/botProfile.ts` and never stores or serializes secret values.
- [ ] Import sources cover Git repository URL, ZIP archive, gated folder/local path support, workflow JSON, and first-party templates.
- [ ] Repair cards are generated for broken or incomplete imports.
- [ ] Tests cover detector scoring, metadata writing, archive policy blocking, secret redaction metadata, and Android parsing/view-model state transitions.
- [ ] Phase 4 explicitly excludes Codex task APIs, a Codex screen, Codex PR automation, and Codex trigger behavior.
