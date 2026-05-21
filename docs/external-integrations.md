# botBlade External Integrations (Public Summary)

botBlade external integrations are optional and explicit.

## Integration style

Use intents, deep links, OAuth/browser flows, provider APIs, Android SAF/share flows, and explicit user confirmation.

## Security posture

- raw secrets blocked by default
- payload preview required for sensitive sends
- content URIs preferred over raw filesystem paths
- signature verification for sensitive direct integrations when feasible
- every external send should be auditable

## Examples

- GitHub (app/browser/auth and workflow links)
- password managers via Autofill/explicit handoff
- cloud storage via SAF/share
- OpenPGP provider integrations through external provider boundaries

See full design details in:
- `docs/design/botblade-security-manual/03-terminal-and-external-integrations.md`
- `docs/design/botblade-security-manual/04-modules-upstream-governance.md`
