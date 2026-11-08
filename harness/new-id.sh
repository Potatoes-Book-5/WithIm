#!/usr/bin/env bash
# 다음 ID를 계산해 tasks.md 또는 index.md 끝에 행을 추가한다.
#   ./harness/new-id.sh T <유형> "<제목>" [우선순위]
#   ./harness/new-id.sh F|S|SCR|ADR "<제목>" <slug> <T-ID>
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

TASKS=docs/tracking/tasks.md
INDEX=docs/tracking/index.md

usage() {
  echo "사용법:" >&2
  echo "  $0 T <feature|fix|docs|design|research|debt|harness> \"제목\" [P0-P3]" >&2
  echo "  $0 F|S|SCR|ADR \"제목\" <slug> <T-ID>" >&2
  exit 2
}

next_id() {
  local prefix=$1 file=$2 max
  max=$(grep -oE "^\| ${prefix}-[0-9]{3} " "$file" | grep -oE '[0-9]{3}' | sort -n | tail -1 || true)
  printf '%s-%03d' "$prefix" $((10#${max:-0} + 1))
}

append_row() {
  local file=$1 row=$2
  if [ -n "$(tail -c1 "$file")" ]; then echo >> "$file"; fi
  printf '%s\n' "$row" >> "$file"
}

check_title() {
  case "$1" in
    ''|*'|'*) echo "제목은 비어 있거나 '|'를 포함할 수 없습니다." >&2; exit 2 ;;
  esac
}

[ $# -ge 1 ] || usage
kind=$1
shift

case "$kind" in
  T)
    [ $# -ge 2 ] || usage
    type=$1 title=$2 priority=${3:-P2}
    case "$type" in feature|fix|docs|design|research|debt|harness) ;; *) usage ;; esac
    case "$priority" in P0|P1|P2|P3) ;; *) usage ;; esac
    check_title "$title"
    id=$(next_id T "$TASKS")
    append_row "$TASKS" "| $id | $type | $title | todo | - | $priority | - | - |"
    ;;
  F|S|SCR|ADR)
    [ $# -eq 3 ] || usage
    title=$1 slug=$2 task=$3
    check_title "$title"
    printf '%s' "$slug" | grep -qE '^[a-z0-9]+(-[a-z0-9]+)*$' || { echo "slug는 소문자, 숫자, 하이픈만 사용합니다." >&2; exit 2; }
    grep -qE "^\| ${task} \|" "$TASKS" || { echo "등록되지 않은 작업: $task" >&2; exit 2; }
    case "$kind" in
      F)   label=feature;  dir=product/features ;;
      S)   label=scenario; dir=product/scenarios ;;
      SCR) label=screen;   dir=design/screens ;;
      ADR) label=decision; dir=decisions ;;
    esac
    id=$(next_id "$kind" "$INDEX")
    append_row "$INDEX" "| $id | $label | $title | reserved | \`$dir/$id-$slug.md\` | $task | - |"
    ;;
  *) usage ;;
esac

echo "$id"
