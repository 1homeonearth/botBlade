# Mobile Bot Workbench Blueprint (BotBlade)

## Goal

Build BotBlade into a phone-first bot IDE that feels like "Codespaces for bots," while preserving security-first import and execution behavior.

## Product pillars

1. **Import anything safely**: Git URL, archive, template, or local folder.
2. **Understand before run**: detector output + human-readable Bot Profile.
3. **Run in clear modes**: Local, Sandboxed Local, Remote, Read-only Inspect.
4. **Mobile-native editing**: tabs, quick actions, palette, logs, terminal drawer.
5. **Git first**: clone/fetch/pull/branch/diff/stage/commit/push with plain-language status.
6. **Visible security posture**: secrets vault, capabilities, policy prompts, audit trail.

## Target surfaces

### 1) Dashboard / project cards
Per-project card metadata:
- bot type
- runtime + package manager
- branch + ahead/behind
- last run + current status
- secrets health
- recent incident summary
- readiness score

### 2) Import flow (deterministic)
1. User selects source (Git/Archive/Template/Folder).
2. Static scanner analyzes repo tree and manifests without executing code.
3. Detector emits candidate bot archetypes + confidence.
4. Build-plan resolver proposes install/start/test commands.
5. Security policy gate computes required capabilities.
6. User confirms setup checklist + secrets placeholders.
7. Runtime profile is provisioned.
8. Health checks run.
9. User starts bot.

### 3) Bot Profile output contract
Bot Profile should include:
- probable platform (Discord/Telegram/etc.)
- language/runtime version suggestion
- package manager + lockfile confidence
- required/optional secret keys
- likely entrypoint + scripts
- permissions/capabilities needed
- startup risks and auto-repair suggestions

## Detection matrix (first-party)
- Dependency signatures (package/pyproject/Cargo)
- Manifest signatures (`botblade.yml`, `docker-compose.yml`, Dockerfile)
- Code heuristics (imports, framework constructors, long-running loop)
- Runtime file signals (Procfile, npm scripts, Poetry scripts, Cargo bins)

## Execution backend strategy

### Run Mode: Local
- Fast path for trusted local testing.

### Run Mode: Sandboxed Local
- Stronger boundary for imported/untrusted repos.
- Capability-based filesystem and network constraints.

### Run Mode: Remote
- Off-device execution lane for heavy builds or long-running bots.

### Run Mode: Read-only Inspect
- Parse, inspect, and validate without runtime execution.

## Repair Import system

When a run/install fails, classify failure and suggest scoped repairs:
- missing dependency install
- runtime version mismatch
- missing `.env` keys
- wrong package manager
- missing migration step
- missing command deploy step

Every repair action is previewed and user-confirmed.

## Security requirements (must stay true)
- Static inspection only during import analysis.
- Secrets are references + encrypted storage; values never exposed in API or logs.
- Fail closed on ambiguous policy outcomes.
- Capability prompts are explicit and auditable.
- No vendored GPL code; upstream intake follows governance process.

## Architecture slices (incremental delivery)

### Slice A — Import Intelligence
- Add detector registry and confidence scoring.
- Produce Bot Profile JSON + UI summary card.

### Slice B — Runtime Profiles
- Add per-project runtime profile model (mode, runtime, policy, cache).
- Wire start/stop actions to selected run mode.

### Slice C — Mobile IDE Ergonomics
- Command palette actions for common bot tasks.
- File search, symbol outline, quick env edit, logs terminal drawers.

### Slice D — Git UX Upgrade
- Branch/divergence status and staging UX.
- Conflict hints + safe-resolve helpers.

### Slice E — Repair and Incident Cards
- Parse logs to known failure classes.
- Generate suggested fixes with one-tap apply preview.

### Slice F — Template and Store lanes
- Curated templates + "import by Git" catalog metadata.
- Keep lock-in-free workflow (always repo-based).

## Definition of done for "Codespaces-for-bots" v1
- User can import a public bot repo from phone.
- Bot Profile identifies runtime, secrets, and start command with confidence score.
- User can fill secrets, run health checks, and start bot in chosen run mode.
- User can edit files, commit/push changes, and inspect logs/incidents.
- Security capability prompts and audit records are visible for key actions.
