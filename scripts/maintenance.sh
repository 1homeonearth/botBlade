#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[maintenance] $*"; }
warn() { echo "[maintenance][warn] $*" >&2; }

source "$(dirname "${BASH_SOURCE[0]}")/lib/gradle-cache-warm.sh"

warm_gradle_cache_without_wrapper maintenance
