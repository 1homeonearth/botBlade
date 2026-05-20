# botBlade Terminal Security (Public Summary)

botBlade terminal features are planned to be policy-gated and mode-scoped.

## Planned modes

- logs-only (default)
- backend shell (restricted)
- workload shell (reviewed contexts)
- external Termux handoff (optional, explicit)

## Core safety expectations

- explicit command preview for high-risk flows
- no secret attachment by default
- sanitization of dangerous terminal escape/control sequences
- audited session lifecycle with expiration and redacted logs

See full design details in:
- `docs/design/botblade-security-manual/03-terminal-and-external-integrations.md`
