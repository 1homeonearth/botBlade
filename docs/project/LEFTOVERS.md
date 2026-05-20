# LEFTOVERS

- Blocker: No GitHub auth token is currently injected in this container, so CI workflow status cannot be queried.
- Relevant files:
  - `.github/workflows/android.yml`
  - `scripts/check-android-ci-health.sh`
- Exact next action:
  1. Inject `GH_TOKEN` or `GITHUB_TOKEN` into runtime env (or write token to `~/.config/gh/token`).
  2. Run `./scripts/check-android-ci-health.sh main` and record the run URL/outcome.


Token-less fallback playbook: `docs/project/gh-cli-auth.md#token-less-ci-verification-fallback-playbook` (use "not verifiable", not "failing", when auth is unavailable).
