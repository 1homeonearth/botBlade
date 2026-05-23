#!/usr/bin/env bash

warm_gradle_cache_without_wrapper() {
  local mode_label="${1:?mode label is required}"
  local strict_warm_cache="${BOTBLADE_STRICT_WARM_CACHE:-0}"

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

  log "Pre-fetching Gradle dependencies (network available during ${mode_label})"
  if ! (
    ./gradlew --no-daemon --version >/dev/null &&
    ./gradlew --no-daemon resolveAllDependencies
  ); then
    warn "Gradle cache warm step failed; ${mode_label} can continue. Set BOTBLADE_STRICT_WARM_CACHE=1 to treat this as fatal."
    if [[ "$strict_warm_cache" == "1" ]]; then
      return 1
    fi
    return 0
  fi

  log "Gradle dependency pre-fetch complete"
}
