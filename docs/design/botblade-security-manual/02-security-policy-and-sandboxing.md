# 02 — Security Policy and Sandboxing

## Policy framework

- **[Mixed]** Policy is explicit and versioned; full gate coverage remains roadmap work.
- Layers: baseline policy, environment overlay, workload profile overlay, bounded session override.
- Conflict rule: most restrictive outcome wins.

## Mandatory gates

Security gates must execute before:

- build actions (`build_gate`)
- runtime activation (`runtime_gate`)
- deployment actions (`deploy_gate`)
- terminal sessions and privileged commands (`terminal_gate`)
- module install/enable/update/remove (`module_gate`)
- external handoffs/intents (`external_intent_gate`)

Gate invariants:
- deny-by-default
- explicit capability grants only
- fail-closed for malformed, unsigned, unknown, or profile-mismatched policy inputs

## Sandbox policy requirements

- **[Planned]** Rust policy engine evaluates sandbox capability sets.
- Least privilege applies to filesystem, process, network, IPC, and external-intent scopes.
- Capability inheritance across sessions is denied unless explicitly authorized.

## Secret handling policy

- **[Implemented]** No raw secret values in logs/docs/tests/artifacts.
- **[Planned]** Secret references include TTL, destination binding, and purpose tags.
- **[Planned]** Exposure attempts trigger deny + audit.

## Import and archive safety gates

- `repo_import_gate`: untrusted by default; no scripts/hooks/build execution during analysis.
- `archive_safety_gate`: blocks traversal, symlink escapes, decompression bombs, manifest mismatch.
- `extraction_gate`: allows extraction only after approved destination + profile binding.

## Audit requirements

Audit records must include deterministic audit IDs, policy version, gate verdict, and redacted rationale.

## Test requirements

- Gate matrix tests (allow/deny/review).
- Sandbox profile compatibility tests.
- Secret redaction tests (logs, diagnostics, terminal previews).
- Archive safety tests (traversal, symlink, bomb-shape fixtures).
