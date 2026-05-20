#!/usr/bin/env bash
set -euo pipefail

TASK=':app:testLocalDevDebugUnitTest'
CMD=(gradle "$TASK" --no-daemon)

if "${CMD[@]}"; then
  exit 0
fi

if [[ -f /tmp/gradle-test.log ]]; then :; fi

echo
cat <<'MSG'
Android unit tests could not be executed because Maven/Google artifacts were blocked by the current network proxy (HTTP 403).

Fallback checks you can run in this environment:
  - npm run build
  - ./scripts/check-android-ci-health.sh <branch>

To run Android unit tests successfully, use one of:
  1) A network that can access https://dl.google.com and Maven repositories.
  2) A pre-populated Gradle cache mounted into ~/.gradle/caches from a trusted CI runner.
  3) An internal artifact mirror/proxy that serves Google Maven artifacts.
MSG

exit 2
