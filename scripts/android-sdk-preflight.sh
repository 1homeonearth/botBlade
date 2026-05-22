#!/usr/bin/env bash
set -euo pipefail

PASS=0; FAIL=0
log()  { echo "[preflight] $*"; }
warn() { echo "[preflight][warn] $*" >&2; }
fail() { echo "[preflight][FAIL] $*" >&2; FAIL=$((FAIL+1)); }
pass() { log "OK: $*"; PASS=$((PASS+1)); }

check_android_home() {
  if [[ -z "${ANDROID_HOME:-}" && -n "${ANDROID_SDK_ROOT:-}" ]]; then
    ANDROID_HOME="$ANDROID_SDK_ROOT"
    export ANDROID_HOME
  fi

  if [[ -z "${ANDROID_HOME:-}" ]]; then
    fail "ANDROID_HOME is not set"
    return
  fi

  if [[ ! -d "$ANDROID_HOME" ]]; then
    fail "ANDROID_HOME does not point to an existing directory: $ANDROID_HOME"
    return
  fi

  pass "ANDROID_HOME=$ANDROID_HOME"

  local required_components=(
    "platforms/android-35"
    "build-tools/35.0.0"
    "platform-tools"
  )
  local component
  for component in "${required_components[@]}"; do
    if [[ -d "$ANDROID_HOME/$component" ]]; then
      pass "Android SDK component present: $component"
    else
      fail "Missing Android SDK component: $ANDROID_HOME/$component"
    fi
  done
}

check_repo_access() {
  if [[ "${ANDROID_PREFLIGHT_CHECK_NETWORK:-0}" != "1" ]]; then
    warn "Skipping Maven network probe; set ANDROID_PREFLIGHT_CHECK_NETWORK=1 for connectivity diagnostics."
    return 0
  fi

  local url="${ANDROID_PREFLIGHT_MAVEN_URL:-https://dl.google.com/dl/android/maven2/}"
  local http_code
  http_code=$(curl -sSo /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
  case "$http_code" in
    200|301|302|404)
      pass "Maven repo reachable (HTTP $http_code)"
      ;;
    000)
      warn "Maven repo unreachable (network/proxy/DNS failure). Builds may still work with cached dependencies or mirrors."
      ;;
    403)
      warn "Maven repo returned HTTP 403. Check proxy/auth policy if dependency resolution later fails."
      ;;
    *)
      warn "Maven repo returned HTTP $http_code. Investigate if builds fail."
      ;;
  esac
}

main() {
  check_android_home
  check_repo_access
  if [[ $FAIL -gt 0 ]]; then
    echo "[preflight] $FAIL check(s) failed. Fix before building." >&2
    exit 1
  fi
  log "All required preflight checks passed."
}

main "$@"
