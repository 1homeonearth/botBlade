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

## Phase 2: Deployment credibility
- Document runtime target model and production boundaries.
- Harden local Docker adapter behavior and failure handling.
- Add remote VPS/SSH adapter design.
- Add deployment health checks.

## Phase 3: Production posture
- External identity provider integration.
- Vault-backed secret management.
- Containerized build workers for untrusted code isolation.
- Signed release verification guide and operator runbooks.
