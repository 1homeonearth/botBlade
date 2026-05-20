# 05 — Implementation Roadmap

## Phase 0: Manual + AGENTS + upstream tracking

Scope:
- Land the security design manual.
- Update `AGENTS.md` with mandatory manual usage guidance.
- Seed `upstreams.yml` with candidate/reference integrations and license notes.

Exit criteria:
- Manual files exist and are linked.
- AGENTS instructions are explicit for security/runtime/import/terminal domains.
- `upstreams.yml` exists with initial entries.

## Phase 1: Rust workspace skeleton

Scope:
- Add Rust workspace layout and initial crates:
  - `botblade-core`
  - `botblade-security`
  - `botblade-cli`
- Define shared types for policy verdicts and report artifacts.

Checks:
- Workspace/build sanity checks.
- Basic unit tests for serialization contracts.

## Phase 2: Repo safety scanner

Scope:
- Implement non-executing repository and archive scanner.
- Add hostile-input parsing, path normalization, and archive validation primitives.
- Emit structured safety findings and confidence levels.

Checks:
- Path traversal tests.
- Archive abuse tests (symlink escape, decompression bomb guardrails, malformed headers).
- Scanner deterministic output tests.

## Phase 3: Policy gates and cached decisions

Scope:
- Introduce preflight policy gates for build/run/deploy/terminal/external handoff.
- Implement cache key model (content hash + policy version + profile fingerprint).
- Enforce deny-by-default when evidence is stale or incomplete.

Checks:
- Gate allow/deny matrix tests.
- Cache hit/miss/invalidation tests.
- Secret redaction and policy-evidence tests.

## Phase 4: Terminal skeleton

Scope:
- Implement terminal modes and approval flow:
  - logs-only
  - backend shell
  - workload shell
  - external Termux mode
- Add terminal command sanitizer/validator pipeline.

Checks:
- Terminal mode gating tests.
- Command-plan validation tests.
- Audit trail redaction tests.

## Phase 5: External app integrations

Scope:
- Add safe external integration adapters (OpenPGP provider, browser/OAuth flows, intent/deep link handoff).
- Enforce explicit user confirmation and scoped data sharing.

Checks:
- Intent allowlist/blocklist tests.
- Consent-required flow tests.
- External handoff policy gate tests.

## Phase 6: Module/plugin system

Scope:
- Implement module manifests, capability declarations, provenance checks, and update policy rules.
- Wire module lifecycle events into policy and audit pipelines.

Checks:
- Module install/enable/update/remove gate tests.
- Signature/checksum verification tests.
- Upstream governance policy tests tied to `upstreams.yml`.

## Phase 7: Android UI surfacing

Scope:
- Surface security decisions, manifest status, gate failures, and remediation hints in Android UX.
- Expose terminal/runtime/deploy controls only when policy permits.

Checks:
- UI integration tests for security state rendering.
- Regression tests to preserve Discord-specific behavior while universal workload support expands.

## Ongoing definition of done

For any security-relevant change:
- update the manual sections in the same change
- add/adjust tests for affected gates
- verify secret redaction expectations
- update `upstreams.yml` if upstream posture changed
