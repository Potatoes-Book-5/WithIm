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
- 주행 중·주행 완료(`DrivingState`, 화면 전환과 주행 세션)는 AAOS 기어 속성 `GEAR_SELECTION`으로 정합니다(T-034, [sdk-usage-map.md](sdk-usage-map.md#기어-상태t-034)). 이 문서의 `CarUxRestrictions`는 화면 제한에 씁니다.
- DO 선언과 규제 적용(T-035): `MainActivity`에 `distractionOptimized` 메타데이터를 둡니다. 선언 방법은 Pleos 원문에 없어 AAOS Driver Distraction Guidelines를 따릅니다. `vehicle/restriction/CarUxRestrictionSource`가 `Car.createCar`로 연결해 `isRequiresDistractionOptimization()`을 받고, 규제 중에는 주행 화면만 보여 줍니다(DI-002). `getActiveRestrictions()`의 개별 플래그는 주행 화면에 영상·키보드·설정 입력이 없어 아직 나누어 쓰지 않습니다([T-035 계획](../plans/active/T-035-driving-do-declaration.md) 미결).

## 원문에 없는 항목

- 제한 플래그 전체 목록
- 화면 차단의 구체적 방식
