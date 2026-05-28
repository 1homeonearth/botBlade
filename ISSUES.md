# ISSUES Resolution Log

## 2026-05-28T03:54:54Z UTC — ISSUE 1 (ci build type / prodCi variant)
- Problem: CI depends on `:app:assembleProdCi` variant consistency.
- Resolution: Confirmed `prod` flavor dimension already exists and `ci` build type exists; updated `ci` to be non-debuggable to match CI expectations and reduce debug-surface risk.
- Status: Fixed in `app/build.gradle.kts`.

## 2026-05-28T03:54:54Z UTC — ISSUE 2 (ENOBUFS in git status)
- Problem: Large `git status --porcelain` output could overflow buffer and be misreported as unavailable/clean.
- Resolution: Added explicit `maxBuffer` (10 MiB), ENOBUFS-specific safe-mode handling (`available: true`, `clean: false`, explanatory note), and test coverage including ENOBUFS path.
- Status: Fixed in `backend/src/services/gitStatusService.ts` and `backend/src/__tests__/gitStatusService.test.ts`.

## 2026-05-28T03:54:54Z UTC — ISSUE 3 (workflow checkout integrity)
- Problem: Workflow used manual `git init/fetch/checkout` instead of `actions/checkout`.
- Resolution: Replaced manual checkout in both jobs with `actions/checkout@v4` and removed redundant wrapper chmod.
- Status: Fixed in `.github/workflows/android.yml`.

## 2026-05-28T03:54:54Z UTC — ISSUE 4 (wrapper files committed)
- Problem: Prior CI failures reported missing wrapper artifacts.
- Resolution: Verified all required wrapper files are tracked in git (`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`, `gradle/wrapper/gradle-wrapper.properties`) and `.gitignore` does not exclude wrapper jar.
- Status: Verified (no file regeneration required).

## 2026-05-28T03:54:54Z UTC — ISSUE 5 (APK output path mismatch risk)
- Problem: Workflow hardcoded APK path can fail when variant output naming differs.
- Resolution: Updated workflow APK verification/copy steps to discover the `prod/ci` APK via `find` glob under `app/build/outputs/apk`.
- Status: Fixed in `.github/workflows/android.yml`.
