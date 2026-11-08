# 주행 규제 연동

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [주행 규제 개발 가이드](https://document.pleos.ai/docs/pleos-only/vehicle-app-developer-guide/driving-restriction-development-guide)

## 요약

주행 중 UX 제한은 플랫폼이 제공하는 `CarUxRestrictions` 상태를 기준으로 적용합니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| DI-001 | MUST | `CarUxRestrictionsManager`에 `registerListener()`로 등록하고 `onUxRestrictionsChanged()`로 변경을 수신한다. |
| DI-002 | MUST | DO를 선언한 앱은 규제 활성 시 `isRequiresDistractionOptimization()`과 `getActiveRestrictions()`로 기능별 제한을 직접 적용한다. |
| DI-003 | INFO | 지역별 `ActiveRestrictions`는 플랫폼이 내려주므로 앱은 수신값을 반영한다. |

## 이 앱에 적용

- 주행 상태 수신은 `vehicle/` 패키지에서 처리하고 domain에는 앱 자체 상태로 전달합니다.

## 원문에 없는 항목

- 제한 플래그 전체 목록
- 화면 차단의 구체적 방식
