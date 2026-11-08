# 앱 구조와 화면 모델

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [앱 구조 및 동작 모델](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/app-structure-screen-model)

## 요약

앱은 멀티 윈도우, 화면 방향 변화, 포그라운드·백그라운드 전환에 대응해야 합니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| SM-001 | MUST | Driving view를 임의로 가리거나 종료하지 않는다. |
| SM-002 | SHOULD | portrait와 landscape 화면을 각각 설계한다. |
| SM-003 | SHOULD | portrait에는 핵심 기능, landscape에는 부가 정보와 확장 기능을 배치한다. |
| SM-004 | SHOULD | window 크기가 바뀌어도 핵심 기능을 사용할 수 있게 반응형으로 구성한다. |
| SM-005 | SHOULD | 포그라운드 진입 시 상태를 복원하고 UI를 다시 구성한다. |
| SM-006 | SHOULD | 백그라운드 전환 시 화면 갱신, 애니메이션, 불필요한 센서 수집을 중단한다. |
| SM-007 | SHOULD | 백그라운드에서 계속할 작업의 실행 조건과 중단·재개 정책을 정의한다. |
| SM-008 | SHOULD | 백그라운드 상주, 타이머, 폴링, 네트워크 요청을 최소화한다. |
| SM-009 | SHOULD | 프로세스 재생성 후에도 사용자 데이터와 진행 상태를 복원한다. |

## 화면 모델 사실

- 화면은 Driving view 영역과 앱 영역으로 나뉘며, 앱 영역에는 최대 2개 앱을 배치할 수 있습니다.
- `screenOrientation` 미지정 시 기본은 portrait, window 1개입니다. `landscape` 지정 시 window 2개를 사용할 수 있습니다.
- 새 앱은 현재 표시 중인 앱의 방향을 기준으로 실행됩니다.
- 화면에서 사라진 앱은 종료되지 않고 백그라운드에 남을 수 있습니다.

## 이 앱에 적용

- SCR 문서마다 portrait와 landscape 변형을 모두 정의합니다.
- F-005의 근접 판정은 SM-007, SM-008을 기준으로 설계합니다.

## 원문에 없는 항목

- 해상도, 픽셀 크기 기준([layout.md](../design/foundations/layout.md)에서 보강)
- 타이머 주기 기준
