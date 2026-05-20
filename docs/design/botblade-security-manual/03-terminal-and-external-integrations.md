# 03 — Terminal and External Integrations

## Terminal architecture

Terminal support is allowed, but shells are high-risk and must be policy-gated.

### Terminal planes

1. **Preview plane**
   - Render command plan and environment diff before execution.
   - Redact secrets and sensitive paths by default.
2. **Execution plane**
   - Execute only policy-approved command plans.
   - Enforce sandbox profile and runtime capability boundaries.
3. **Audit plane**
   - Record structured, redacted execution metadata and decisions.

### Shell gating rules

- Terminal session start requires gate approval.
- Each command plan is validated before run.
- High-risk command classes require explicit elevated policy.
- Session inheritance of capabilities is disallowed unless policy-authorized.

## Termux integration strategy

Termux is treated as an external app integration, not a vendored subsystem.

Requirements:
- Command preview is explicit before any handoff.
- Intent-based handoff is optional, explicit, and policy-controlled.
- No full-app vendoring of `termux/termux-app`.
- Any dependency candidate from Termux components requires license/provenance review and tests.

## OpenPGP / OpenKeychain strategy

OpenPGP support should use provider APIs/intents where available.

Requirements:
- Use `open-keychain/openpgp-api` style provider compatibility for cryptographic operations when appropriate.
- Treat OpenKeychain app as external integration; do not vendor full app internals.
- Gate key operations via policy (who/what/where), with redacted audit trails.

## Commercial/external app integrations

Integrations (cloud storage, password managers, messaging) must use:
- explicit user consent surfaces
- intent/deep-link/share flows or platform-approved APIs
- bounded data export/import scopes
- policy gate checks before handoff

## Discord compatibility

Existing Discord behavior is a compatibility baseline and must continue working while universal workload support expands.
