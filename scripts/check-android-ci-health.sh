#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/.." && pwd)"
workflow_ref=".github/workflows/android.yml"
branch="${1:-$(git -C "$repo_root" rev-parse --abbrev-ref HEAD)}"

auth_hint() {
  cat >&2 <<'MSG'
Unable to query workflow runs because GitHub CLI is not authenticated.

To fix:
  1) Ensure GH_TOKEN or GITHUB_TOKEN is injected into this runtime environment, or place a token in ~/.config/gh/token.
  2) Run: ./scripts/gh-auto-auth.sh
  3) Re-run: ./scripts/check-android-ci-health.sh
MSG
}

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI not found; cannot check CI workflow health." >&2
  exit 1
fi

"$repo_root/scripts/gh-auto-auth.sh" >/dev/null 2>&1 || true

if ! gh auth status --hostname github.com >/dev/null 2>&1; then
  auth_hint
  exit 2
fi

run_json="$(gh run list --workflow "$workflow_ref" --branch "$branch" --limit 1 --json databaseId,status,conclusion,url,headBranch 2>/dev/null || true)"
if [[ -z "$run_json" || "$run_json" == "[]" ]]; then
  echo "No runs found for $workflow_ref on branch '$branch'." >&2
  exit 3
fi

summary="$(python3 - <<'PY' "$run_json"
import json, sys
runs = json.loads(sys.argv[1])
run = runs[0]
status = run.get('status') or 'unknown'
conclusion = run.get('conclusion') or 'pending'
print(f"{status}|{conclusion}|{run.get('url','')}|{run.get('headBranch','')}")
PY
)"

status="${summary%%|*}"
rest="${summary#*|}"
conclusion="${rest%%|*}"
rest="${rest#*|}"
url="${rest%%|*}"
head_branch="${rest#*|}"

echo "Latest android.yml run on branch '$head_branch': status=$status conclusion=$conclusion"
echo "Run URL: $url"

if [[ "$status" == "completed" && "$conclusion" != "success" ]]; then
  echo "CI health gate: FAIL (latest run is not successful)." >&2
  exit 4
fi

echo "CI health gate: PASS"
