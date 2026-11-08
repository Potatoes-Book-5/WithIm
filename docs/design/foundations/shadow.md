# Shadow

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [Shadow](https://document.pleos.ai/docs/connect/guide/docs-design/foundations/shadow)

## 요약

그림자는 Level 1~3으로 elevation을 표현합니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| SHD-001 | SHOULD | Level 1은 가장 낮은 elevation에 사용한다. |
| SHD-002 | SHOULD | Level 2는 Level 1보다 높은 elevation에 사용한다. |
| SHD-003 | SHOULD | Level 3은 가장 높은 elevation에 사용한다. |

## 토큰

| 레벨 | 레이어 | X | Y | Blur | Spread | Color |
|---|---|---|---|---|---|---|
| Level 1 | Shadow 1-2 | 0 | 6 | 24 | 0 | #14000000 |
| Level 2 | Shadow 1-1 | 0 | 8 | 32 | 0 | #29000000 |
| Level 2 | Shadow 1-2 | 0 | 2 | 2 | 0 | #14000000 |
| Level 3 | Shadow 1-1 | 0 | 20 | 40 | 0 | #3D000000 |
| Level 3 | Shadow 1-2 | 0 | 2 | 4 | 0 | #33000000 |

기계 판독용 값은 [shadow.json](../tokens/shadow.json)에 있습니다.

## 이 앱에 적용

- 알림 표시(SCR-005)는 Level 3 후보입니다.

## 원문에 없는 항목

- 컴포넌트별 레벨 매핑
