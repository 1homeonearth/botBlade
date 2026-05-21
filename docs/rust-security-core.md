# botBlade Rust Security Core (Public Summary)

This page summarizes the security-core direction documented in `docs/design/botblade-security-manual/`.

## What it is

Rust is planned as botBlade's hardened validation engine for hostile inputs, path/archive safety, policy checks, and deterministic security outputs.

## What it is not

This is not a full rewrite of Android or backend layers. Kotlin and Node/TypeScript remain core product layers.

## Key guarantees

- No code execution during repository analysis.
- Preflight security decisions before risky actions.
- Cache-backed runtime decisions to keep active workloads fast.
- Secret references and redaction-first handling.

## Source of truth

For detailed architecture and roadmap, see:
- `docs/design/botblade-security-manual/README.md`
- `docs/design/botblade-security-manual/01-rust-security-core.md`
- `docs/design/botblade-security-manual/02-security-policy-and-sandboxing.md`
