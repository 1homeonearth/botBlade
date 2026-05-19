# ISSUES

This repository tracks detailed troubleshooting chains in `docs/project/ISSUES.md`.

## 2026-05-19T06:45:00Z — Repository rename reference sweep (`royalScepter` -> `botBlade`)
### 2026-05-19T06:45:00Z — Replace legacy name references
- Context: User requested replacement of all textual references to `royalScepter`/related branding variants with `botBlade` variants.
- Commands run:
  - `rg -n "royalScepter|royal-scepter|royal scepter|RoyalScepter|Royal Scepter"`
  - `python - <<'PY' ...` (batch replace across docs/app files)
  - `rg -n "royalScepter|royal-scepter|royal scepter|RoyalScepter|Royal Scepter" || true`
- Observed output/errors: Initial matches found in README/docs/Android settings + manifest; post-change scan returned no matches.
- Versions/environment: UTC shell session on 2026-05-19, repository root `/workspace/royalScepter`.
- Status: Complete
- Next action: Keep historical troubleshooting in `docs/project/ISSUES.md` aligned with future naming decisions.
