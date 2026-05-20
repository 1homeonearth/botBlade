#!/usr/bin/env bash
set -euo pipefail

TASKS=(:app:processDebugMainManifest :app:processReleaseMainManifest)
CHECK_ONLY="${CHECK_ONLY:-0}"

for arg in "$@"; do
  case "$arg" in
    --check-only)
      CHECK_ONLY=1
      ;;
    *)
      echo "ERROR: Unknown argument: $arg" >&2
      echo "Usage: $0 [--check-only]" >&2
      exit 1
      ;;
  esac
done

if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Using existing Gradle wrapper."
  ./gradlew "${TASKS[@]}" --no-daemon
  exit 0
fi

if [ "$CHECK_ONLY" = "1" ]; then
  echo "ERROR: Gradle wrapper is missing and check-only mode is enabled." >&2
  echo "Remediation:" >&2
  echo "  - Run without --check-only to auto-regenerate wrapper files." >&2
  echo "  - Or run: gradle wrapper --gradle-version 8.14.4 --distribution-type bin" >&2
  echo "  - Then rerun: $0 --check-only" >&2
  exit 1
fi

if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Missing runnable ./gradlew+jar and system 'gradle' command." >&2
  echo "Install Gradle or restore gradle/wrapper/gradle-wrapper.jar first." >&2
  exit 1
fi

echo "Gradle wrapper missing; restoring wrapper files via system Gradle 8.14.4 target."
gradle wrapper --gradle-version 8.14.4 --distribution-type bin

if [ ! -x ./gradlew ] || [ ! -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "ERROR: Failed to restore runnable wrapper." >&2
  exit 1
fi

./gradlew "${TASKS[@]}" --no-daemon
