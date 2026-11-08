---
name: exec-plan
description: 여러 단계이거나 세션을 넘겨 이어질 작업의 실행 계획을 작성할 때 사용한다.
---

1. T-ID와 관련 명세를 읽습니다. T-ID가 없으면 `register` 스킬을 먼저 사용합니다.
2. `docs/plans/README.md` 템플릿으로 `docs/plans/active/T-NNN-<slug>.md`를 작성합니다.
3. 각 체크리스트 항목은 독립적으로 검증 가능하게 나눕니다.
4. 범위 밖 항목과 검증 레벨을 명시합니다.
5. 사람의 승인이 필요한 항목은 `docs/team/ai-collaboration.md` 기준으로 표시합니다.
