---
name: register
description: 새 작업이나 todo를 시작하거나 기능, 시나리오, 화면, ADR ID가 필요할 때 사용한다.
---

1. [운영 규칙](../../../docs/tracking/README.md)을 읽습니다.
2. `docs/tracking/tasks.md`에서 중복 작업을 찾습니다. 있으면 그 ID를 사용합니다.
3. 최신 `main` 기준인지 확인합니다.
4. `./harness/new-id.sh`로 T-ID와 필요한 문서 ID를 발급합니다.
5. `./harness/check-registry.sh`를 실행합니다.
6. `chore(tracking): register T-NNN` 커밋과 등록 PR을 제안합니다. 커밋과 푸시는 사용자가 요청할 때만 합니다.
