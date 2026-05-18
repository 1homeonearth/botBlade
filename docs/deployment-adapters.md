# Deployment adapters

Deployment adapters provide a common interface for local and remote-capable deployment targets. The first real production-capable target implemented by this backend is **Docker local** (`local_docker`), which builds and runs the generated bot on the Docker daemon available to the backend process.

## Adapter contract

Every adapter exposes capabilities and implements:

- `validateTarget(target)`
- `prepare(context)`
- `deploy(context)`
- `status(context)`
- `start(context)`
- `stop(context)`
- `restart(context)`
- `logs(context)`
- optional `rollback(context)`

The context includes project metadata, deployment target metadata, the selected succeeded build, the local runtime service, a secret reference resolver, and a command runner. Secret values are resolved only at deploy/action time and are redacted by deployment job logging.

## Build artifacts

Successful builds now create a versioned tarball under the generated project workspace:

```text
generated-projects/<projectId>/artifacts/<buildId>.tgz
```

The artifact excludes `node_modules` and the `artifacts` directory itself. Deployment jobs keep `build.artifactPath` so adapters can identify the exact packaged build they are deploying.

## `local_process`

Implemented for local development. It validates `target.type === "local_process"`, prepares generated files, marks a successful build as deployable by local runtime, and delegates status/start/stop/restart/logs to `LocalProcessRuntimeService`.

Capabilities:

- supported: `deploy`, `status`, `start`, `stop`, `restart`, `logs`
- unsupported: `rollback`

## `local_docker`

Implemented as the first production deployment path. It validates `target.type === "local_docker"` and supports optional non-secret target config:

```json
{
  "image": "royalscepter/my-bot",
  "dockerfile": "Dockerfile"
}
```

Secret values must not be placed in `config`; secret-like config keys are rejected. Instead, pass secret IDs in `secretRefs`. During deploy, the adapter resolves `secretRefs` to environment variables by uppercasing and sanitizing secret names, also mapping `project.discord.tokenSecretRef` to `DISCORD_TOKEN`. Values are written to a temporary env-file in the generated workspace, passed to `docker run --env-file`, and removed immediately after `docker run` returns.

Deploy semantics:

1. Build image: `docker build -t <image>:<buildId> -f <dockerfile> .`
2. Replace current container: `docker rm -f royalscepter-<project-slug>` (ignored if missing).
3. Start container: `docker run -d --name royalscepter-<project-slug> --restart unless-stopped --env-file <tempfile> <image>:<buildId>`.

Runtime semantics:

- `status`: `docker inspect --format {{.State.Status}} <container>`
- `logs`: `docker logs --tail 200 <container>`
- `start`: `docker start <container>`
- `stop`: `docker stop <container>`
- `restart`: `docker restart <container>`
- `rollback`: replace the current container with the most recent previously deployed image for the same project and target.

## API behavior

Deployment targets returned by `/api/deployment-targets` include adapter `capabilities`, allowing clients to show supported and unsupported actions clearly. Deployment action routes are:

- `POST /api/projects/:projectId/deployments` — deploy a succeeded build to a target.
- `GET /api/projects/:projectId/deployments/:deploymentId/status` — adapter status.
- `GET /api/projects/:projectId/deployments/:deploymentId/logs` — adapter logs.
- `POST /api/projects/:projectId/deployments/:deploymentId/start` — adapter start.
- `POST /api/projects/:projectId/deployments/:deploymentId/stop` — adapter stop.
- `POST /api/projects/:projectId/deployments/:deploymentId/restart` — adapter restart.
- `POST /api/projects/:projectId/deployments/:deploymentId/rollback` — adapter rollback when supported.

Unsupported actions return `DEPLOYMENT_ACTION_UNSUPPORTED` with the target type and action.

## Security notes

- Keep secrets in `secretRefs`; target `config` rejects keys containing `secret`, `token`, or `password`.
- Generated deployment logs are passed through secret redaction.
- Do not commit generated workspaces, env files, artifacts, or local Docker credentials.
