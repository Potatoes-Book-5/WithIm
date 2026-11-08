# API 호환성과 가용성

> 확인일: 2026-11-08 · 상태: 초안. 페이지 요약을 바탕으로 작성했으며 T-006에서 원문과 대조합니다.
> 공식 문서를 복제하지 않고 요약합니다. 판단이 필요하면 원문을 우선합니다.

## 원문

- [API 호환성 및 가용성](https://document.pleos.ai/docs/pleos-only/vehicle-app-planning-guide/api-compatibility-availability)

## 요약

지원 API는 차량 모델에 따라 다르므로 실행 시점에 지원 여부를 확인합니다.

## 규칙

강도(MUST/SHOULD)는 원문 표현을 바탕으로 팀이 해석한 값입니다.

| ID | 강도 | 규칙 |
|---|---|---|
| CP-001 | MUST | Vehicle SDK는 도메인별 `checkXXXCapabilities`로 지원 여부를 확인한다. |
| CP-002 | INFO | NaviHelper SDK는 내비게이션 탑재 차량에 한해 전체 호환된다. |
| CP-003 | INFO | TextToSpeech, SpeechToText SDK는 전체 호환된다. |
| CP-004 | MUST | FusedLocation SDK는 `isSupported`로 지원 여부를 확인한다. |
| CP-005 | MUST | AAOS 프로퍼티는 `VehiclePropertyIds`와 `CarPropertyManager`로 확인한다. |
| CP-006 | MUST | 차량 모델명으로 분기하지 않는다. |
| CP-007 | MUST | SDK가 제공하지 않는 특수 프로퍼티는 직접 구현 전에 기술 지원팀 검토를 요청한다. |

## 이 앱에 적용

- 위치 미지원 흐름은 S-006에서 정의합니다.

## 원문에 없는 항목

- 버전 정책
- 미지원 시 처리 절차
