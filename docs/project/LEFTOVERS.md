# LEFTOVERS

- Blocker: `gh` is installed, but no auth token is available for non-interactive login (`./scripts/gh-auto-auth.sh` reports missing `GH_TOKEN`/`GITHUB_TOKEN`/`~/.config/gh/token`).
- Relevant files:
  - `scripts/gh-auto-auth-bootstrap.sh`
  - `scripts/gh-auto-auth.sh`
  - `docs/project/LEFTOVERS.md`
- Exact next action:
  1. Provide a GitHub token via `GH_TOKEN` or `GITHUB_TOKEN` (or write first line to `~/.config/gh/token`).
  2. Run `./scripts/gh-auto-auth-bootstrap.sh && gh auth status`.
  3. Trigger/re-run `android.yml` for the PR branch and confirm lint findings are posted without permission errors.
