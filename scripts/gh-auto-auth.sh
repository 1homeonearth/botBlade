#!/usr/bin/env bash
set -euo pipefail

# Non-interactive GitHub CLI auth bootstrap.
# Priorities:
# 1) GH_TOKEN env var
# 2) GITHUB_TOKEN env var
# 3) GH_Token env var (legacy/mixed-case compatibility)
# 4) ~/.config/gh/token (first line)

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI not found; skipping auto-auth" >&2
  exit 0
fi

if gh auth status --hostname github.com >/dev/null 2>&1; then
  exit 0
fi

token="${GH_TOKEN:-${GITHUB_TOKEN:-${GH_Token:-}}}"
if [[ -z "$token" && -f "$HOME/.config/gh/token" ]]; then
  token="$(head -n 1 "$HOME/.config/gh/token" | tr -d '\r\n')"
fi

if [[ -z "$token" ]]; then
  echo "No GitHub token found in GH_TOKEN, GITHUB_TOKEN, GH_Token, or ~/.config/gh/token; skipping gh login." >&2
  exit 0
fi

printf '%s' "$token" | gh auth login --hostname github.com --with-token >/dev/null

gh auth setup-git >/dev/null

echo "gh CLI login configured for github.com" >&2
