# Roadmap

## Phase 0: Public coherence
- README identity/status cleanup.
- Correct GitHub Release download links.
- Move session/process docs under `docs/project/`.
- Add backend CI workflow.

## Phase 1: Durable local product
- Replace sqlite3 CLI persistence with a library-backed adapter.
- Add restart-survival persistence tests.
- Add production startup checks for auth + secret-key safety.
- Add integration tests proving protected-route auth enforcement.

## Phase 2: Bot import intelligence (Codespaces-for-bots foundation)
- Implement static import scanner + detector registry (no code execution).
- Produce Bot Profile output (bot type, runtime, entrypoint, secrets checklist).
- Add confidence scoring and unknown-bot fallback to generic app profile.
- Add setup checklist UI backed by detector output.

## Phase 3: Runtime profiles and isolation modes
- Add per-project run mode model: Local, Sandboxed Local, Remote, Read-only Inspect.
- Wire start/stop/restart flows to runtime profile selection.
- Add capability prompts and project-level policy visibility.

## Phase 4: Mobile IDE and Git UX uplift
- Command palette actions for common bot tasks.
- File search, outlines, and logs/terminal drawer ergonomics.
- Git branch/divergence status, staged hunks, and guided conflict handling.

## Phase 5: Repair Import + incident guidance
- Classify startup/build/install failures into known issue families.
- Offer safe, previewable one-tap repair suggestions.
- Add incident cards with root-cause hints and next-step actions.

## Phase 6: Lock-in-free templates and catalog
- Add curated template flows (Discord/Telegram/Slack/webhook/etc.).
- Add import catalog metadata for known public bot repos.
- Preserve lock-in-free behavior: everything remains normal git projects.

## Phase 7: Production posture
- External identity provider integration.
- Vault-backed secret management.
- Containerized build workers for untrusted code isolation.
- Signed release verification guide and operator runbooks.
