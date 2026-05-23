#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[setup] $*"; }
warn() { echo "[setup][warn] $*" >&2; }

warm_gradle_cache_without_wrapper() {
  local strict_warm_cache="${BOTBLADE_STRICT_WARM_CACHE:-0}"
  if [[ ! -f ./gradlew ]]; then
    warn "gradlew not found. Skipping dependency pre-fetch."
    return 0
  fi
  chmod +x ./gradlew
  log "Pre-fetching Gradle dependencies (network available during setup)"
  if ! (
    ./gradlew --no-daemon --version >/dev/null &&
    ./gradlew --no-daemon resolveAllDependencies
  ); then
    warn "Gradle cache warm step failed; setup can continue. Set BOTBLADE_STRICT_WARM_CACHE=1 to treat this as fatal."
    if [[ "$strict_warm_cache" == "1" ]]; then
      return 1
    fi
    return 0
  fi
  log "Gradle dependency pre-fetch complete"
}

warm_gradle_cache_without_wrapper
