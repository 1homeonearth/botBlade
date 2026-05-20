# GitHub CLI automatic login in this environment

Use this repo's helper scripts to configure `gh` automatic login for each new shell session.

## One-time setup

```bash
./scripts/gh-auto-auth-bootstrap.sh
```

The bootstrap requires GitHub CLI (`gh`) to already be installed; it exits with install hints when `gh` is missing.

That command writes a small managed block to `~/.bashrc` (or `GH_PROFILE_FILE` when set) which runs `scripts/gh-auto-auth.sh` on shell startup.

For login shell compatibility, the bootstrap also writes a managed bridge block in `~/.bash_profile` to source `~/.bashrc`.

## Provide your secret token

Set one of the following before starting a new shell:

- `GH_TOKEN`
- `GITHUB_TOKEN`
- `GH_Token` (legacy mixed-case fallback)

Or store a token in `~/.config/gh/token` (first line only).

> If you add or edit secrets in the Codex UI, restart/recreate the environment (or otherwise confirm env-variable injection is active) so the running container can see updated `GH_TOKEN`/`GITHUB_TOKEN` values.

The startup hook runs non-interactive login via:

```bash
gh auth login --hostname github.com --with-token
gh auth setup-git
```

## Debug mode (optional)

Set `GH_AUTO_AUTH_DEBUG=1` to print which token source checks were performed without printing token values:

```bash
GH_AUTO_AUTH_DEBUG=1 ./scripts/gh-auto-auth.sh
```

## Quick verification

```bash
command -v gh

env | awk -F= '/^(GH_TOKEN|GITHUB_TOKEN)=/ { print $1 "=<set>" }'

./scripts/gh-auto-auth.sh

gh auth status --hostname github.com
```


## CI health gate helper

Use the repo helper to run auto-auth first and then check the latest `android.yml` workflow status for the current branch:

```bash
./scripts/check-android-ci-health.sh
```

Pass a branch name explicitly when needed:

```bash
./scripts/check-android-ci-health.sh main
```
