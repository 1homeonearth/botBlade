# botBlade Security Design Manual

This manual defines the architecture and guardrails for botBlade's Rust-centered security framework, terminal controls, external integrations, module system, and upstream governance.

## Scope and intent

This manual is design-level groundwork for phased implementation. It does **not** claim that every control described here is already implemented.

Primary goals:
- Establish Rust as the security core for hostile-input and policy validation.
- Preserve Kotlin Android and Node/TypeScript backend as product/app layers.
- Keep active bots fast by validating early, caching verdicts, and avoiding unsafe runtime behavior.
- Support universal workloads without regressing existing Discord behavior.

## Document map

1. [01-rust-security-core.md](./01-rust-security-core.md)
   - Security core responsibilities, trust boundaries, validation pipeline.
2. [02-security-policy-and-sandboxing.md](./02-security-policy-and-sandboxing.md)
   - Policy model, gates, profiles, and enforcement lifecycle.
3. [03-terminal-and-external-integrations.md](./03-terminal-and-external-integrations.md)
   - Terminal architecture, shell gating, Termux and OpenPGP integration model.
4. [04-modules-upstream-governance.md](./04-modules-upstream-governance.md)
   - Native module model and upstream code/dependency governance.
5. [05-implementation-roadmap.md](./05-implementation-roadmap.md)
   - Sequenced implementation plan, milestones, and test expectations.
6. [upstreams.yml](./upstreams.yml)
   - Registry for approved, candidate, and reference upstream components.

## Non-negotiable principles

- No repository code execution during import analysis.
- No raw secret values in logs, docs, tests, screenshots, fixtures, or reports.
- No implicit privileged operations; all sensitive actions require explicit policy.
- No full-app vendoring of GPL applications (for example Termux app and OpenKeychain app).
- No upstream vendoring without license review, provenance, pinning, tests, and attribution.

## Maintenance contract

When behavior changes in repo import, archive handling, manifests, build plans, runtime profiles, terminal sessions, external integrations, secret handling, sandboxing, upstream dependencies, Rust crates, deployment security, or native modules/plugins:
- Update this manual in the same change set.
- Add or update security-focused tests.
- Ensure `upstreams.yml` remains consistent with integration and vendoring decisions.
