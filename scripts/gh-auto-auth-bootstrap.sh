#!/usr/bin/env bash
set -euo pipefail

profile_file="${GH_PROFILE_FILE:-$HOME/.bash_profile}"
marker_start="# >>> botBlade gh auto-auth >>>"
marker_end="# <<< botBlade gh auto-auth <<<"
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

echo "Installed gh auto-auth hook in $profile_file"
