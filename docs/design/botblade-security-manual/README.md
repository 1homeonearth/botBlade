# botBlade Security Design Manual

## Purpose

This manual is the implementation-groundwork source for botBlade's Rust-centered security architecture, policy-gated runtime, terminal/intent safety model, and upstream/module governance.

It defines what must be true before broad feature rollout. It does not claim all features are already implemented.

## Scope

This manual governs work involving:
- repository import and archive extraction
- manifest/build-plan/runtime-profile handling
- security policy decisions and sandbox controls
- terminal sessions and external integrations
- secret references, redaction, and injection rules
- module/plugin lifecycle and capabilities
- upstream dependency/integration governance
- deployment security behaviors

## Current state (May 20, 2026)

- Manual and governance scaffolding are in place.
- Existing Android + Node/TypeScript product layers remain baseline.
- Existing Discord behavior remains compatibility-critical while universal workload support evolves.

## Non-negotiable security rules

- Rust is the hardened security decision engine.
- Repository analysis must not execute imported code.
- Security checks are preflight and cache-backed; runtime must reuse fresh decisions.
- Secrets are handled as references and metadata, not exposed raw values.
- Path and archive handling are fail-closed.
- Terminal and external intent flows are explicit, gated, audited, and sanitized.
- Native modules must declare capabilities, provenance, update policy, and tests.
- Do not vendor full GPL applications (e.g., Termux app/OpenKeychain app) into botBlade.
- External integrations use explicit user action and platform-safe boundaries.

## Manual map

1. [01-rust-security-core.md](./01-rust-security-core.md)
2. [02-security-policy-and-sandboxing.md](./02-security-policy-and-sandboxing.md)
3. [03-terminal-and-external-integrations.md](./03-terminal-and-external-integrations.md)
4. [04-modules-upstream-governance.md](./04-modules-upstream-governance.md)
5. [05-implementation-roadmap.md](./05-implementation-roadmap.md)
6. [upstreams.yml](./upstreams.yml)

Supplemental public-facing stubs:
- [docs/rust-security-core.md](../../rust-security-core.md)
- [docs/terminal-security.md](../../terminal-security.md)
- [docs/external-integrations.md](../../external-integrations.md)

## How future agents should use this manual

- Read this README before touching covered areas.
- Read the relevant section files before making changes.
- Update manual docs in the same change set when behavior changes.
- Add/adjust tests for gate behavior, import/archive safety, terminal safety, and secret handling changes.
- Keep `upstreams.yml` aligned with integration/vendoring posture.
- Preserve existing Discord functionality as universal workload support is added.
