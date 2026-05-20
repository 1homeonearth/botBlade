# 02 — Security Policy and Sandboxing

## Policy model

botBlade policy is explicit, versioned, and environment-aware.

Policy layers:
- Global baseline policy (immutable defaults for critical safety).
- Deployment/environment overlays (dev/staging/prod/mobile/server).
- Workload profile overlays (language/runtime/module needs).
- Session overrides (strictly bounded, audited, and time-limited).

Conflict rule: most restrictive decision wins.

## Mandatory security gates

Security policy gates must run before:
- Build execution
- Runtime activation
- Deployment actions
- Terminal session start or privileged command execution
- Module install/enable/update
- External intents/app handoffs

A gate must deny by default when required evidence is missing.

## Sandbox requirements

- Sandbox policy must be evaluated by Rust policy engine.
- Sandboxes should apply least privilege for filesystem, process, network, IPC, and external app intents.
- Gate decisions must include explicit capability grants, not implicit defaults.
- Fail closed on profile mismatch, malformed manifest, or unsigned/unknown policy artifacts.

## Secret handling policy

- Secrets are referenced, not embedded.
- Values are injected only through explicit policy-approved channels.
- Secrets are redacted in logs, traces, crash reports, terminal previews, and diagnostics.
- Secret scope includes TTL, destination binding, and purpose tagging.
- Secret exposure attempts (accidental or malicious) trigger deny+audit.

## Repo import and archive safety

- Repo import treats all content as untrusted.
- Analysis runs without executing imported scripts, hooks, builds, or binaries.
- Archive validation blocks path traversal, symlink escapes, decompression bombs, and manifest mismatch.
- Extraction is allowed only after gate approval and destination sandbox binding.

## Auditing and observability

Gate system must produce:
- deterministic audit IDs
- policy version + module version
- decision summary + redacted rationale
- escalation path for manual review

Telemetry must never include raw secret values.
