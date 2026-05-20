#!/usr/bin/env bash
set -euo pipefail

if ! command -v gh >/dev/null 2>&1; then
  printf '%s\n' \
    "Error: GitHub CLI is required but not installed (missing command: gh)." \
    "Install package 'gh' first, then rerun: ./scripts/gh-auto-auth-bootstrap.sh" \
    "Ubuntu/Debian: sudo apt update && sudo apt install -y gh" >&2
  exit 1
fi

profile_file="${GH_PROFILE_FILE:-$HOME/.bashrc}"
marker_start="# >>> botBlade gh auto-auth >>>"
marker_end="# <<< botBlade gh auto-auth <<<"
bridge_start="# >>> gh-auto-auth bashrc bridge >>>"
bridge_end="# <<< gh-auto-auth bashrc bridge <<<"
repo_root="$(cd "$(dirname "$0")/.." && pwd)"
block=$(cat <<BLOCK
$marker_start
# Run non-interactive gh login when a token secret is present.
if [ -x "$repo_root/scripts/gh-auto-auth.sh" ]; then
  "$repo_root/scripts/gh-auto-auth.sh" >/dev/null 2>&1 || true
fi
$marker_end
BLOCK
)
bridge_block=$(cat <<BLOCK
$bridge_start
if [ -f "\$HOME/.bashrc" ]; then
    source "\$HOME/.bashrc"
fi
$bridge_end
BLOCK
)

mkdir -p "$(dirname "$profile_file")"
touch "$profile_file"
python3 - "$profile_file" "$marker_start" "$marker_end" "$block" <<'PY'
import pathlib, re, sys
path = pathlib.Path(sys.argv[1])
start, end, block = sys.argv[2], sys.argv[3], sys.argv[4]
text = path.read_text(encoding='utf-8') if path.exists() else ''
pat = re.compile(re.escape(start) + r".*?" + re.escape(end), re.S)
new = pat.sub(block, text) if pat.search(text) else (text.rstrip() + "\n\n" + block + "\n")
path.write_text(new, encoding='utf-8')
PY

bash_profile="$HOME/.bash_profile"
touch "$bash_profile"
python3 - "$bash_profile" "$bridge_start" "$bridge_end" "$bridge_block" <<'PY'
import pathlib, re, sys
path = pathlib.Path(sys.argv[1])
start, end, block = sys.argv[2], sys.argv[3], sys.argv[4]
text = path.read_text(encoding='utf-8') if path.exists() else ''
pat = re.compile(re.escape(start) + r".*?" + re.escape(end), re.S)
new = pat.sub(block, text) if pat.search(text) else (text.rstrip() + "\n\n" + block + "\n")
path.write_text(new, encoding='utf-8')
PY

echo "Installed gh auto-auth hook in $profile_file"
