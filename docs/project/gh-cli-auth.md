# GitHub CLI automatic login in this environment

Use this repo's helper scripts to configure `gh` automatic login for each new shell session.

## One-time setup

```bash
./scripts/gh-auto-auth-bootstrap.sh
```

That command writes a small managed block to `~/.bash_profile` which runs `scripts/gh-auto-auth.sh` on shell startup.

## Provide your secret token

Set one of the following before starting a new shell:

- `GH_TOKEN`
- `GITHUB_TOKEN`

Or store a token in `~/.config/gh/token` (first line only).

The startup hook runs non-interactive login via:

```bash
gh auth login --hostname github.com --with-token
gh auth setup-git
```

## Verify

```bash
gh auth status
```
