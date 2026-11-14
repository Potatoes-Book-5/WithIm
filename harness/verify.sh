#!/usr/bin/env bash
# 검증 단일 진입점. 사용법은 docs/QUALITY.md 참고.
#   ./harness/verify.sh --docs     문서, ID 검사만
#   ./harness/verify.sh [--fast]   문서 + 빌드, 단위 테스트, lint (기본)
#   ./harness/verify.sh --device   위 항목 + 에뮬레이터 설치, 계측 테스트
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

mode=${1:---fast}
case "$mode" in
  --docs|--fast|--device) ;;
  *) echo "사용법: $0 [--docs|--fast|--device]" >&2; exit 2 ;;
esac

mkdir -p harness/runs
log="harness/runs/$(date +%Y%m%d-%H%M%S)-${mode#--}.log"

run() {
  echo "==> $*" | tee -a "$log"
  "$@" 2>&1 | tee -a "$log"
}

run bash harness/check-docs.sh
run bash harness/check-registry.sh

if [ "$mode" != "--docs" ]; then
  run ./gradlew :core:domain:test assembleDebug testDebugUnitTest lintDebug
fi

if [ "$mode" = "--device" ]; then
  run ./gradlew installDebug connectedDebugAndroidTest
fi

echo "통과: $mode (로그: $log)"
