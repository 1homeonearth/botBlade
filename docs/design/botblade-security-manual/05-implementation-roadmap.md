# 05 — Implementation Roadmap

## Phase 0 — Manual foundation **[Implemented]**

- Manual set created and normalized.
- `upstreams.yml` seeded and tracked.
- Root agent guidance requires manual review/sync.

Exit criteria:
- Manual and AGENTS guardrails merged.

## Phase 1 — Rust validation foundation **[Planned]**

Scope:
- Rust path/archive/manifest/policy primitives.
- Canonical verdict schema + deterministic serialization.

Required tests:
- path traversal, malformed archive, manifest violation, deny-by-default.

## Phase 2 — Policy and sandbox gates **[Planned]**

Scope:
- Enforce gates before build/runtime/deploy/terminal/module/external-intent.
- Implement capability-scoped sandbox contracts.

Required tests:
- gate decision matrix, profile compatibility, redaction coverage.

## Phase 3 — Terminal and external hardening **[Planned]**

Scope:
- command preview/approval pipeline
- policy-gated Termux handoff
- OpenPGP provider compatibility path

Required tests:
- command-plan validation, intent allow/deny, terminal audit redaction.

## Phase 4 — Module and upstream enforcement **[Planned]**

Scope:
- module lifecycle gate automation
- policy checks linked to `upstreams.yml`

Required tests:
- module lifecycle and upstream governance checks.

## Phase 5 — Performance and compatibility **[Planned]**

Scope:
- verdict caching + incremental revalidation
- Discord behavior parity during universal workload expansion

Required tests:
- cache hit/miss benchmarks, Discord regression suite, cross-workload smoke tests.

## Definition of done (security-sensitive changes)

- manual updated in same PR
- gates and redaction tests updated
- `upstreams.yml` updated for upstream/integration changes
- no raw secrets introduced
