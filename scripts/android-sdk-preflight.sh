#!/usr/bin/env bash
set -euo pipefail

required_platform="platforms/android-35"
required_build_tools="build-tools/35.0.0"

fail() {
  cat >&2 <<MESSAGE
Android SDK preflight failed: $1

Install the required Android SDK packages, then rerun this check:
  sdkmanager "platforms;android-35" "build-tools;35.0.0" "platform-tools"

Set ANDROID_HOME to your Android SDK root, for example:
  export ANDROID_HOME=/path/to/android-sdk

Optional local setup may use an untracked local.properties file containing:
  sdk.dir=/path/to/android-sdk

See docs/android-sdk-setup.md for the full bootstrap guide.
MESSAGE
  exit 1
}

if [[ -z "${ANDROID_HOME:-}" ]]; then
  fail 'ANDROID_HOME is not set.'
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

echo "Android SDK preflight passed for $ANDROID_HOME"
