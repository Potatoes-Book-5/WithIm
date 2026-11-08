# SDK API 정책

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [SDK API 정책](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/pleos-connect-sdk-api)

## 요약

AAOS 표준에 있는 기능은 표준 API를 쓰고, 표준으로 접근할 수 없는 영역에 SDK를 사용합니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| API-001 | SHOULD | AAOS 표준에 동일하거나 유사한 API가 있으면 표준 API를 사용한다. |
| API-002 | SHOULD | 속도, 기어 등 표준 데이터는 AAOS Car API로 접근한다. |
| API-003 | SHOULD | 제조사 특화 기능은 Pleos Connect SDK로 접근한다. |
| API-004 | INFO | SDK 기능이 AAOS 표준에 편입되어도 기존 SDK API는 최소 2개 메이저 버전까지 유지된다. |

## 이 앱에 적용

- 기능별 선택 결과는 [sdk-usage-map.md](sdk-usage-map.md)에 기록합니다.

## 원문에 없는 항목

- 개별 API 목록과 권한 요구사항
