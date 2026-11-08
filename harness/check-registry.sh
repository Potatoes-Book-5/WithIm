#!/usr/bin/env bash
# docs/tracking의 ID 원장과 실제 문서가 일치하는지 검사한다.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

TASKS=docs/tracking/tasks.md
INDEX=docs/tracking/index.md
ID_RE='(T|F|S|SCR|ADR)-[0-9]{3}'
errors=0
err() { echo "  [오류] $*" >&2; errors=$((errors + 1)); }

# ID로 시작하는 표의 행만 출력한다.
rows() { grep -E "^\| *${ID_RE} *\|" "$1" || true; }
first_col() { awk -F'|' '{ gsub(/ /, "", $2); print $2 }'; }

# 1. 중복 ID
registered=$( { rows "$TASKS"; rows "$INDEX"; } | first_col | sort)
dups=$(printf '%s\n' "$registered" | uniq -d)
for id in $dups; do err "중복 ID: $id"; done

is_registered() { printf '%s\n' "$registered" | grep -qx "$1"; }

# 2. 상태값
while IFS= read -r line; do
  [ -z "$line" ] && continue
  id=$(printf '%s\n' "$line" | first_col)
  status=$(printf '%s\n' "$line" | awk -F'|' '{ gsub(/ /, "", $5); print $5 }')
  case "$status" in
    todo|doing|review|done|blocked|dropped) ;;
    *) err "$id: 잘못된 작업 상태 '$status'" ;;
  esac
done <<< "$(rows "$TASKS")"

# 3. 색인 상태값과 경로
while IFS= read -r line; do
  [ -z "$line" ] && continue
  id=$(printf '%s\n' "$line" | first_col)
  status=$(printf '%s\n' "$line" | awk -F'|' '{ gsub(/ /, "", $5); print $5 }')
  path=$(printf '%s\n' "$line" | awk -F'|' '{ gsub(/[ `]/, "", $6); print $6 }')
  case "$status" in
    reserved|deprecated) ;;
    draft|approved)
      [ -f "docs/$path" ] || err "$id: 문서 없음 docs/$path" ;;
    *) err "$id: 잘못된 문서 상태 '$status'" ;;
  esac
done <<< "$(rows "$INDEX")"

# 4. 문서 파일이 색인에 있고 frontmatter가 일치하는지
for file in docs/product/features/*.md docs/product/scenarios/*.md \
            docs/design/screens/*.md docs/decisions/*.md; do
  [ -f "$file" ] || continue
  base=$(basename "$file")
  id=$(printf '%s\n' "$base" | grep -oE "^${ID_RE}" || true)
  [ -z "$id" ] && continue
  is_registered "$id" || err "$file: 색인에 없는 ID $id"
  fm_id=$(sed -n 's/^id: *//p' "$file" | head -1)
  [ "$fm_id" = "$id" ] || err "$file: frontmatter id '$fm_id'가 파일명과 다름"
  fm_task=$(sed -n 's/^task: *//p' "$file" | head -1)
  is_registered "$fm_task" || err "$file: 등록되지 않은 task '$fm_task'"
done

# 5. 본문에서 참조하는 ID가 등록되었는지 (team, tracking/README의 예시는 제외)
scan_dirs="docs/product docs/design docs/decisions docs/principles docs/platform docs/plans"
refs=$( { grep -rhoE --include='*.md' "(^|[^A-Za-z0-9_-])${ID_RE}" $scan_dirs "$TASKS" "$INDEX" || true; } \
  | grep -oE "${ID_RE}" | sort -u)
for id in $refs; do
  is_registered "$id" || err "미등록 ID 참조: $id"
done

# 6. 참조하는 규칙 ID가 정의되었는지
prefixes=$(grep -E '^\| [A-Z]+ \| `' "$INDEX" | awk -F'|' '{ gsub(/ /, "", $2); print $2 }' | paste -sd'|' -)
if [ -n "$prefixes" ]; then
  defined=$(grep -rhE --include='*.md' --exclude='compliance-matrix.md' "^\| (${prefixes})-[0-9]{3} \|" \
      docs/principles docs/platform docs/design/foundations \
    | awk -F'|' '{ gsub(/ /, "", $2); print $2 }' | sort)
  rule_dups=$(printf '%s\n' "$defined" | uniq -d)
  for id in $rule_dups; do err "중복 규칙 ID: $id"; done
  used=$( { grep -rhoE --include='*.md' "(^|[^A-Za-z0-9_-])(${prefixes})-[0-9]{3}" $scan_dirs || true; } \
    | grep -oE "(${prefixes})-[0-9]{3}" | sort -u)
  for id in $used; do
    printf '%s\n' "$defined" | grep -qx "$id" || err "정의되지 않은 규칙 참조: $id"
  done
fi

if [ "$errors" -gt 0 ]; then
  echo "check-registry 실패: $errors건" >&2
  exit 1
fi
echo "check-registry 통과"
