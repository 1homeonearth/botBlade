#!/usr/bin/env bash
set -euo pipefail

# Non-interactive GitHub CLI auth bootstrap.
# Priorities:
# 1) GH_TOKEN env var
# 2) GITHUB_TOKEN env var
# 3) ~/.config/gh/token (first line)

DEBUG_MODE="${GH_AUTO_AUTH_DEBUG:-0}"

debug_log() {
  if [[ "$DEBUG_MODE" == "1" ]]; then
    echo "[gh-auto-auth][debug] $*" >&2
  fi
}

if ! command -v gh >/dev/null 2>&1; then
  echo "gh CLI not found; skipping auto-auth" >&2
  exit 0
fi

debug_log "gh CLI found at: $(command -v gh)"

if gh auth status --hostname github.com >/dev/null 2>&1; then
  debug_log "gh auth status indicates already logged in for github.com"
  exit 0
fi

debug_log "gh auth status indicates no active login for github.com"
debug_log "Checking token source: GH_TOKEN env var"
token="${GH_TOKEN:-}"
if [[ -n "$token" ]]; then
  debug_log "Using token source: GH_TOKEN env var"
else
  debug_log "GH_TOKEN env var not set; checking GITHUB_TOKEN env var"
  token="${GITHUB_TOKEN:-}"
  if [[ -n "$token" ]]; then
    debug_log "Using token source: GITHUB_TOKEN env var"
  else
    debug_log "GITHUB_TOKEN env var not set; checking file source: ~/.config/gh/token"
    if [[ -f "$HOME/.config/gh/token" ]]; then
      token="$(head -n 1 "$HOME/.config/gh/token" | tr -d '\r\n')"
      if [[ -n "$token" ]]; then
        debug_log "Using token source: ~/.config/gh/token"
      else
        debug_log "~/.config/gh/token exists but first line is empty"
      fi
    else
      debug_log "Token file not found: ~/.config/gh/token"
    fi
  fi
fi

if [[ -z "$token" ]]; then
  cat >&2 <<'MSG'
No GitHub token found; skipping gh login.
Token lookup order:
  1) GH_TOKEN (runtime environment variable)
  2) GITHUB_TOKEN (runtime environment variable)
  3) ~/.config/gh/token (first line)

If your secret is configured in the Codex UI, make sure it is injected into this running container as GH_TOKEN or GITHUB_TOKEN,
or store the token in ~/.config/gh/token.
MSG
  exit 0
fi

printf '%s' "$token" | gh auth login --hostname github.com --with-token >/dev/null

gh auth setup-git >/dev/null

debug_log "gh auth login and setup-git completed"
echo "gh CLI login configured for github.com" >&2
