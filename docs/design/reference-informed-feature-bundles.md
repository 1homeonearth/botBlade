# Reference-Informed Feature Bundles

This plan translates external references into BotBlade-native implementations. We adopt behavior-level ideas only and do not copy upstream source, branding, layouts, icons, text, or implementation structures.

## Bundle A: Forge Sync (JGit-inspired)
- Native Git workflows for bot-forge projects: clone/open/status/stage/commit/branch/sync/history/diff/conflict.
- Git operations are explicit user actions, cancellable where possible, and always audited.
- Guardrails: malformed `.git`, huge repos, nested repos, binary diffs, and path confinement.
- Integrates with import/repair/build/deploy/rollback flow and health ribbon.

## Bundle B: Utility Shell (Fossify-inspired)
- Android-native local-first settings, file workflows, backup/restore, and permissions explanation surfaces.
- No ads, trackers, or analytics by default.
- BotBlade terminology-first labels: forge, blade, audit, workspace, repair.

## Bundle C: Bot Editor (Acode-inspired)
- Multi-tab mobile editor with file tree, search, find/replace, command palette, diagnostics, and bot-aware actions.
- Plugin-ready action registry and bot templates.
- “Explain Project” + “Repair Project” commands via local backend.

## Bundle D: Import Catalog (hackerkid/bots-inspired)
- Curated bot import catalog and template import surfaces.
- Import sources: Git URL, ZIP, local folder.
- Classifier pipeline for framework/runtime/scripts/secrets/deploy assumptions.

## Immediate implementation order
1. Add this plan.
2. Add architecture notes (`docs/architecture.md`).
3. Track TODOs per bundle in `TASKS.md`.
4. First practical slice:
   - Forge Sync import from URL
   - Local project file tree
   - Basic editor open/save
   - `package.json` classifier
   - Project health summary
5. Wire into Dashboard, Projects, Editor, Logs, Settings, onboarding, secrets, audit/build history.
6. Add tests for import, file tree indexing, classifier, and Git logging.

## First-slice acceptance criteria
- API supports `/forge-sync/import`, `/forge-sync/status`, `/forge-sync/classify`, `/forge-sync/health`.
- Import/classify/status actions generate audit events with redacted metadata.
- File tree and open/save continue to enforce workspace confinement.
- Health summary combines Git, dependency, build, and secrets states.

