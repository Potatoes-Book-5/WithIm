#!/usr/bin/env bash
# Markdown 상대 링크가 실제 파일을 가리키는지 검사한다.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

errors=0
tmp=$(mktemp)
trap 'rm -f "$tmp"' EXIT

find . -name '*.md' \
  -not -path './.git/*' -not -path './.gradle/*' -not -path './.idea/*' \
  -not -path './harness/runs/*' -not -path '*/build/*' > "$tmp"

while IFS= read -r file; do
  dir=$(dirname "$file")
  links=$(grep -oE '\]\([^)]+\)' "$file" | sed -E 's/^\]\(//; s/\)$//' || true)
  [ -z "$links" ] && continue
  while IFS= read -r link; do
    case "$link" in
      http://*|https://*|mailto:*|\#*) continue ;;
    esac
    target=${link%%#*}
    [ -z "$target" ] && continue
    if [ ! -e "$dir/$target" ]; then
      echo "  [오류] $file: 깨진 링크 $link" >&2
      errors=$((errors + 1))
    fi
  done <<< "$links"
done < "$tmp"

if [ "$errors" -gt 0 ]; then
  echo "check-docs 실패: $errors건" >&2
  exit 1
fi
echo "check-docs 통과"
