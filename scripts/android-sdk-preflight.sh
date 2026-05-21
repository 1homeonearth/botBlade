#!/usr/bin/env bash
set -euo pipefail

required_platform="platforms/android-35"
required_build_tools="build-tools/35.0.0"

fail() {
  cat >&2 <<MESSAGE
Android SDK preflight failed: $1

Install the required Android SDK packages, then rerun this check:
  ./scripts/android-sdk-bootstrap.sh

Or install manually with:
  sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

Set ANDROID_HOME to your Android SDK root, for example:
  export ANDROID_HOME=/path/to/android-sdk

Optional local setup may use an untracked local.properties file containing:
  sdk.dir=/path/to/android-sdk

If direct Google downloads are blocked, run the bootstrap script with ANDROID_CMDLINE_TOOLS_URL pointing at an approved mirror.

See docs/android-sdk-setup.md for the full bootstrap guide.
MESSAGE
  exit 1
}


check_repo_access() {
  local url="${ANDROID_PREFLIGHT_REPO_URL:-https://dl.google.com/dl/android/maven2/}"
  if ! command -v curl >/dev/null 2>&1; then
    echo "Android SDK preflight warning: curl not found; skipping optional repository reachability check." >&2
    return 0
  fi

  local code
  code=$(curl -sS -o /dev/null -w "%{http_code}" --max-time 20 "$url" || true)
  case "$code" in
    200|301|302|403|404)
      echo "Android SDK preflight repository check: reachable enough for diagnostics ($url -> HTTP $code)." >&2
      ;;
    000)
      echo "Android SDK preflight warning: cannot reach $url (network/DNS/proxy). SDK install checks passed; continuing." >&2
      ;;
    *)
      echo "Android SDK preflight warning: unexpected HTTP status $code from $url. SDK install checks passed; continuing." >&2
      ;;
  esac
}

if [[ -z "${ANDROID_HOME:-}" && -n "${ANDROID_SDK_ROOT:-}" ]]; then
  ANDROID_HOME="$ANDROID_SDK_ROOT"
fi

if [[ -z "${ANDROID_HOME:-}" && -f local.properties ]]; then
  sdk_dir_line=$(sed -n 's/^sdk\.dir=//p' local.properties | tail -n 1)
  if [[ -n "$sdk_dir_line" ]]; then
    ANDROID_HOME="$sdk_dir_line"
  fi
fi

if [[ -z "${ANDROID_HOME:-}" ]]; then
  fail 'ANDROID_HOME is not set and no sdk.dir was found in local.properties.'
fi

if [[ ! -d "$ANDROID_HOME" ]]; then
  fail "ANDROID_HOME does not point to an existing directory: $ANDROID_HOME"
fi

missing=()
if [[ ! -d "$ANDROID_HOME/$required_platform" ]]; then
  missing+=("\$ANDROID_HOME/$required_platform")
fi

if [[ ! -d "$ANDROID_HOME/$required_build_tools" ]]; then
  missing+=("\$ANDROID_HOME/$required_build_tools")
fi

if (( ${#missing[@]} > 0 )); then
  fail "missing required SDK directory/directories: ${missing[*]}"
fi

if [[ "${ANDROID_PREFLIGHT_CHECK_NETWORK:-0}" == "1" ]]; then
  check_repo_access
fi

echo "Android SDK preflight passed for $ANDROID_HOME"
