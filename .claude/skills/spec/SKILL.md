---
name: spec
description: 기능 문서(F-NNN)나 시나리오 문서(S-NNN)를 작성하거나 보강할 때 사용한다.
---

1. 대상 문서와 `docs/product/overview.md`, `glossary.md`, `feature-state-matrix.md`를 읽습니다.
2. 관련 규칙 문서를 `docs/principles/`, `docs/platform/`에서 읽습니다.
3. 파일의 템플릿 항목을 모두 채웁니다. 인수 조건은 검증 가능한 문장으로 씁니다.
4. Parking, Drive, 오프라인, API 미지원, 백그라운드 동작을 빠짐없이 정의합니다.
5. 모르는 내용은 추정하지 않고 `미결 사항`에 적습니다.
6. 기능 범위가 바뀌면 `feature-state-matrix.md`와 관련 화면 문서를 갱신합니다.
7. `./harness/verify.sh --docs`를 실행합니다.
