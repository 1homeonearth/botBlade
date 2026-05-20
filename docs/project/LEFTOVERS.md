# LEFTOVERS

## Remaining work requiring GitHub-hosted execution
1. Trigger Android workflow for `pull_request` and `push` to `main`; capture run URLs.
2. Verify wrapper fallback behavior:
   - with `./gradlew` present+executable path
   - with wrapper missing/non-executable path (uses `gradle`)
3. Verify `assembleDebug` and `assembleRelease` complete in both required runs.
4. Verify release artifacts/checksums (`bot-blade.apk`, aliases, `SHA256SUMS.txt`, `release.json`, `INSTALL.md`).
5. Verify attestation outputs in signed runs.
6. Complete Android UI polish screenshot pass in README.

## Session notes (2026-05-20)
- Completed:
  - Removed binary `gradle/wrapper/gradle-wrapper.jar` to satisfy binary-restricted PR channels.
  - Updated `.github/workflows/android.yml` preflight to warn (not fail) when `./gradlew` is absent.
  - Updated build step to use wrapper when available, else fallback to system `gradle`.
- Commands run:
  - `rm -f gradle/wrapper/gradle-wrapper.jar`
- Current blockers:
  - Cannot run GitHub Actions from this container (`gh` CLI unavailable and no GitHub UI access from here).
- Exact continuation:
  1. Open PR and confirm it is accepted without binary artifacts.
  2. Run workflow on PR and main push; store URLs and outcomes in `docs/project/ISSUES.md`.
  3. If hosted runners lack system gradle, re-add text-only instructions or wrapper support strategy based on runner capabilities.


## Additional follow-up (2026-05-20)
- Validate sticky PR artifact comment behavior by rerunning the same PR workflow and confirming the existing marker comment is edited in place (not duplicated).
- Suggested command (from a GitHub-authenticated shell): `gh workflow run android.yml --ref <branch>` then rerun once and inspect PR comments.
