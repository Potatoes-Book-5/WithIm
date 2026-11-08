#!/usr/bin/env bash
# 클론 후 1회 실행한다. git 훅 경로를 설정하고 로컬 설정 파일을 확인한다.
set -euo pipefail

ROOT=$(cd "$(dirname "$0")/.." && pwd)
cd "$ROOT"

chmod +x harness/*.sh harness/hooks/*
git config core.hooksPath harness/hooks
echo "git 훅 설정 완료: harness/hooks"

if [ ! -f local.properties ]; then
  echo "local.properties가 없습니다. 다음 명령 후 Pleos 키를 입력하세요:"
  echo "  cp local.properties.example local.properties"
fi
