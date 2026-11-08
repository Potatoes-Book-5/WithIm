---
name: verify
description: 변경 사항을 검증하거나 빌드, 테스트, 문서 검사 결과를 확인할 때 사용한다.
---

1. 문서만 바뀌었으면 `./harness/verify.sh --docs`, 코드가 바뀌었으면 `./harness/verify.sh`를 실행합니다.
2. 앱 동작이 바뀌었고 에뮬레이터가 실행 중이면 `./harness/verify.sh --device`를 실행합니다.
3. 실패하면 `harness/runs/`의 로그에서 첫 원인을 찾아 수정하고 다시 실행합니다.
4. 실행한 레벨과 실행하지 못한 레벨을 그대로 보고합니다.
5. L3 항목이 있으면 사람이 확인할 목록을 `docs/QUALITY.md` 기준으로 제시합니다.
