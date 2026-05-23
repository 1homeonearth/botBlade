#!/usr/bin/env bash
set -euo pipefail

log()  { echo "[setup] $*"; }
warn() { echo "[setup][warn] $*" >&2; }

source "$(dirname "${BASH_SOURCE[0]}")/lib/gradle-cache-warm.sh"

warm_gradle_cache_without_wrapper setup
