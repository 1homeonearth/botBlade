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
  else
    pass "ANDROID_HOME=$ANDROID_HOME"
  fi
}

check_repo_access() {
  local url="https://dl.google.com/dl/android/maven2/"
  local http_code
  http_code=$(curl -sSo /dev/null -w "%{http_code}" --max-time 10 "$url" 2>/dev/null || echo "000")
  case "$http_code" in
    200|301|302|403|404)
      pass "Maven repo reachable (HTTP $http_code)"
      ;;
    000)
      fail "Maven repo unreachable (network/proxy/DNS failure). Set HTTPS_PROXY or fix DNS."
      ;;
    *)
      warn "Maven repo returned HTTP $http_code — investigate if builds fail"
      pass "Maven probe completed"
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
  log "All preflight checks passed."
}

main "$@"
