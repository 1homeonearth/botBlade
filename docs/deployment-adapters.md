# Deployment Adapters

Deployment adapters provide a common interface for local and future remote deployment targets.

## Interface

Adapters implement:

- `validateTarget(target)`
- `prepare(context)`
- `deploy(context)`
- `status(context)`
- `start(context)`
- `stop(context)`
- `restart(context)`
- `logs(context)`
- optional `rollback(context)`

The context includes project metadata, deployment target metadata, the selected build job, and the local runtime service.

## local_process

Implemented for local development. It validates `target.type === "local_process"`, prepares generated files, marks a successful build as deployable by local runtime, and delegates status/start/stop/restart/logs to `LocalProcessRuntimeService`.

Use this adapter when the backend host is also the runtime host.

## local_docker

Skeleton only. Target creation and Docker CLI availability testing exist, but deployment currently returns `DOCKER_NOT_CONFIGURED`. This is intentional so the backend does not report fake Docker success.

Future work includes image build, container lifecycle, volume/env handling, registry publishing, and log streaming.

## Credential handling

Adapters should receive credential references through `secretRefs`, resolve them only when needed, and redact all logs. Target `config` rejects secret-like keys such as token/password/secret.
