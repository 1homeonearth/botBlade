# 04 — Native Modules and Upstream Governance

## Native module system plan

botBlade will support controlled native modules/plugins across these classes:
- Language modules
- Platform modules
- Deployment modules
- Terminal modules
- Secure integration modules

Each module type must declare:
- capabilities requested
- runtime surface touched
- data classes accessed
- required policy gates
- rollback/uninstall behavior

## Module lifecycle controls

For install, enable, update, and removal:
- validate manifests with Rust core
- validate checksums/signatures where supported
- evaluate policy compatibility
- run sandbox capability diff review
- run regression tests for impacted gates

Deny activation if module metadata is incomplete or provenance is untrusted.

## Upstream governance policy

Trusted code may be borrowed only when all are satisfied:
1. License reviewed and compatible for intended use.
2. Provenance verified (source origin and ownership).
3. Version/commit pinned.
4. Security and behavior tests added or updated.
5. Attribution recorded.
6. Rollback plan documented.

No full vendoring of upstream app code without explicit governance approval.

## `upstreams.yml` as source of truth

`docs/design/botblade-security-manual/upstreams.yml` tracks:
- upstream identity and classification
- integration mode (reference/dependency/external app/platform feature)
- licensing notes and restrictions
- review status and required follow-ups

Any upstream-related change must update `upstreams.yml` and tests.

## Rust crate governance

Security-critical validation should prefer Rust crates with:
- actively maintained releases
- clear licensing
- reproducible dependency graph
- pinned versions and update cadence

Crate additions affecting security decisions require policy and regression tests.
