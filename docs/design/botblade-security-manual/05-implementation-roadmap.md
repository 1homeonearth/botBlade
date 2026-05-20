# 05 — Implementation Roadmap

## Phase 0: Documentation groundwork (current)

Deliverables:
- Security design manual files.
- Upstream registry seed (`upstreams.yml`).
- Agent instructions to enforce manual-first edits.

Exit criteria:
- Manual committed and referenced by root `AGENTS.md`.

## Phase 1: Rust validation foundation

Scope:
- Create Rust crates/modules for path normalization, archive inspection, manifest schema checks, and policy evaluation scaffold.
- Define canonical verdict artifact schema.

Checks:
- Unit tests for path traversal, malformed archives, and manifest violations.
- Golden tests for verdict serialization.

## Phase 2: Policy gates and sandbox enforcement

Scope:
- Wire gates before build/runtime/deploy/terminal/module/external-intent actions.
- Implement deny-by-default enforcement and policy versioning.

Checks:
- Gate pass/deny matrix tests.
- Sandbox profile compatibility tests.
- Secret redaction tests across logs and diagnostics.

## Phase 3: Terminal and external integration hardening

Scope:
- Implement command preview/approval flow.
- Add policy-gated Termux intent handoff integration.
- Add OpenPGP provider compatibility paths.

Checks:
- Terminal command-plan validation tests.
- External intent allow/deny tests.
- Redacted terminal audit output tests.

## Phase 4: Module system and upstream controls

Scope:
- Implement module manifest and lifecycle gates.
- Enforce upstream governance automation tied to `upstreams.yml`.

Checks:
- Module install/update/rollback tests.
- Upstream policy compliance checks (license/provenance/pinning/test presence).

## Phase 5: Performance and compatibility stabilization

Scope:
- Enable validation caching and incremental revalidation.
- Verify Discord behavior parity while expanding universal workload features.

Checks:
- Cache hit/miss performance benchmarks.
- Discord regression suite.
- Cross-workload compatibility smoke tests.

## Ongoing definition of done

For security-relevant changes, include:
- design doc updates
- policy/gate tests
- no-secret-exposure validation
- upstream metadata updates when dependencies/integrations change
