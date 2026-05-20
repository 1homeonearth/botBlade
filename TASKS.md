# TASKS.md

Purpose: single deterministic execution plan for future agents so they can continue work without re-parsing prior prompt history.

## Execution Order Legend
- **[P#]** = ordered phase (run in ascending order).
- **[BLOCKS]** = task blocks later phases until complete.
- **[UNBLOCKED]** = can run once listed blockers are cleared.
- **[PARALLEL]** = safe parallel work after prerequisites are satisfied.

## [P0] Session Bootstrap and Guardrails [BLOCKS]
- Confirm repository instructions in `AGENTS.md`.
- Read and clear `LEFTOVERS.md` if present before new work.
- If PR creation is planned, apply CI health gate process from `docs/project/gh-cli-auth.md` when auth is unavailable.

## [P1] Crash Triage Task Group [BLOCKS]
- Collect latest reproducible crash reports and affected surfaces.
- Classify each crash by: security impact, data-loss risk, reproducibility, and owner area.
- Map each crash to one of the security-manual domains for required controls:
  - repo import
  - archive extraction
  - manifests
  - build plans
  - runtime profiles
  - terminal sessions
  - external app integrations
  - secrets
  - sandboxing
  - upstream dependencies
  - Rust crates
  - deployment security
  - native modules/plugins
- Output required before next phase: prioritized crash queue with explicit fix order.

## [P2] Manual Docs Alignment Task Group [BLOCKS]
- Validate implementation intent against:
  - `docs/design/botblade-security-manual/README.md`
  - `docs/design/botblade-security-manual/01-rust-security-core.md`
  - `docs/design/botblade-security-manual/02-security-policy-and-sandboxing.md`
  - `docs/design/botblade-security-manual/03-terminal-and-external-integrations.md`
  - `docs/design/botblade-security-manual/04-modules-upstream-governance.md`
  - `docs/design/botblade-security-manual/05-implementation-roadmap.md`
- For any behavior change in covered areas, queue mandatory manual updates in the same PR.
- Output required before next phase: doc-impact checklist per planned code change.

## [P3] AGENTS Enforcement Task Group [BLOCKS]
- Verify instruction precedence for touched paths:
  - `AGENTS.md`
  - nearest nested `AGENTS.md`
  - `AGENTS.override.md` when present (takes precedence at that directory level)
- Confirm no task begins before actionable leftovers are attempted.
- Enforce focused edits + smallest useful post-edit check.
- Output required before next phase: compliance note listing governing instruction files per changed path.

## [P4] Upstream Governance Task Group [BLOCKS]
- For any vendoring or upstream intake, enforce governance using:
  - `docs/design/botblade-security-manual/upstreams.yml`
  - `docs/design/botblade-security-manual/04-modules-upstream-governance.md`
- Required gates before merge:
  - upstream entry present in `upstreams.yml`
  - license review completed
  - attribution recorded
  - tests added/updated for impacted behavior
- Output required before next phase: upstream governance checklist with pass/fail per gate.

## [P5] Rust Core Planning Task Group [UNBLOCKED after P1-P4]
- Plan security-critical validation work in Rust first.
- Align proposed Rust changes with:
  - `docs/design/botblade-security-manual/01-rust-security-core.md`
  - `docs/design/botblade-security-manual/05-implementation-roadmap.md`
- Identify boundaries with non-Rust components and define interface contracts.

## [P6] Terminal / External Integration Planning [UNBLOCKED after P1-P4]
- Plan hardening tasks for terminal sessions and external integrations using:
  - `docs/design/botblade-security-manual/03-terminal-and-external-integrations.md`
  - `docs/design/botblade-security-manual/02-security-policy-and-sandboxing.md`
- Ensure plans preserve existing Discord behavior while enabling universal workload support.
- Define explicit tests for terminal handling, path resolution, secret redaction, and external intents.

## [P7] Module Governance Planning [UNBLOCKED after P1-P4]
- Plan native module/plugin governance updates guided by:
  - `docs/design/botblade-security-manual/04-modules-upstream-governance.md`
  - `docs/design/botblade-security-manual/upstreams.yml`
- Confirm module onboarding/removal lifecycle, ownership, and security sign-off criteria.

## [P8] Verification Task Group [BLOCKS release/merge]
- Run smallest useful checks relevant to touched code.
- Mandatory verification focus when applicable:
  - security gates
  - archive handling
  - terminal handling
  - path resolution
  - secret redaction
  - external intents
  - upstream dependency changes
- Record command, result, and any environment limits.

## [P9] Deferred Items Register [PARALLEL once active blockers identified]
- Use `LEFTOVERS.md` only for unfinished, actionable handoff items.
- Keep each leftover minimal:
  - blocker
  - relevant files
  - exact next action
- Delete completed leftovers after verification; delete `LEFTOVERS.md` when empty.

## [P10] Required Inputs Gate [BLOCKS execution when missing]
Future agents must obtain these inputs before implementation if not already known:
- Reproduction details for current crash set (steps, logs, expected vs actual).
- Scope decision: hotfix vs roadmap delivery.
- Risk tolerance and release window constraints.
- CI/auth status for workflow checks (or token-less fallback path from `docs/project/gh-cli-auth.md`).
- Upstream intake intent (yes/no) to trigger governance gates in `docs/design/botblade-security-manual/upstreams.yml`.

## Quick Start Sequence (Deterministic)
1. Execute **P0**.
2. Complete **P1 → P4** in order (hard blockers).
3. Run **P5, P6, P7** (parallel allowed where ownership is disjoint).
4. Execute **P8** before merge/release.
5. Maintain **P9** continuously for unfinished work.
6. Re-check **P10** whenever a blocker appears due to missing inputs.
