# LEFTOVERS

- Blocker: Container network/proxy policy prevents installing GitHub CLI and reaching Ubuntu/GitHub endpoints (`apt` and direct `curl` return 403; direct no-proxy DNS fails).
- Relevant files:
  - `.github/workflows/android.yml`
  - `docs/project/LEFTOVERS.md`
- Exact next action:
  1. In a GitHub-authenticated environment with working network egress (or preinstalled `gh`), run `gh auth status`.
  2. Trigger/re-run `android.yml` for the PR branch and `main`, record run URLs/outcomes in `docs/project/ISSUES.md`.
  3. Verify sticky PR comment updates in-place (existing marker comment edited, not duplicated).
