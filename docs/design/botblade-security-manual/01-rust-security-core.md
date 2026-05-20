# 01 — Rust Security Core + Runtime-Safe Security Architecture

## Purpose

Implement Rust as botBlade's hardened security engine, not as a full product rewrite.

Rust owns:
- hostile-input parsing
- deterministic validation
- path safety
- archive safety
- manifest validation
- command-plan validation
- checksum/signature helpers
- policy evaluation

Kotlin (Android) and Node/TypeScript (backend) remain the product/application layers.

## Core principle

Security checks happen before runtime.

Runtime hot paths must rely on cached, signed, or hash-keyed safety decisions so active bots do not pay full repository scan cost during normal start/run flows.

## Feature highlights

- Rust security core
- No code execution during repository analysis
- Cached safety reports
- Policy-gated runtime
- Secret references, not secret values
- Fail-closed path handling
- Deterministic JSON CLI output
- Optional Android-native Rust later through NDK/JNI only when useful

## Target Rust workspace

```text
rust/
  Cargo.toml
  crates/
    botblade-core/
    botblade-security/
    botblade-cli/
```

### `botblade-core`

Responsibilities:
- shared schema types
- manifest schema
- runtime profile schema
- build plan schema
- repo safety report schema
- policy decision schema
- typed error schema
- JSON serialization/deserialization contracts

### `botblade-security`

Responsibilities:
- canonical path resolver
- workspace boundary validator
- archive scanner
- zip-slip validator
- tar traversal validator
- symlink escape detector
- manifest validator
- secret-pattern redactor
- static script scanner
- Dockerfile scanner
- Docker Compose scanner
- GitHub Actions scanner
- command-plan validator
- sandbox policy evaluator
- SHA-256 and SHA-512 helpers
- signature-verification interface
- terminal escape sanitizer (later, if useful)

### `botblade-cli`

Machine-readable wrapper used by Node backend first.

Planned commands:
- `botblade-security scan-repo --root <path> --out <json>`
- `botblade-security validate-manifest --file <path> --out <json>`
- `botblade-security check-archive --file <path> --out <json>`
- `botblade-security validate-command-plan --file <path> --out <json>`
- `botblade-security evaluate-policy --input <json> --out <json>`
- `botblade-security redact --input <file> --out <json>`

## CLI output and exit behavior

Output rules:
- stdout must be valid JSON
- stderr may include diagnostics but must never include secrets
- every finding must include:
  - severity
  - category
  - title
  - message
  - file path (if relevant)
  - redacted evidence
  - recommendation
  - block flag

Exit rules:
- exit code `0` means scanner completed
- nonzero exit means scanner failed (not automatically that repo is unsafe)
- unsafe results should still return JSON with blocked decisions when possible

## Backend wrapper contract

Primary integration target:
- `backend/src/services/rustSecurityService.ts`

Responsibilities:
- locate `botblade-security` CLI
- run with timeout
- restrict paths to backend workspace root
- reject path traversal before invoking Rust
- parse JSON output
- map errors to current API error shape
- redact stderr before logging
- cache report by repository snapshot hash
- return `RUST_SECURITY_TOOL_UNAVAILABLE` if Rust toolchain is missing, while preserving existing Discord/local flows

## Cache model

Cache hash inputs:
- file path list
- file sizes
- file modified timestamps where safe
- sampled file content hashes
- manifest hash
- build plan hash
- runtime profile hash
- environment ID
- secret metadata fingerprints (never secret values)

Invalidate on:
- repository update
- manifest update
- build plan edit
- runtime profile edit
- environment edit
- secret metadata change
- upstream dependency update
- scanner version change

Runtime start must avoid full rescan when fresh safety + policy artifacts exist.

Runtime start checks:
- `safetyReportId`
- `policyDecisionId`
- report freshness
- manifest hash
- runtime profile hash
- environment ID
- required confirmations

## Repo safety scanner categories

1. Secrets
   - API keys, tokens, private keys, `.env`, credential-looking strings
   - Discord/Telegram/Slack/GitHub token-like patterns
   - database URLs
   - webhook signing secrets
   - evidence must be redacted
2. Dependencies
   - missing lockfiles
   - native extensions
   - install scripts
   - broad/unpinned versions
   - deprecated markers where visible statically
3. Scripts
   - `postinstall` / `preinstall`
   - pipe-to-shell patterns
   - encoded payloads/obfuscation
   - destructive deletion patterns
   - privilege escalation commands
4. Permissions
   - filesystem writes outside workspace
   - broad network access during build
   - privileged container/process behavior
5. Docker
   - root/no `USER`
   - latest tags
   - privileged flags
   - host mounts / docker socket usage
6. GitHub Actions
   - unpinned third-party actions
   - broad write permissions
   - risky trigger usage
   - shell script payload risks
7. License/provenance context
   - missing license
   - detected license
   - README/source URL/commit info when available
   - stars/forks/activity as weak context only

## Security decision states

- `allowed`
- `allowed_with_restrictions`
- `requires_confirmation`
- `blocked`

## Failure behavior

- Default deny when critical evidence is missing.
- Preserve deterministic JSON output even when findings are severe.
- Avoid partial execution side effects during analysis.

## Android integration path

Phase 1:
- Rust runs through backend CLI only.

Phase 2:
- optional Android NDK/JNI bridge for local-only scanning
- evaluate `cargo-ndk` impact before adoption
- do not require Android-native Rust for remote-backend users

Phase 3:
- Kotlin wrapper may call Rust library for local archive scan, manifest validation, checksum verification

## Test categories

Rust tests:
- zip-slip rejected
- tar traversal rejected
- symlink escape rejected
- absolute path rejected
- Windows drive path rejected
- normalized `..` escape rejected
- malformed manifest rejected
- secret values redacted
- unsafe scripts flagged
- Dockerfile warnings emitted
- GitHub Actions warnings emitted
- unsafe command plan blocked
- safe simple repo still analyzable
- blocked finding blocks execution where gate requires deny

Backend tests:
- Rust unavailable error is clear
- stale report blocks runtime
- cached report reused
- cache invalidates on manifest change
- secret values absent from API/log output
- existing Discord routes remain working
