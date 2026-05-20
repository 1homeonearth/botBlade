# 03 — Terminal and External Integrations

## Terminal control model

- **[Mixed]** Terminal support exists/expected; full policy-gated command lifecycle is planned hardening work.
- Shell execution is high-risk and must be gate-controlled.

### Terminal planes

1. `terminal_preview_plane` — show command plan + env diff with redaction.
2. `terminal_execution_plane` — run only gate-approved plans in sandbox profile.
3. `terminal_audit_plane` — emit structured, redacted execution audit events.

## Terminal gate definitions

- `terminal_session_start_gate`: required before opening a session.
- `terminal_command_plan_gate`: required for every command plan.
- `terminal_elevation_gate`: required for high-risk command classes.

Constraints:
- deny capability inheritance unless explicitly policy-approved
- reject unsigned or profile-mismatched execution contexts

## External integration policy

### Termux

- **[Implemented]** Treat Termux app as external integration (not vendored app code).
- **[Planned]** Explicit preview + policy gate before any intent handoff.
- **[Implemented]** No full-app vendoring of `termux/termux-app`.

### OpenPGP / OpenKeychain

- **[Planned]** Use provider API compatibility path (e.g., `open-keychain/openpgp-api`) where appropriate.
- **[Implemented]** OpenKeychain app is external integration; no full-app vendoring.
- **[Planned]** Gate key operations by actor/workload/destination profile.

### Commercial/service integrations

Applies to GitHub, password managers, cloud storage, Slack/Discord/Telegram:
- explicit user consent surface
- bounded import/export scope
- policy gate before handoff
- redacted audit trail after handoff

## Test requirements

- Terminal command-plan validation tests.
- Terminal redaction tests for preview/logging/audit.
- External intent allow/deny matrix tests.
- Regression tests preserving existing Discord behavior.
