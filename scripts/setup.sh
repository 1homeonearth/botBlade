#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[setup] $*"; }
warn() { echo "[setup][warn] $*" >&2; }

warm_gradle_cache_without_wrapper() {
  if [[ ! -f ./gradlew ]]; then
    warn "gradlew not found. Skipping dependency pre-fetch."
    return 0
  fi
  chmod +x ./gradlew
  log "Pre-fetching Gradle dependencies (network available during setup)"
  ./gradlew --no-daemon --version >/dev/null || true
  ./gradlew --no-daemon resolveAllDependencies || true
  log "Gradle dependency pre-fetch complete"
}

warm_gradle_cache_without_wrapper
