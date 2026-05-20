# botBlade Security Design Manual

This directory defines the **security design contract** for botBlade. It translates source design prompts into implementation-grade requirements with explicit state labels, security gates, test obligations, and provenance/license boundaries.

## Status legend

- **[Implemented]**: Present in repository behavior today and expected to remain enforced.
- **[Planned]**: Approved design requirement not yet fully implemented.
- **[Mixed]**: Partially implemented; remaining work is tracked in roadmap.

## Document map

1. [01-rust-security-core.md](./01-rust-security-core.md)
2. [02-security-policy-and-sandboxing.md](./02-security-policy-and-sandboxing.md)
3. [03-terminal-and-external-integrations.md](./03-terminal-and-external-integrations.md)
4. [04-modules-upstream-governance.md](./04-modules-upstream-governance.md)
5. [05-implementation-roadmap.md](./05-implementation-roadmap.md)
6. [upstreams.yml](./upstreams.yml)

## Non-negotiable security boundaries

- **[Implemented]** No raw secret values in docs, logs, tests, screenshots, fixtures, or diagnostics.
- **[Implemented]** No implicit privilege escalation; sensitive actions must be policy-gated.
- **[Implemented]** No full-app vendoring of GPL apps (including Termux app and OpenKeychain app).
- **[Implemented]** No upstream vendoring without license/provenance review, pinning, attribution, and tests.
- **[Planned]** Rust becomes source-of-truth for all security-critical verdicts during phased rollout.

## Synchronization contract

When behavior changes in any of these areas, update this manual in the **same PR**:
repo import, archive extraction, manifests, build plans, runtime profiles, terminal sessions, external integrations, secrets, sandboxing, upstream dependencies, Rust crates, deployment security, modules/plugins.

Also update:
- tests for affected security gates,
- `upstreams.yml` for integration/dependency changes,
- implementation status labels in relevant manual sections.

## Verification checklist (required in future PRs)

- [ ] Manual updated for behavior changes in security-sensitive areas.
- [ ] `docs/design/botblade-security-manual/upstreams.yml` updated (or explicitly marked not impacted).
- [ ] Tests added/updated for security-sensitive changes (gates, archives, paths, terminal handling, redaction, external intents, upstream governance).
