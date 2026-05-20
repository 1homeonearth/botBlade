# 04 — Modules and Upstream Governance

## Module system constraints

- **[Planned]** Controlled native modules/plugins for language, platform, deployment, terminal, and secure integrations.
- Module manifests must declare capabilities, touched runtime surfaces, data classes, required gates, and rollback behavior.

## Module lifecycle gates

Required for install/enable/update/remove:
- `module_manifest_gate`
- `module_provenance_gate`
- `module_policy_compat_gate`
- `module_sandbox_diff_gate`
- `module_regression_gate`

Default action: deny activation when metadata is incomplete/untrusted.

## Upstream governance policy

Any borrowed upstream code/dependency requires all of:

1. License compatibility review.
2. Provenance verification (origin + ownership).
3. Version/commit pinning.
4. Security/behavior tests.
5. Attribution record.
6. Rollback plan.

- **[Implemented]** No vendoring of full upstream app code without explicit governance approval.
- **[Implemented]** `upstreams.yml` is mandatory source of provenance and usage-mode truth.

## License/provenance boundaries

- GPL app integrations can be external-app integrations without vendoring app internals.
- Candidate dependencies are blocked until license/provenance status moves to approved.
- Security-sensitive Rust crates must be actively maintained, pinned, and test-covered.

## Test requirements

- Module lifecycle gate tests (install/update/rollback/remove).
- Provenance/license policy tests tied to `upstreams.yml` entries.
- Regression tests for upstream-policy deny paths.
