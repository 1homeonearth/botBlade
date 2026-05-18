#!/usr/bin/env bash
set -euo pipefail

sdk_root="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-$HOME/android-sdk}}"
cmdline_tools_url="${ANDROID_CMDLINE_TOOLS_URL:-https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip}"
required_packages=("platform-tools" "platforms;android-35" "build-tools;35.0.0")
profile_file="${ANDROID_PROFILE_FILE:-$HOME/.bash_profile}"

usage() {
  cat <<USAGE
Usage: [ANDROID_HOME=/path/to/sdk] [ANDROID_CMDLINE_TOOLS_URL=https://mirror/commandlinetools-linux-...zip] $0

Installs Android command-line tools and the SDK packages required by this repo,
then writes exports to your shell profile and an untracked local.properties file.
USAGE
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

write_shell_exports() {
  local marker_start="# >>> botBlade Android SDK >>>"
  local marker_end="# <<< botBlade Android SDK <<<"
  local block
  block=$(cat <<EXPORTS
$marker_start
export ANDROID_HOME="$sdk_root"
export ANDROID_SDK_ROOT="$sdk_root"
case ":\$PATH:" in
  *":\$ANDROID_HOME/platform-tools:"*) ;;
  *) export PATH="\$ANDROID_HOME/platform-tools:\$PATH" ;;
esac
case ":\$PATH:" in
  *":\$ANDROID_HOME/cmdline-tools/latest/bin:"*) ;;
  *) export PATH="\$ANDROID_HOME/cmdline-tools/latest/bin:\$PATH" ;;
esac
$marker_end
EXPORTS
)
  mkdir -p "$(dirname "$profile_file")"
  touch "$profile_file"
  python3 - "$profile_file" "$marker_start" "$marker_end" "$block" <<'PY'
import sys
from pathlib import Path
path = Path(sys.argv[1])
start, end, block = sys.argv[2], sys.argv[3], sys.argv[4]
text = path.read_text()
if start in text and end in text:
    before, rest = text.split(start, 1)
    _, after = rest.split(end, 1)
    text = before.rstrip() + "\n" + block + after
else:
    text = text.rstrip() + "\n" + block + "\n"
path.write_text(text)
PY
}

write_local_properties() {
  printf 'sdk.dir=%s\n' "$sdk_root" > local.properties
}

install_cmdline_tools() {
  if [[ -x "$sdk_root/cmdline-tools/latest/bin/sdkmanager" ]]; then
    return
  fi
  require_command curl
  require_command unzip
  local archive
  archive=$(mktemp /tmp/android-cmdline-tools.XXXXXX.zip)
  local extract_dir
  extract_dir=$(mktemp -d /tmp/android-cmdline-tools.XXXXXX)
  echo "Downloading Android command-line tools from $cmdline_tools_url"
  if ! curl -fL --retry 3 --retry-delay 2 -o "$archive" "$cmdline_tools_url"; then
    cat >&2 <<MESSAGE
Failed to download Android command-line tools.

This environment may require an approved mirror. Retry with:
  ANDROID_CMDLINE_TOOLS_URL=https://your-mirror/commandlinetools-linux-13114758_latest.zip $0
MESSAGE
    exit 1
  fi
  mkdir -p "$sdk_root/cmdline-tools"
  unzip -q -o "$archive" -d "$extract_dir"
  rm -rf "$sdk_root/cmdline-tools/latest"
  mv "$extract_dir/cmdline-tools" "$sdk_root/cmdline-tools/latest"
}

install_sdk_packages() {
  yes | "$sdk_root/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk_root" --licenses >/dev/null
  "$sdk_root/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$sdk_root" "${required_packages[@]}"
}

mkdir -p "$sdk_root"
install_cmdline_tools
install_sdk_packages
write_shell_exports
write_local_properties

cat <<MESSAGE
Android SDK bootstrap complete.

For this shell, run:
  export ANDROID_HOME="$sdk_root"
  export ANDROID_SDK_ROOT="$sdk_root"
  export PATH="\$ANDROID_HOME/cmdline-tools/latest/bin:\$ANDROID_HOME/platform-tools:\$PATH"

Future login shells will load the same values from: $profile_file
The repo-local Gradle SDK pointer was written to: local.properties
MESSAGE
