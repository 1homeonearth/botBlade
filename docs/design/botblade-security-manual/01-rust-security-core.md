# 01 — Rust Security Core

## Purpose

Rust is the security core for botBlade. It owns hostile-input handling and trust decisions before higher-level app/runtime execution.

Kotlin Android and Node/TypeScript remain product layers for UX, orchestration, integrations, and workload delivery.

## Trust boundaries

The Rust security core is the only component allowed to:
- Parse untrusted repo/import payloads for security verdicts.
- Normalize paths and enforce canonical filesystem boundaries.
- Validate archives and manifests against policy.
- Validate command plans and repo safety profiles.
- Evaluate policy rules used by build/runtime/deploy/terminal/integration gates.
- Compute or verify checksums/signatures used by integrity controls.

All other layers consume Rust-produced verdict artifacts and must not bypass them.

## Security-critical Rust responsibilities

Implement in Rust (priority order):
1. Hostile-input parsing and strict decoding.
2. Path normalization and traversal prevention.
3. Archive validation (zip/tar/gzip/etc.) without extraction side effects.
4. Manifest validation (schema + policy constraints).
5. Command-plan validation (allowlist, denylist, argument constraints).
6. Repo safety scanning (dangerous patterns, executable risk signals).
7. Checksum/signature helper primitives.
8. Policy evaluation engine used by all security gates.

## Execution model

1. **Import phase**: inspect untrusted content as data only.
2. **Validation phase**: run Rust checks and policy evaluation.
3. **Verdict cache phase**: persist signed/hashed verdict artifacts.
4. **Runtime enablement**: allow downstream actions only if gate verdicts permit.

No code from imported repositories executes during import or analysis.

## Performance model

- Run expensive validation pre-runtime.
- Cache results by deterministic identity (content hash + policy version + environment profile).
- Revalidate only when content, policy, or relevant environment changes.
- Keep active bots fast by consuming cached verdicts and incremental deltas.

## Data contracts

Every validation result should emit a machine-readable artifact with at least:
- input identity (hashes, source metadata)
- policy version/profile
- verdict (allow/deny/review)
- reasons and redaction-safe evidence
- timestamp and toolchain version

## Compatibility commitments

- Preserve existing Discord behavior while adding universal workload support.
- Support phased rollout where legacy validators remain temporarily, but Rust verdicts are source-of-truth for security-critical decisions.
