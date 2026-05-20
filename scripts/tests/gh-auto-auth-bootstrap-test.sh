#!/usr/bin/env bash
set -euo pipefail

repo_root="$(cd "$(dirname "$0")/../.." && pwd)"
script="$repo_root/scripts/gh-auto-auth-bootstrap.sh"

assert_contains() {
  local file="$1"
  local pattern="$2"
  if ! grep -Fq "$pattern" "$file"; then
    echo "Expected '$pattern' in $file" >&2
    exit 1
  fi
}

assert_count() {
  local file="$1"
  local pattern="$2"
  local expected="$3"
  local count
  count=$(grep -Fc "$pattern" "$file" || true)
  if [ "$count" -ne "$expected" ]; then
    echo "Expected $expected occurrences of '$pattern' in $file, got $count" >&2
    exit 1
  fi
}

tmp_home="$(mktemp -d)"
trap 'rm -rf "$tmp_home"' EXIT

set +e
missing_gh_output=$(HOME="$tmp_home" PATH="/definitely-missing" /bin/bash "$script" 2>&1)
missing_gh_status=$?
set -e
if [ "$missing_gh_status" -eq 0 ]; then
  echo "Expected bootstrap to fail when gh is missing" >&2
  exit 1
fi
if [[ "$missing_gh_output" != *"missing command: gh"* ]]; then
  echo "Expected missing gh error message, got: $missing_gh_output" >&2
  exit 1
fi

HOME="$tmp_home" "$script" >/dev/null
assert_contains "$tmp_home/.bashrc" "# >>> botBlade gh auto-auth >>>"
assert_count "$tmp_home/.bashrc" "# >>> botBlade gh auto-auth >>>" 1
assert_contains "$tmp_home/.bash_profile" "# >>> gh-auto-auth bashrc bridge >>>"
assert_count "$tmp_home/.bash_profile" "# >>> gh-auto-auth bashrc bridge >>>" 1
assert_contains "$tmp_home/.bash_profile" "case \$- in"
assert_contains "$tmp_home/.bash_profile" "*i*)"

HOME="$tmp_home" "$script" >/dev/null
assert_count "$tmp_home/.bashrc" "# >>> botBlade gh auto-auth >>>" 1
assert_count "$tmp_home/.bash_profile" "# >>> gh-auto-auth bashrc bridge >>>" 1

override_file="$tmp_home/custom_profile"
HOME="$tmp_home" GH_PROFILE_FILE="$override_file" "$script" >/dev/null
assert_contains "$override_file" "# >>> botBlade gh auto-auth >>>"
assert_count "$override_file" "# >>> botBlade gh auto-auth >>>" 1

cat > "$tmp_home/.bash_profile" <<'PROFILE'
# existing content
# >>> gh-auto-auth bashrc bridge >>>
if [ -f "$HOME/.bashrc" ]; then
    source "$HOME/.bashrc"
fi
# <<< gh-auto-auth bashrc bridge <<<
PROFILE
HOME="$tmp_home" "$script" >/dev/null
assert_count "$tmp_home/.bash_profile" "# >>> gh-auto-auth bashrc bridge >>>" 1
assert_contains "$tmp_home/.bash_profile" "case \$- in"
assert_contains "$tmp_home/.bash_profile" "*i*)"



cat > "$tmp_home/.bash_profile" <<'PROFILE'
# user preface
# >>> gh-auto-auth bashrc bridge >>>
if [ -f "$HOME/.bashrc" ]; then
  source "$HOME/.bashrc"
fi
# <<< gh-auto-auth bashrc bridge <<<
# user suffix
PROFILE
HOME="$tmp_home" "$script" >/dev/null
HOME="$tmp_home" "$script" >/dev/null
assert_count "$tmp_home/.bash_profile" "# >>> gh-auto-auth bashrc bridge >>>" 1
assert_contains "$tmp_home/.bash_profile" "case \$- in"
assert_contains "$tmp_home/.bash_profile" "*i*)"
assert_contains "$tmp_home/.bash_profile" 'source "$HOME/.bashrc"'

echo "gh-auto-auth-bootstrap tests passed"
