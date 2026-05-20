# LEFTOVERS

- Blocker: `GH_TOKEN`/`GITHUB_TOKEN`/`GH_Token` are not present in this session environment, so `gh auth login --with-token` cannot run.
- Relevant files:
  - `scripts/gh-auto-auth.sh`
  - `docs/project/gh-cli-auth.md`
  - `docs/project/LEFTOVERS.md`
- Exact next action:
  1. Launch Codex with one of these env vars set to a PAT: `GH_TOKEN` (preferred), `GITHUB_TOKEN`, or `GH_Token`.
  2. Run `./scripts/gh-auto-auth.sh && gh auth status --hostname github.com`.
  3. Run `./scripts/gh-auto-auth-bootstrap.sh` once to persist auto-login on new shells.
