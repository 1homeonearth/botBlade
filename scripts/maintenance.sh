#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[maintenance] $*"; }
warn() { echo "[maintenance][warn] $*" >&2; }

warm_gradle_cache_without_wrapper() {
  if [[ ! -f ./gradlew ]]; then
    warn "gradlew not found. Skipping dependency pre-fetch."
    return 0
  fi
  chmod +x ./gradlew
  log "Pre-fetching Gradle dependencies (network available during maintenance)"
  ./gradlew --no-daemon --version >/dev/null
  ./gradlew --no-daemon resolveAllDependencies
  log "Gradle dependency pre-fetch complete"
}

warm_gradle_cache_without_wrapper
