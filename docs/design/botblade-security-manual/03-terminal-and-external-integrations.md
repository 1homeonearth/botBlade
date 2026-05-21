# 03 — Terminal, External Integrations, and Intent Safety

## Purpose

Enable terminal and external integration capability without bypassing policy controls.

Terminal and intent surfaces are powerful and must remain scoped, auditable, sanitized, and secret-safe by default.

## Core principle

Terminal sessions are policy-gated and secret-empty by default.
External integrations are optional, explicit, previewed, and permission-bounded.

## Terminal modes

1. `logs_only`
   - default mode
   - streams logs only
   - no command input
2. `backend_shell`
   - controlled shell on backend host
   - trusted workspace + confirmation required
   - no secrets attached by default
3. `workload_shell`
   - attach to approved running workload/container
   - requires fresh safety/policy decisions
   - production requires extra confirmation
4. `external_termux`
   - optional intent handoff to installed Termux
   - exact command preview required
   - no secrets unless explicit user-chosen secret refs

## Terminal backend API (planned)

- `POST /api/terminal/sessions`
- `GET /api/terminal/sessions/:sessionId`
- `POST /api/terminal/sessions/:sessionId/input`
- `GET /api/terminal/sessions/:sessionId/output`
- `POST /api/terminal/sessions/:sessionId/resize`
- `POST /api/terminal/sessions/:sessionId/close`

Session fields:
- id
- workspaceId
- repositoryId
- workloadId
- environmentId
- runtimeProfileId
- mode
- workingDirectory
- allowedCommands
- attachedSecretRefs
- networkPolicy
- createdAt
- expiresAt
- status
- safetyReportId
- policyDecisionId
- auditEventIds

## Terminal sanitizer requirements

- preserve useful ANSI color output
- neutralize OSC 52 clipboard writes by default
- detect abusive control sequences
- block output-driven navigation/intent triggering
- enforce scrollback limits
- redact known and token-shaped secrets
- raw mode only behind explicit advanced setting

## Termux integration rules

- Termux support is optional.
- Do not vendor entire `termux/termux-app`.
- Use explicit preview + user confirmation before handoff.
- Verify package/signature where feasible for sensitive flows.
- Prefer safe handoff files/exports rather than broad path exposure.
- botBlade must work without Termux installed.

## OpenPGP/OpenKeychain integration

- Treat OpenKeychain as optional external provider.
- Do not vendor OpenKeychain app internals.
- Prefer provider API compatibility (`openpgp-api`) after license/provenance review.
- Keep private keys outside botBlade.
- Persist only provider metadata and user choice.

## External integration architecture

Use a registry model with fields such as:
- id
- displayName
- packageName
- integrationType
- supportedActions
- requiredSignatureDigests
- installed
- verified
- optional
- warning

Rules:
- explicit user action required
- sensitive payloads require preview
- raw secrets blocked by default
- use content URIs (not raw filesystem paths)
- narrow URI grants and revoke when possible
- chooser for generic sharing, explicit package targeting only for verified integrations
- every external send produces audit event

## Commercial integration candidates

- GitHub app/browser/deep links/OAuth
- 1Password / Bitwarden / Proton Pass (Autofill or explicit handoff)
- Google Drive / Dropbox / OneDrive via SAF/share
- Slack / Discord / Telegram via intent/deep-link/share
- Browser/custom tabs for docs, OAuth, and dashboards

## Export bundle rules

Supported bundle types (planned):
- diagnostic bundle
- redacted logs
- manifest
- import report
- safety report
- build plan
- runtime profile
- deployment profile
- upstream dependency report

Bundle requirements:
- no secret values
- include checksums/provenance/timestamps
- include safety report ID + manifest hash
- support optional OpenPGP signing/encryption later through external provider

## Android screens (planned)

Terminal screen:
- mode, directory, environment, safety state
- input enable/disable reason
- redacted output
- copy redacted output
- close/clear/resize controls

External integrations screen:
- detected integrations
- verification state
- supported actions
- required permissions
- last used
- audit history
- disable/reset controls

## Tests

- unsafe repo cannot open shell
- logs-only works without secrets
- backend shell requires policy approval
- workload shell requires reviewed runtime profile
- production shell requires confirmation
- Termux preview required
- secrets not attached by default
- OSC 52 neutralized
- scrollback limit enforced
- directory traversal rejected
- sessions expire
