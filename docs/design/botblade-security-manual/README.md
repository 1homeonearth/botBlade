# botBlade Security Design Manual

## Purpose
This manual is the implementation-groundwork contract for botBlade's
security architecture. Every agent working on repository import,
archive extraction, manifests, build plans, runtime profiles, terminal
sessions, external integrations, secrets, sandboxing, upstream
dependencies, Rust crates, deployment security, or module/plugin
systems must read this file before making changes.

## Scope
Covers: Rust security core, policy/sandbox model, terminal and external
integrations, module/upstream governance, and phased implementation roadmap.

## Current state (as of this writing)
- Design manual: complete (this document set)
- Rust workspace: NOT YET IMPLEMENTED
- Repo safety scanner: NOT YET IMPLEMENTED
- Security policy gates: NOT YET IMPLEMENTED
- Terminal backend sessions: NOT YET IMPLEMENTED
- External integration registry: NOT YET IMPLEMENTED
- Module/plugin system: NOT YET IMPLEMENTED
- Android UI surfacing for security features: NOT YET IMPLEMENTED

Do not claim any of the above as implemented. Only add "implemented" next
to an item when the code actually exists.

## Non-negotiable security rules

1. Repository analysis MUST NEVER execute code. Static inspection only.
2. Safety checks are preflight, cached, and policy-gated. Runtime hot
   paths must not rescan the full repository while a bot is running.
3. Secrets are stored as references with metadata. Secret VALUES are
   never returned in API responses, logs, audit events, screenshots,
   docs, tests, or fixtures.
4. Every terminal path is scoped, gated, audited, and sanitized.
5. External integrations use intents, deep links, OAuth, SAF, and
   browser/custom tabs. No bundled proprietary SDKs without explicit
   review and justification.
6. No GPL-licensed code is vendored into the source tree. Ever.
7. No upstream code is vendored without an upstreams.yml entry, license
   review, and attribution comment.
8. Rust is the preferred language for hostile-input parsing, path
   normalization, archive validation, manifest validation, command-plan
   validation, checksum/signature helpers, and policy evaluation.
9. Fail closed: when a safety decision cannot be made, default to blocked.
10. Existing Discord bot behavior must keep working throughout all changes.

## Manual map
- 01-rust-security-core.md — Rust workspace, crates, CLI contract, cache model
- 02-security-policy-and-sandboxing.md — Gates, policy model, sandbox defaults, secrets
- 03-terminal-and-external-integrations.md — Terminal modes, sanitizer, intent safety
- 04-modules-upstream-governance.md — Module manifest, trust levels, upstream governance
- 05-implementation-roadmap.md — Phased delivery plan

## How future agents use this manual
- Read README.md before any work in the scope areas listed above.
- Keep this manual updated when behavior changes.
- Add tests for every security-gate, terminal, archive, repo-import,
  or secret-handling change.
- Do not mark a phase complete in the roadmap unless the code exists.
