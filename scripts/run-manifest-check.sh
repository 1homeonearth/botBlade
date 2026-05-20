#!/usr/bin/env bash
set -euo pipefail

TASKS=(:app:processDebugMainManifest :app:processReleaseMainManifest)

if [ -x ./gradlew ] && [ -f gradle/wrapper/gradle-wrapper.jar ]; then
  echo "Using existing Gradle wrapper."
  ./gradlew "${TASKS[@]}" --no-daemon
  exit 0
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
