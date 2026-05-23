#!/usr/bin/env bash

warm_gradle_cache_without_wrapper() {
  local mode_label="${1:?mode label is required}"

  if [[ ! -f ./gradlew ]]; then
    warn "gradlew not found. Skipping dependency pre-fetch."
    return 0
  fi

  chmod +x ./gradlew
  log "Pre-fetching Gradle dependencies (network available during ${mode_label})"
  ./gradlew --no-daemon --version >/dev/null
  ./gradlew --no-daemon resolveAllDependencies
  log "Gradle dependency pre-fetch complete"
}
