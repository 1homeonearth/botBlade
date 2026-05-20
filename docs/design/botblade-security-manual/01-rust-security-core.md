# 01 — Rust Security Core

## Purpose and trust model

- **[Planned]** Rust is the security core that owns hostile-input parsing and security verdicts before build/runtime/deploy execution.
- **[Implemented]** Kotlin Android and Node/TypeScript remain product/orchestration layers.
- **[Implemented]** Downstream layers must treat security verdict artifacts as authoritative when present.

## Security-critical ownership (Rust)

The following controls are Rust-owned by design:

1. **[Planned]** Hostile-input parsing and strict decoding.
2. **[Planned]** Path normalization and traversal prevention.
3. **[Planned]** Archive validation without extraction side effects.
4. **[Planned]** Manifest schema + policy validation.
5. **[Planned]** Command/build-plan policy validation.
6. **[Planned]** Repository safety/risk scanning.
7. **[Planned]** Checksum/signature primitives.
8. **[Planned]** Shared policy evaluation engine used by all gates.

## Gate definitions (core)

- `import_analysis_gate` — blocks execution while repository/archive input is still untrusted.
- `manifest_integrity_gate` — requires schema validity + policy compatibility.
- `build_plan_gate` — validates command plans against allow/deny constraints.
- `runtime_profile_gate` — validates runtime profile capabilities and provenance.

All core gates are **deny-by-default** when required evidence is missing.

## Validation lifecycle

1. **Import phase** (data only, no execution).
2. **Validation phase** (Rust checks + policy evaluation).
3. **Verdict artifact phase** (hash-linked, redaction-safe output).
4. **Runtime enablement** (only if gates pass).

## Verdict artifact minimum schema

- input identity (content hash + source metadata)
- policy/profile version
- verdict (`allow` | `deny` | `review`)
- reasons + redaction-safe evidence
- timestamp + toolchain version

## Test requirements

- Unit tests: path traversal, malformed archive headers, manifest violations.
- Golden tests: deterministic verdict serialization.
- Regression tests: deny-by-default on missing/malformed evidence.

## Performance and compatibility

- **[Planned]** Cache by `content hash + policy version + environment profile`.
- **[Implemented]** Preserve Discord behavior while universal workload support expands.
