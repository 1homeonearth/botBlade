#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[setup] $*"; }
warn() { echo "[setup][warn] $*" >&2; }

warm_gradle_cache_without_wrapper() {
  if [[ ! -f ./gradlew ]]; then
    warn "gradlew not found. Skipping dependency pre-fetch."
    return 0
  fi
  # Keep permission mutation opt-in so local filesystem modes are not silently rewritten.
  if [[ ! -x ./gradlew ]]; then
    if [[ "${BOTBLADE_FIX_GRADLEW_PERMS:-0}" == "1" ]]; then
      log "gradlew is not executable; applying chmod because BOTBLADE_FIX_GRADLEW_PERMS=1"
      if ! chmod +x ./gradlew; then
        warn "Failed to chmod ./gradlew; skipping dependency pre-fetch."
        return 0
      fi
    else
      warn "gradlew is not executable. Set BOTBLADE_FIX_GRADLEW_PERMS=1 to allow chmod; skipping dependency pre-fetch."
      return 0
    fi
  fi
  log "Pre-fetching Gradle dependencies (network available during setup)"
  ./gradlew --no-daemon --version >/dev/null
  ./gradlew --no-daemon resolveAllDependencies
  log "Gradle dependency pre-fetch complete"
}

warm_gradle_cache_without_wrapper
